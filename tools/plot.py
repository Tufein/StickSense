#!/usr/bin/env python3
"""Plot download speed vs. cumulative data used, from speedlog.csv.

Usage: python3 plot.py speedlog.csv [usage.csv] [out.png]

usage.csv (optional) = your daily usage readings: date,used_gb
(from My Telenet screenshots or your router). If omitted, cumulative usage is
derived from wan_rx_bytes+wan_tx_bytes in speedlog.csv when present.
Requires: pip install matplotlib
"""
import csv, sys
from datetime import datetime
import matplotlib.pyplot as plt

speed_csv = sys.argv[1]
usage_csv = sys.argv[2] if len(sys.argv) > 2 and sys.argv[2].endswith(".csv") else None
out = sys.argv[-1] if sys.argv[-1].endswith(".png") else "throttle-evidence.png"

t, dl, used = [], [], []
first = None
with open(speed_csv) as f:
    for r in csv.DictReader(f):
        if not r["download_mbps"]:
            continue
        ts = datetime.strptime(r["timestamp_utc"], "%Y-%m-%dT%H:%M:%SZ")
        t.append(ts); dl.append(float(r["download_mbps"]))
        if r.get("wan_rx_bytes") and r.get("wan_tx_bytes"):
            b = int(r["wan_rx_bytes"]) + int(r["wan_tx_bytes"])
            first = b if first is None else first
            used.append((b - first) / 1e9)

fig, ax1 = plt.subplots(figsize=(12, 5))
ax1.plot(t, dl, ".-", label="Download (Mbps)")
ax1.set_ylabel("Download speed (Mbps)")
ax1.set_xlabel("Time (UTC)")

ax2 = ax1.twinx()
if usage_csv:
    ut, ug = [], []
    with open(usage_csv) as f:
        for r in csv.DictReader(f):
            ut.append(datetime.strptime(r["date"], "%Y-%m-%d")); ug.append(float(r["used_gb"]))
    ax2.plot(ut, ug, "r-", label="Data used (GB)")
elif len(used) == len(t):
    ax2.plot(t, used, "r-", label="Data used (GB, router counter)")
ax2.set_ylabel("Cumulative data used (GB)")

fig.legend(loc="upper left")
plt.title("Advertised 'onbeperkt' vs. measured speed")
fig.tight_layout()
plt.savefig(out, dpi=150)
print(f"saved {out}")
