package com.gathin.tvhome;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PrayerTimesTest {

    /** The line tools/esolat.py writes for Johor Bahru (JHR02) on 23 October 2026, as JAKIM publishes it. */
    @Test
    public void readsJakimDay() {
        assertArrayEquals(new int[]{5 * 60 + 39, 12 * 60 + 52, 16 * 60 + 9, 18 * 60 + 53, 20 * 60 + 3},
                PrayerTimes.parseDay("05:39 12:52 16:09 18:53 20:03"));
    }

    @Test
    public void refusesAnythingElse() {
        assertNull(PrayerTimes.parseDay(""));
        assertNull(PrayerTimes.parseDay("05:39 12:52 16:09 18:53"));
        assertNull(PrayerTimes.parseDay("5:39 12:52 16:09 18:53 20:03"));
        assertNull(PrayerTimes.parseDay("05:39 12:52 16:09 18:53 24:03"));
        assertNull(PrayerTimes.parseDay("05:39 12:52 16:9x 18:53 20:03"));
    }

    @Test
    public void nextPrayer() {
        int[] t = {339, 772, 969, 1133, 1203};
        assertEquals(0, PrayerTimes.next(t, 0));
        assertEquals(2, PrayerTimes.next(t, 800));
        assertEquals(-1, PrayerTimes.next(t, 1300));
    }

    @Test
    public void timeLeft() {
        assertEquals("lagi 1 min", PrayerTimes.left(1));
        assertEquals("lagi 23 min", PrayerTimes.left(23));
        assertEquals("lagi 1 j", PrayerTimes.left(60));
        assertEquals("lagi 1 j 5 min", PrayerTimes.left(65));
        assertEquals("lagi 8 j 30 min", PrayerTimes.left(510));
        assertEquals("masuk waktu", PrayerTimes.left(0));
    }

    @Test
    public void formats() {
        assertEquals("5:07", PrayerTimes.format(307, true));
        assertEquals("17:42", PrayerTimes.format(1062, true));
        assertEquals("5:42", PrayerTimes.format(1062, false));
        assertEquals("12:57", PrayerTimes.format(777, false));
    }
}
