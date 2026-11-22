/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The audiophile tone stage: everything here rewrites samples, so the whole
 * processor is a straight passthrough until at least one control leaves its
 * neutral value — and it is forced neutral while Bit Perfect is on, exactly
 * like the EQ and the spatial widener.
 *
 * Chain order inside the buffer, per frame:
 *   high-pass → mono → crossfeed → balance → midnight → pre-amp → limiter
 *
 * The limiter is last so nothing downstream can push a boosted signal past
 * full scale, and the pre-amp sits just before it so a +12 dB boost is caught
 * rather than clipped by the PCM cast.
 */

package com.vox.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.tanh

@UnstableApi
class AudiophileDspProcessor : BaseAudioProcessor() {

    /** -1 fully left … 0 centred … +1 fully right. */
    @Volatile var balance: Float = 0f
        set(value) {
            field = value.coerceIn(-1f, 1f)
        }

    /** True sums both channels to mono. */
    @Volatile var mono: Boolean = false

    /** 0 off … 1 maximum Bauer-style crossfeed. */
    @Volatile var crossfeed: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    /** -12 dB … +12 dB, applied before the limiter. */
    @Volatile var preampDb: Float = 0f
        set(value) {
            field = value.coerceIn(-12f, 12f)
        }

    /** 20 Hz high-pass, to take sub-sonic rumble out of small drivers. */
    @Volatile var highPass: Boolean = false

    /** Gentle downward compression of loud peaks, for late-night listening. */
    @Volatile var midnight: Boolean = false

    /** Held while the A/B "Raw audio" button is down. */
    @Volatile var rawBypass: Boolean = false

    /** Set by Bit Perfect: every control is forced neutral, values kept for restore. */
    @Volatile private var bitPerfectBypass: Boolean = false

    private val active: Boolean
        get() = !rawBypass && !bitPerfectBypass && (
            balance != 0f || mono || crossfeed > 0f || preampDb != 0f || highPass || midnight
            )

    /** Applies the stored settings in one shot; [bitPerfect] neutralises the stage. */
    fun applyCached(
        balance: Float,
        mono: Boolean,
        crossfeed: Float,
        preampDb: Float,
        highPass: Boolean,
        midnight: Boolean,
        bitPerfect: Boolean,
    ) {
        bitPerfectBypass = bitPerfect
        this.balance = if (bitPerfect) 0f else balance
        this.mono = !bitPerfect && mono
        this.crossfeed = if (bitPerfect) 0f else crossfeed
        this.preampDb = if (bitPerfect) 0f else preampDb
        this.highPass = !bitPerfect && highPass
        this.midnight = !bitPerfect && midnight
    }

    private var highPassCoeff = 0f
    private var lowpassCoeff = 0f
    private var hpLowLeft = 0f
    private var hpLowRight = 0f
    private var crossLowLeft = 0f
    private var crossLowRight = 0f
    private var delayLeft = ShortArray(0)
    private var delayRight = ShortArray(0)
    private var delayIndex = 0
    private var processingActive = false
    private var borrowedOutput = AudioProcessor.EMPTY_BUFFER

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT ||
            inputAudioFormat.channelCount != 2 ||
            inputAudioFormat.sampleRate <= 0
        ) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun onFlush() {
        val rate = inputAudioFormat.sampleRate
        if (rate > 0) {
            highPassCoeff = (1.0 - exp(-2.0 * PI * HIGH_PASS_HZ / rate)).toFloat()
            lowpassCoeff = (1.0 - exp(-2.0 * PI * CROSSFEED_LOWPASS_HZ / rate)).toFloat()
            val samples = (rate * CROSSFEED_DELAY_MS / 1000f).roundToInt().coerceAtLeast(1)
            if (delayLeft.size != samples) {
                delayLeft = ShortArray(samples)
                delayRight = ShortArray(samples)
            }
        } else {
            delayLeft = ShortArray(0)
            delayRight = ShortArray(0)
        }
        delayLeft.fill(0)
        delayRight.fill(0)
        delayIndex = 0
        hpLowLeft = 0f
        hpLowRight = 0f
        crossLowLeft = 0f
        crossLowRight = 0f
        processingActive = false
        borrowedOutput = AudioProcessor.EMPTY_BUFFER
    }

    override fun onReset() {
        onFlush()
        delayLeft = ShortArray(0)
        delayRight = ShortArray(0)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val byteCount = inputBuffer.remaining()
        if (byteCount == 0) return
        borrowedOutput = AudioProcessor.EMPTY_BUFFER

        if (!active || delayLeft.isEmpty()) {
            if (processingActive) onFlush()
            borrowedOutput = inputBuffer.slice().order(ByteOrder.nativeOrder())
            inputBuffer.position(inputBuffer.limit())
            return
        }

        val frameCount = byteCount / BYTES_PER_FRAME
        val outputBuffer = replaceOutputBuffer(byteCount)
        inputBuffer.order(ByteOrder.nativeOrder())
        outputBuffer.order(ByteOrder.nativeOrder())
        processingActive = true

        val preamp = 10.0.pow(preampDb / 20.0).toFloat()
        val leftBalance = if (balance > 0f) 1f - balance else 1f
        val rightBalance = if (balance < 0f) 1f + balance else 1f
        val crossGain = crossfeed * CROSSFEED_MAX_GAIN
        val delaySize = delayLeft.size

        repeat(frameCount) {
            val rawLeft = inputBuffer.short.toFloat() / SHORT_SCALE
            val rawRight = inputBuffer.short.toFloat() / SHORT_SCALE
            var left = rawLeft
            var right = rawRight

            if (highPass) {
                hpLowLeft += highPassCoeff * (left - hpLowLeft)
                hpLowRight += highPassCoeff * (right - hpLowRight)
                left -= hpLowLeft
                right -= hpLowRight
            }

            if (mono) {
                val sum = (left + right) * 0.5f
                left = sum
                right = sum
            }

            // Read the delayed pair before overwriting the slot, then always store
            // the untouched input: feeding the widened sample back would let the
            // crossfeed compound on itself, and a stale line would smear when the
            // control is switched back on.
            val delayedLeft = delayLeft[delayIndex].toFloat() / SHORT_SCALE
            val delayedRight = delayRight[delayIndex].toFloat() / SHORT_SCALE
            delayLeft[delayIndex] = clampToShort(rawLeft * SHORT_SCALE)
            delayRight[delayIndex] = clampToShort(rawRight * SHORT_SCALE)
            if (++delayIndex == delaySize) delayIndex = 0

            if (crossGain > 0f) {
                crossLowLeft += lowpassCoeff * (delayedRight - crossLowLeft)
                crossLowRight += lowpassCoeff * (delayedLeft - crossLowRight)
                left += crossLowLeft * crossGain
                right += crossLowRight * crossGain
            }

            left *= leftBalance
            right *= rightBalance

            if (midnight) {
                left = softCompress(left) * MIDNIGHT_MAKEUP
                right = softCompress(right) * MIDNIGHT_MAKEUP
            }

            left *= preamp
            right *= preamp

            outputBuffer.putShort(clampToShort(limit(left) * SHORT_SCALE))
            outputBuffer.putShort(clampToShort(limit(right) * SHORT_SCALE))
        }

        outputBuffer.put(inputBuffer)
        outputBuffer.flip()
    }

    override fun getOutput(): ByteBuffer {
        val processed = super.getOutput()
        val borrowed = borrowedOutput
        borrowedOutput = AudioProcessor.EMPTY_BUFFER
        return if (borrowed.hasRemaining()) borrowed else processed
    }

    override fun isEnded(): Boolean = super.isEnded() && !borrowedOutput.hasRemaining()

    /** Soft-knee downward compression: peaks above -9 dBFS ease toward the ceiling. */
    private fun softCompress(sample: Float): Float {
        val magnitude = abs(sample)
        if (magnitude <= COMPRESS_KNEE_START) return sample
        val over = magnitude - COMPRESS_KNEE_START
        val range = COMPRESS_CEILING - COMPRESS_KNEE_START
        val compressed = COMPRESS_KNEE_START + range * tanh(over / range)
        return sign(sample) * compressed
    }

    /** Soft-knee peak limit: untouched below the knee, asymptotic to full scale above it. */
    private fun limit(sample: Float): Float {
        val magnitude = abs(sample)
        if (magnitude <= LIMIT_KNEE) return sample
        val over = (magnitude - LIMIT_KNEE) / (1f - LIMIT_KNEE)
        val limited = LIMIT_KNEE + (1f - LIMIT_KNEE) * tanh(over)
        return sign(sample) * limited.coerceAtMost(1f)
    }

    private fun clampToShort(value: Float): Short =
        value.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat()).toInt().toShort()

    private companion object {
        const val BYTES_PER_FRAME = 4 // stereo, 16-bit
        const val SHORT_SCALE = 32_768f
        const val HIGH_PASS_HZ = 20.0
        const val CROSSFEED_LOWPASS_HZ = 700.0
        const val CROSSFEED_DELAY_MS = 0.3f
        const val CROSSFEED_MAX_GAIN = 0.45f
        const val COMPRESS_KNEE_START = 0.355f     // ≈ -9 dBFS
        const val COMPRESS_CEILING = 0.85f
        const val MIDNIGHT_MAKEUP = 1.4f           // ≈ +3 dB, recovered by the limiter
        const val LIMIT_KNEE = 0.95f
    }
}
