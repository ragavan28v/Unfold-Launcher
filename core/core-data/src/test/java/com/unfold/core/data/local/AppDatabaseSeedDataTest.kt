package com.unfold.core.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDatabaseSeedDataTest {
    @Test
    fun defaultSeed_containsExpectedFolderNames() {
        val folderNames = AppDatabaseSeedData.defaultFolders().map { it.name }
        assertTrue(folderNames.contains("Social"))
        assertTrue(folderNames.contains("Games"))
        assertTrue(folderNames.contains("Other"))
    }

    @Test
    fun classification_resolvesKnownPackagesAndFolders() {
        assertEquals("MESSAGING", AppDatabaseSeedData.resolveCategory("com.whatsapp", "WhatsApp"))
        assertEquals("seed_games", AppDatabaseSeedData.folderIdForCategory("GAMES"))
        assertEquals("FINANCE", AppDatabaseSeedData.resolveCategory("com.example.bankapp", "Banking"))
    }
}
