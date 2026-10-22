package com.gathin.tvhome;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * The five daily prayer times, computed on the TV from the sun's position: no network, no library.
 * JAKIM's parameters (Malaysia): Subuh and Isyak at the sun 18 degrees below the horizon, Asar when a
 * shadow is its object's length plus the noon shadow (Shafi'i), and two minutes of ihtiyat on each.
 * Agrees with the published e-Solat tables to within a minute or two.
 */
final class PrayerTimes {

    static final String[] DEFAULT_NAMES = {"Subuh", "Zohor", "Asar", "Maghrib", "Isyak"};
    private static final double FAJR_ANGLE = 18, ISHA_ANGLE = 18, SUNSET_ANGLE = 0.833;
    private static final double IHTIYAT_MIN = 2;

    private PrayerTimes() {
    }

    /** Minutes after local midnight for Subuh, Zohor, Asar, Maghrib and Isyak, rounded up to the minute. */
    static int[] minutes(int year, int month, int day, double lat, double lon, double tzHours) {
        double jd = julianDay(year, month, day) - lon / 360.0;
        double d = jd + 0.5 - 2451545.0;   // local noon, where the sun's place matters most
        double g = Math.toRadians(fix(357.529 + 0.98560028 * d, 360));
        double q = fix(280.459 + 0.98564736 * d, 360);
        double l = Math.toRadians(fix(q + 1.915 * Math.sin(g) + 0.020 * Math.sin(2 * g), 360));
        double e = Math.toRadians(23.439 - 0.00000036 * d);
        double ra = fix(Math.toDegrees(Math.atan2(Math.cos(e) * Math.sin(l), Math.cos(l))) / 15, 24);
        double decl = Math.asin(Math.sin(e) * Math.sin(l));
        double eqt = q / 15 - ra;
        double noon = fix(12 - eqt, 24) + tzHours - lon / 15;
        double phi = Math.toRadians(lat);

        double fajr = noon - hourAngle(FAJR_ANGLE, phi, decl);
        double asrAlt = -Math.toDegrees(Math.atan(1 / (1 + Math.tan(Math.abs(phi - decl)))));
        double asr = noon + hourAngle(asrAlt, phi, decl);
        double maghrib = noon + hourAngle(SUNSET_ANGLE, phi, decl);
        double isha = noon + hourAngle(ISHA_ANGLE, phi, decl);
        double[] h = {fajr, noon, asr, maghrib, isha};
        int[] out = new int[5];
        for (int i = 0; i < 5; i++) out[i] = (int) Math.ceil(h[i] * 60 + IHTIYAT_MIN);
        return out;
    }

    /** Today's times in the TV's own time zone. */
    static int[] today(double lat, double lon) {
        Calendar c = Calendar.getInstance();
        double tz = TimeZone.getDefault().getOffset(c.getTimeInMillis()) / 3_600_000.0;
        return minutes(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), lat, lon, tz);
    }

    /** Index of the next prayer after {@code nowMinutes}, or -1 once Isyak has passed. */
    static int next(int[] times, int nowMinutes) {
        for (int i = 0; i < times.length; i++) if (times[i] > nowMinutes) return i;
        return -1;
    }

    /** "5:42" or "17:42". */
    static String format(int minutes, boolean h24) {
        int h = minutes / 60 % 24, m = minutes % 60;
        if (!h24) h = h % 12 == 0 ? 12 : h % 12;
        return h + ":" + (m < 10 ? "0" : "") + m;
    }

    /** Hours between noon and the sun reaching {@code angle} degrees below the horizon (negative: above). */
    private static double hourAngle(double angle, double phi, double decl) {
        double cos = (-Math.sin(Math.toRadians(angle)) - Math.sin(phi) * Math.sin(decl))
                / (Math.cos(phi) * Math.cos(decl));
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cos)))) / 15;
    }

    private static double julianDay(int y, int m, int d) {
        if (m <= 2) {
            y--;
            m += 12;
        }
        int a = y / 100, b = 2 - a + a / 4;
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + b - 1524.5;
    }

    private static double fix(double v, double range) {
        v %= range;
        return v < 0 ? v + range : v;
    }
}
