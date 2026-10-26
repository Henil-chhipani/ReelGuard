package com.reelguard.app

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakFormatterTest {

    private val hour = 3_600_000L
    private val day = 86_400_000L

    @Test
    fun `under an hour`() {
        assertEquals("<1h", StreakFormatter.duration(0L))
        assertEquals("<1h", StreakFormatter.duration(59 * 60_000L))
    }

    @Test
    fun `hours`() {
        assertEquals("5h", StreakFormatter.duration(5 * hour))
    }

    @Test
    fun `days`() {
        assertEquals("1d", StreakFormatter.duration(24 * hour))
        assertEquals("3d", StreakFormatter.duration(3 * day))
    }

    @Test
    fun `weeks`() {
        assertEquals("2w", StreakFormatter.duration(14 * day))
        assertEquals("1w 3d", StreakFormatter.duration(10 * day))
    }

    @Test
    fun `months weeks and days`() {
        assertEquals("1M 5d", StreakFormatter.duration(35 * day))
        assertEquals("2M 3W 3d", StreakFormatter.duration(84 * day))
    }

    @Test
    fun `years`() {
        assertEquals("1Y", StreakFormatter.duration(365 * day))
        assertEquals("1Y 1M 5d", StreakFormatter.duration(400 * day))
    }
}
