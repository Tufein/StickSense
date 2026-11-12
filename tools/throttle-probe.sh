#!/usr/bin/env bash
# throttle-probe.sh - classify HOW a connection is slowed, using only normal downloads.
# Run from a wired host. It changes nothing on your router or modem.
#
# It measures the same transfer across: several hosts, IPv4 vs IPv6, 1 vs 4 parallel
# streams. Interpretation:
#   all rows slow equally            -> account/line-level shaping (volume FUP, applied upstream)
#   one host/protocol slow, rest fast-> traffic-class or peering issue (not a volume FUP)
#   IPv4 slow, IPv6 fast (or reverse)-> per-protocol path difference, worth reporting
#   1 stream slow, 4 streams fast    -> per-flow limit or TCP/latency issue, not a hard cap
#
# Usage: ./throttle-probe.sh [out.csv]
set -uo pipefail
OUT="${1:-throttle-probe.csv}"
SIZE=50000000   # 50 MB per stream; ~400 MB of traffic per full run, mind your own cap
URLS=(
  "cloudflare|https://speed.cloudflare.com/__down?bytes=${SIZE}"
  "ovh|https://proof.ovh.net/files/100Mb.dat"
  "tele2|https://speedtest.tele2.net/100MB.zip"
)
[[ -f "$OUT" ]] || echo "timestamp_utc,host,ip_version,streams,aggregate_mbps" > "$OUT"

one() { # url ipflag -> bytes/sec to stdout (cap at SIZE bytes)
  curl -s -o /dev/null --max-filesize 0 -r 0-$((SIZE-1)) "$2" -w '%{speed_download}\n' --max-time 60 "$1" 2>/dev/null || echo 0
}

for entry in "${URLS[@]}"; do
  host="${entry%%|*}"; url="${entry#*|}"
  for ipv in 4 6; do
    for n in 1 4; do
      tmp="$(mktemp)"
      for _ in $(seq "$n"); do one "$url" "-$ipv" >> "$tmp" & done; wait
      mbps="$(awk '{s+=$1} END {printf "%.1f", s*8/1e6}' "$tmp")"; rm -f "$tmp"
      echo "$(date -u +%FT%TZ),$host,v$ipv,$n,$mbps" | tee -a "$OUT"
    done
  done
done
