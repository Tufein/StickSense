#!/usr/bin/env bash
# speedlog.sh - append one speed measurement to a CSV for evidence.
# Run from a WIRED machine behind the modem. Example cron (every 30 min):
#   */30 * * * * /path/to/tools/speedlog.sh /path/to/evidence/speed/speedlog.csv
#
# Needs one of:
#   - Ookla CLI 'speedtest' (https://www.speedtest.net/apps/cli)  [preferred]
#   - 'speedtest-cli' (pip install speedtest-cli)
# Optional: 'iperf3' + IPERF_SERVER env var for a second, independent measurement.
# Optional: WAN_IFACE env var (e.g. eth0) to log interface byte counters (on a router/Linux gateway).
set -euo pipefail

OUT="${1:-./speedlog.csv}"
TS="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
DL=""; UL=""; PING=""; SERVER=""; TOOL=""

if command -v speedtest >/dev/null 2>&1 && speedtest --version 2>/dev/null | grep -qi ookla; then
  TOOL="ookla"
  J="$(speedtest --accept-license --accept-gdpr -f json 2>/dev/null || true)"
  if [[ -n "$J" ]]; then
    read -r DL UL PING SERVER < <(python3 -c '
import json,sys
d=json.loads(sys.argv[1])
print(round(d["download"]["bandwidth"]*8/1e6,2), round(d["upload"]["bandwidth"]*8/1e6,2),
      round(d["ping"]["latency"],1), d["server"]["name"].replace(" ","_"))' "$J")
  fi
elif command -v speedtest-cli >/dev/null 2>&1; then
  TOOL="speedtest-cli"
  # CSV columns: id,sponsor,name,timestamp,distance,ping,download(bps),upload(bps),...
  R="$(speedtest-cli --csv 2>/dev/null || true)"
  if [[ -n "$R" ]]; then
    read -r DL UL PING SERVER < <(python3 -c '
import csv,sys
r=next(csv.reader([sys.argv[1]]))
print(round(float(r[6])/1e6,2), round(float(r[7])/1e6,2), r[5], r[1].replace(" ","_"))' "$R")
  fi
else
  echo "No speedtest tool found" >&2; exit 1
fi

IPERF_DL=""
if [[ -n "${IPERF_SERVER:-}" ]] && command -v iperf3 >/dev/null 2>&1; then
  IPERF_DL="$(iperf3 -c "$IPERF_SERVER" -R -t 10 -J 2>/dev/null \
    | python3 -c 'import json,sys; print(round(json.load(sys.stdin)["end"]["sum_received"]["bits_per_second"]/1e6,2))' 2>/dev/null || true)"
fi

RX=""; TX=""
if [[ -n "${WAN_IFACE:-}" && -r "/sys/class/net/${WAN_IFACE}/statistics/rx_bytes" ]]; then
  RX="$(cat /sys/class/net/${WAN_IFACE}/statistics/rx_bytes)"
  TX="$(cat /sys/class/net/${WAN_IFACE}/statistics/tx_bytes)"
fi

[[ -f "$OUT" ]] || echo "timestamp_utc,tool,download_mbps,upload_mbps,ping_ms,server,iperf_download_mbps,wan_rx_bytes,wan_tx_bytes" > "$OUT"
echo "${TS},${TOOL},${DL},${UL},${PING},${SERVER},${IPERF_DL},${RX},${TX}" >> "$OUT"
