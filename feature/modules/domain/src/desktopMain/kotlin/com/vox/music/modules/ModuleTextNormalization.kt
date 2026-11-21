package com.vox.music.modules

import java.text.Normalizer

internal actual fun moduleUnicodeCompatibilityDecomposition(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFKD)
