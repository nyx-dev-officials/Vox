/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Tidal OAuth 2.0 PKCE login.
 *
 * The client id lives in `local.properties` and reaches the app through
 * `BuildConfig.TIDAL_CLIENT_ID`. PKCE is a public-client flow, so the client
 * secret is deliberately NOT sent from the app — it stays out of the token
 * exchange and out of anything that could be lifted from an APK.
 *
 * Tokens never touch plain SharedPreferences: they are sealed with an
 * AES/GCM key held by the Android Keystore, so a backup dump or a rooted
 * file browser sees ciphertext only.
 */

package com.vox.music.utils.tidal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.vox.music.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Sealed session, as stored. */
@Serializable
data class TidalTokens(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expires_at") val expiresAtMillis: Long = 0L,
)

@Serializable
private data class TidalTokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)

object TidalAuth {

    private const val TAG = "TidalAuth"
    private const val PREFS = "vox_tidal_auth"
    private const val KEY_TOKENS = "tokens_v1"
    private const val KEY_STATE = "oauth_state"
    private const val KEY_VERIFIER = "oauth_verifier"
    private const val KEY_ALIAS = "vox.tidal.tokens.v1"
    private const val GCM_TAG_BITS = 128

    private const val AUTHORIZE_URL = "https://login.tidal.com/authorize"
    private const val TOKEN_URL = "https://auth.tidal.com/v1/oauth2/token"

    /** Must match the intent filter on MainActivity exactly. */
    const val REDIRECT_URI = "vox://tidal-auth"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val http by lazy { OkHttpClient() }
    private val random = SecureRandom()

    /** True when the build actually carries a Tidal client id. */
    val isConfigured: Boolean
        get() = BuildConfig.TIDAL_CLIENT_ID.isNotBlank()

    fun isLoggedIn(context: Context): Boolean =
        isConfigured && pullTokens(context)?.refreshToken?.isNotBlank() == true

    /** Opens Tidal's consent screen. Null when the build has no client id. */
    fun beginLogin(context: Context): Intent? {
        if (!isConfigured) return null
        val verifier = randomToken(64)
        val challenge = challengeFor(verifier)
        val state = randomToken(24)
        prefs(context).edit()
            .putString(KEY_VERIFIER, verifier)
            .putString(KEY_STATE, state)
            .apply()

        val authorize = Uri.parse(AUTHORIZE_URL).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", BuildConfig.TIDAL_CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", "r_usr")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)
            .build()
        return Intent(Intent.ACTION_VIEW, authorize)
    }

    /**
     * Called with the `vox://tidal-auth?code=…&state=…` redirect. Returns a human
     * readable failure message, or null on success.
     */
    fun completeLogin(context: Context, redirect: Uri): String? {
        val store = prefs(context)
        val returnedState = redirect.getQueryParameter("state")
        val expectedState = store.getString(KEY_STATE, null)
        if (expectedState == null || returnedState != expectedState) {
            return "Login state did not match — try again."
        }
        redirect.getQueryParameter("error")?.let {
            return redirect.getQueryParameter("error_description") ?: "Tidal returned: $it"
        }
        val code = redirect.getQueryParameter("code") ?: return "Tidal sent no authorization code."
        val verifier = store.getString(KEY_VERIFIER, null) ?: return "Missing PKCE verifier — try again."

        val form = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", REDIRECT_URI)
            .add("client_id", BuildConfig.TIDAL_CLIENT_ID)
            .add("code_verifier", verifier)
            .build()

        val response = runCatching { postToken(form) }.getOrElse {
            Timber.tag(TAG).w(it, "token exchange failed")
            return it.message ?: "Could not reach Tidal."
        }
        if (response.accessToken.isNullOrBlank()) {
            return response.errorDescription ?: response.error ?: "Tidal rejected the login."
        }
        persist(context, response)
        store.edit().remove(KEY_STATE).remove(KEY_VERIFIER).apply()
        Timber.tag(TAG).i("Tidal login complete")
        return null
    }

    /** A usable bearer token, refreshed when it is within a minute of expiry. */
    fun accessToken(context: Context): String? {
        val tokens = pullTokens(context) ?: return null
        if (tokens.accessToken.isBlank()) return null
        if (tokens.expiresAtMillis - System.currentTimeMillis() > 60_000L) return tokens.accessToken
        return refresh(context, tokens)
    }

    fun logout(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun refresh(context: Context, tokens: TidalTokens): String? {
        if (tokens.refreshToken.isBlank()) return null
        val form = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", tokens.refreshToken)
            .add("client_id", BuildConfig.TIDAL_CLIENT_ID)
            .build()
        val response = runCatching { postToken(form) }.getOrElse {
            Timber.tag(TAG).w(it, "token refresh failed")
            return null
        }
        if (response.accessToken.isNullOrBlank()) {
            Timber.tag(TAG).w("refresh rejected: ${response.errorDescription ?: response.error}")
            return null
        }
        // A refresh response may omit the refresh token; keep the old one then.
        persist(context, response, previousRefresh = tokens.refreshToken)
        return response.accessToken
    }

    private fun postToken(form: FormBody): TidalTokenResponse {
        val request = Request.Builder()
            .url(TOKEN_URL)
            .header("Accept", "application/json")
            .post(form)
            .build()
        http.newCall(request).execute().use { call ->
            val body = call.body?.string().orEmpty()
            if (!call.isSuccessful) {
                Timber.tag(TAG).d("token HTTP ${call.code}: ${body.take(200)}")
            }
            return json.decodeFromString<TidalTokenResponse>(body)
        }
    }

    private fun persist(context: Context, response: TidalTokenResponse, previousRefresh: String? = null) {
        val expiresIn = response.expiresIn ?: 3600L
        val tokens = TidalTokens(
            accessToken = response.accessToken.orEmpty(),
            refreshToken = response.refreshToken?.takeIf { it.isNotBlank() } ?: previousRefresh.orEmpty(),
            expiresAtMillis = System.currentTimeMillis() + expiresIn * 1000L,
        )
        val encoded = json.encodeToString(TidalTokens.serializer(), tokens)
        prefs(context).edit().putString(KEY_TOKENS, seal(encoded)).apply()
    }

    private fun pullTokens(context: Context): TidalTokens? {
        val sealed = prefs(context).getString(KEY_TOKENS, null) ?: return null
        val plain = runCatching { unseal(sealed) }.getOrElse {
            Timber.tag(TAG).w(it, "stored tokens unreadable — asking for a fresh login")
            prefs(context).edit().remove(KEY_TOKENS).apply()
            return null
        }
        return runCatching { json.decodeFromString<TidalTokens>(plain) }.getOrNull()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- PKCE -----------------------------------------------------------------

    private fun randomToken(bytes: Int): String {
        val buffer = ByteArray(bytes)
        random.nextBytes(buffer)
        return Base64.encodeToString(buffer, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun challengeFor(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    // ---- Keystore sealing -----------------------------------------------------

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun seal(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + ciphertext, Base64.NO_WRAP)
    }

    private fun unseal(encoded: String): String {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        require(bytes.size > 12) { "sealed blob too short" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(GCM_TAG_BITS, bytes, 0, 12),
        )
        val plain = cipher.doFinal(bytes, 12, bytes.size - 12)
        return String(plain, Charsets.UTF_8)
    }
}
