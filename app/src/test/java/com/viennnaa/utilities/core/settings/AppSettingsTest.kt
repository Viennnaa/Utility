package com.viennnaa.utilities.core.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `following the system takes the device setting`() {
        assertTrue(ThemeMode.SYSTEM.resolveDark(systemInDark = true))
        assertFalse(ThemeMode.SYSTEM.resolveDark(systemInDark = false))
    }

    @Test
    fun `an explicit choice overrides the device`() {
        // The point of offering the choice is that it wins.
        assertFalse(ThemeMode.LIGHT.resolveDark(systemInDark = true))
        assertTrue(ThemeMode.DARK.resolveDark(systemInDark = false))
    }

    @Test
    fun `an unknown or missing stored mode falls back to following the device`() {
        assertEquals(ThemeMode.SYSTEM, themeModeOf(null))
        assertEquals(ThemeMode.SYSTEM, themeModeOf(""))
        assertEquals(ThemeMode.SYSTEM, themeModeOf("PUCE"))
        assertEquals(ThemeMode.DARK, themeModeOf("DARK"))
    }

    @Test
    fun `the default settings follow the device and pin nothing`() {
        val settings = AppSettings()
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertTrue(settings.dynamicColor)
        assertTrue(settings.favouriteIds.isEmpty())
    }

    @Test
    fun `pinning adds to the end so existing pins do not move`() {
        var favourites = listOf("a", "b")
        favourites = toggleFavourite(favourites, "c")
        assertEquals(listOf("a", "b", "c"), favourites)
    }

    @Test
    fun `pinning something already pinned unpins it`() {
        assertEquals(listOf("a", "c"), toggleFavourite(listOf("a", "b", "c"), "b"))
    }

    @Test
    fun `unpinning the only favourite empties the list`() {
        assertTrue(toggleFavourite(listOf("a"), "a").isEmpty())
    }

    @Test
    fun `pinning past the cap is refused rather than dropping an existing pin`() {
        // Quietly unpinning something the user chose would be worse than
        // declining the new one.
        val full = (1..MAX_FAVOURITES).map { "app$it" }
        assertEquals(full, toggleFavourite(full, "one more"))
        assertFalse(canPinMore(full))
        assertTrue(canPinMore(full.dropLast(1)))
    }

    @Test
    fun `a full list can still be unpinned from`() {
        val full = (1..MAX_FAVOURITES).map { "app$it" }
        assertEquals(MAX_FAVOURITES - 1, toggleFavourite(full, "app1").size)
    }

    @Test
    fun `sanitize drops ids that no longer name a mini app`() {
        // A mini app removed in an update leaves its id behind in storage.
        assertEquals(
            listOf("a", "c"),
            sanitizeFavourites(listOf("a", "gone", "c"), setOf("a", "b", "c")),
        )
    }

    @Test
    fun `sanitize keeps the stored order`() {
        assertEquals(
            listOf("c", "a", "b"),
            sanitizeFavourites(listOf("c", "a", "b"), setOf("a", "b", "c")),
        )
    }

    @Test
    fun `sanitize removes duplicates`() {
        assertEquals(listOf("a", "b"), sanitizeFavourites(listOf("a", "b", "a"), setOf("a", "b")))
    }

    @Test
    fun `sanitize caps a list that grew too long in storage`() {
        val known = (1..20).map { "app$it" }
        val result = sanitizeFavourites(known, known.toSet())
        assertEquals(MAX_FAVOURITES, result.size)
    }

    @Test
    fun `sanitize of an empty or wholly unknown list gives nothing`() {
        assertTrue(sanitizeFavourites(emptyList(), setOf("a")).isEmpty())
        assertTrue(sanitizeFavourites(listOf("x", "y"), setOf("a")).isEmpty())
    }
}
