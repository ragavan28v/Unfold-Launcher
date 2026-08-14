package com.ragavan.unfold

import com.unfold.core.domain.model.WallpaperMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun presetWallpaperMode_isSupported() {
        assertEquals("PRESET", WallpaperMode.PRESET.name)
    }
}