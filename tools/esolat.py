#!/usr/bin/env python3
"""Download a year of prayer times for one JAKIM zone from e-Solat and save them in a theme folder.

The launcher never goes online for these: the theme tools push prayer_times.json to the TV, and Home shows
the day's five times as JAKIM published them. Run it again early each January for the new year.

Usage:
    esolat.py <zone> <theme folder>       e.g.  esolat.py JHR02 themes/mine
Zones are JAKIM's codes (JHR02 Johor Bahru, SGR01 Shah Alam/Petaling, WLY01 Kuala Lumpur, ...):
https://www.e-solat.gov.my
"""
import json
import os
import re
import sys
import urllib.request

URL = "https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=year&zone={zone}"
MONTHS = {"Jan": 1, "Feb": 2, "Mac": 3, "Apr": 4, "Mei": 5, "Jun": 6,
          "Jul": 7, "Ogos": 8, "Ogo": 8, "Sep": 9, "Okt": 10, "Nov": 11, "Dis": 12}
FIELDS = ("fajr", "dhuhr", "asr", "maghrib", "isha")
TIME = re.compile(r"^([01]\d|2[0-3]):([0-5]\d)(:[0-5]\d)?$")


def convert(data, zone):
    """e-Solat's JSON to {"source", "zone", "times": {"YYYY-MM-DD": "HH:MM HH:MM HH:MM HH:MM HH:MM"}}.
    Anything that doesn't look exactly as expected is refused, never guessed."""
    rows = data.get("prayerTime") if isinstance(data, dict) else None
    if not isinstance(rows, list) or not rows:
        raise ValueError("no prayerTime list in the reply")
    times = {}
    for row in rows:
        m = re.match(r"^(\d{2})-([A-Za-z]+)-(\d{4})$", str(row.get("date", "")))
        if not m or m.group(2) not in MONTHS:
            raise ValueError("unexpected date %r" % row.get("date"))
        day = "%s-%02d-%s" % (m.group(3), MONTHS[m.group(2)], m.group(1))
        hhmm = []
        for f in FIELDS:
            t = TIME.match(str(row.get(f, "")))
            if not t:
                raise ValueError("unexpected %s %r on %s" % (f, row.get(f), day))
            hhmm.append(t.group(1) + ":" + t.group(2))
        times[day] = " ".join(hhmm)
    return {"source": "JAKIM e-Solat", "zone": zone, "times": times}


def main():
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    zone = sys.argv[1].upper()
    if not re.match(r"^[A-Z]{3}\d{2}$", zone):
        sys.exit("zone must be a JAKIM code such as JHR02")
    folder = sys.argv[2]
    if not os.path.isdir(folder):
        sys.exit("no such theme folder: " + folder)
    req = urllib.request.Request(URL.format(zone=zone), headers={"User-Agent": "glass-tv"})
    with urllib.request.urlopen(req, timeout=60) as r:
        data = json.loads(r.read().decode("utf-8"))
    out = convert(data, zone)
    path = os.path.join(folder, "prayer_times.json")
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(out, f, separators=(",", ":"), sort_keys=True)
    days = sorted(out["times"])
    print("%s: %d days, %s to %s -> %s" % (zone, len(days), days[0], days[-1], path))


if __name__ == "__main__":
    main()
