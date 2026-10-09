package br.com.studyou.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class IsoInstantTest {

    // 2026-10-17T18:09:18Z
    private val base = 1792260558000L

    @Test
    fun parsesUtcWithMicroseconds() {
        assertEquals(base + 364, parseIsoInstantMillis("2026-10-17T18:09:18.364140Z"))
    }

    @Test
    fun parsesExplicitOffset() {
        assertEquals(base, parseIsoInstantMillis("2026-10-17T15:09:18-03:00"))
        assertEquals(base, parseIsoInstantMillis("2026-10-17T18:09:18+00:00"))
    }
}
