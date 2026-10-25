package com.gathin.tvhome;

import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * The five daily prayer times exactly as JAKIM publishes them: tools/esolat.py saves a year of e-Solat
 * times for one zone as prayer_times.json in the theme, and the theme tools push it to the TV. Nothing is
 * computed and nothing is fetched here; a day missing from the file shows no card.
 */
final class PrayerTimes {

    static final String FILE_NAME = "prayer_times.json";
    static final String[] NAMES = {"Subuh", "Zohor", "Asar", "Maghrib", "Isyak"};
    private static final String TAG = "TvHome.Prayer";

    /** "YYYY-MM-DD" to the day's five times in minutes after midnight. */
    private final Map<String, int[]> days;
    final String zone;

    private PrayerTimes(Map<String, int[]> days, String zone) {
        this.days = days;
        this.zone = zone;
    }

    /** Reads the file; null when it is missing or not as tools/esolat.py writes it. */
    static PrayerTimes load(File file) {
        if (file == null || !file.isFile() || file.length() > 256 * 1024) return null;
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[(int) file.length()];
            int n = 0, r;
            while (n < buf.length && (r = in.read(buf, n, buf.length - n)) > 0) n += r;
            return parse(new String(buf, 0, n, StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.w(TAG, "ignoring " + file + ": " + e.getMessage());
            return null;
        }
    }

    static PrayerTimes parse(String text) throws org.json.JSONException {
        JSONObject json = new JSONObject(text);
        JSONObject times = json.getJSONObject("times");
        Map<String, int[]> days = new HashMap<>();
        for (Iterator<String> it = times.keys(); it.hasNext(); ) {
            String day = it.next();
            int[] t = parseDay(times.optString(day));
            if (t != null) days.put(day, t);
        }
        return days.isEmpty() ? null : new PrayerTimes(days, json.optString("zone"));
    }

    /** "05:39 12:52 16:09 18:53 20:03" to minutes, or null if it is anything else. */
    static int[] parseDay(String s) {
        String[] parts = s.trim().split(" ");
        if (parts.length != NAMES.length) return null;
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.length() != 5 || p.charAt(2) != ':') return null;
            try {
                int h = Integer.parseInt(p.substring(0, 2)), m = Integer.parseInt(p.substring(3));
                if (h > 23 || m > 59) return null;
                out[i] = h * 60 + m;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return out;
    }

    /** The day's times, or null when the file has no entry for it (a new year needs a new file). */
    int[] on(Calendar c) {
        return days.get(String.format(Locale.ROOT, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)));
    }

    /** Index of the next prayer after {@code nowMinutes}, or -1 once Isyak has begun. */
    static int next(int[] times, int nowMinutes) {
        for (int i = 0; i < times.length; i++) if (times[i] > nowMinutes) return i;
        return -1;
    }

    /** Time until the next prayer: "lagi 23 min", "lagi 1 j 5 min", "lagi 2 j"; "masuk waktu" at 0. */
    static String left(int minutes) {
        if (minutes <= 0) return "masuk waktu";
        int h = minutes / 60, m = minutes % 60;
        if (h == 0) return "lagi " + m + " min";
        return "lagi " + h + " j" + (m > 0 ? " " + m + " min" : "");
    }

    /** "5:42" or "17:42". */
    static String format(int minutes, boolean h24) {
        int h = minutes / 60 % 24, m = minutes % 60;
        if (!h24) h = h % 12 == 0 ? 12 : h % 12;
        return h + ":" + (m < 10 ? "0" : "") + m;
    }
}
