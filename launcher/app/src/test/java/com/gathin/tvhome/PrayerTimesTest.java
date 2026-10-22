package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PrayerTimesTest {

    /** Kuala Lumpur, 23 October 2026, against JAKIM's published e-Solat table for zone WLY01. */
    @Test
    public void kualaLumpurMatchesJakimWithinTwoMinutes() {
        int[] t = PrayerTimes.minutes(2026, 10, 23, 3.139, 101.687, 8);
        int[] jakim = {5 * 60 + 49, 13 * 60, 16 * 60 + 19, 19 * 60, 20 * 60 + 10};
        for (int i = 0; i < 5; i++) {
            assertTrue(PrayerTimes.DEFAULT_NAMES[i] + " was " + PrayerTimes.format(t[i], true),
                    Math.abs(t[i] - jakim[i]) <= 2);
        }
    }

    @Test
    public void timesAreInOrder() {
        int[] t = PrayerTimes.minutes(2026, 6, 21, 3.139, 101.687, 8);
        for (int i = 1; i < 5; i++) assertTrue(t[i] > t[i - 1]);
    }

    @Test
    public void nextPrayer() {
        int[] t = {340, 777, 963, 1139, 1208};
        assertEquals(0, PrayerTimes.next(t, 0));
        assertEquals(2, PrayerTimes.next(t, 800));
        assertEquals(-1, PrayerTimes.next(t, 1300));
    }

    @Test
    public void formats() {
        assertEquals("5:07", PrayerTimes.format(307, true));
        assertEquals("17:42", PrayerTimes.format(1062, true));
        assertEquals("5:42", PrayerTimes.format(1062, false));
        assertEquals("12:57", PrayerTimes.format(777, false));
    }
}
