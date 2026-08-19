package com.unfold.core.ui.iconpack

import kotlin.test.Test
import kotlin.test.assertEquals

class IconPackResolverTest {
    @Test
    fun `pack resource names are generated from package names`() {
        val names = IconPackResolver.buildResourceCandidates("com.example.app")
        assertEquals(
            listOf(
                "com_example_app",
                "icon_com_example_app",
                "ic_com_example_app",
                "app_icon_com_example_app"
            ),
            names
        )
    }
}
