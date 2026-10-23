import os
import sys
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "tools"))
import esolat  # noqa: E402

ROW = {"hijri": "1448-05-02", "date": "23-Okt-2026", "day": "Friday", "imsak": "05:29:00",
       "fajr": "05:39:00", "syuruk": "06:51:00", "dhuha": "07:18:00", "dhuhr": "12:52:00",
       "asr": "16:09:00", "maghrib": "18:53:00", "isha": "20:03:00"}


class EsolatTest(unittest.TestCase):
    def test_converts_jakim_rows(self):
        out = esolat.convert({"prayerTime": [ROW, dict(ROW, date="01-Ogos-2026")]}, "JHR02")
        self.assertEqual(out["zone"], "JHR02")
        self.assertEqual(out["times"]["2026-10-23"], "05:39 12:52 16:09 18:53 20:03")
        self.assertIn("2026-08-01", out["times"])

    def test_refuses_what_it_does_not_understand(self):
        for bad in [{}, {"prayerTime": []}, {"prayerTime": [dict(ROW, date="2026-10-23")]},
                    {"prayerTime": [dict(ROW, date="23-Foo-2026")]}, {"prayerTime": [dict(ROW, asr="4:09 PM")]}]:
            with self.assertRaises(ValueError):
                esolat.convert(bad, "JHR02")


if __name__ == "__main__":
    unittest.main()
