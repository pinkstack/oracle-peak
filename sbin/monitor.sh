#!/usr/bin/env bash
set -e

(sudo airodump-ng wlan1mon \
  --beacons \
  --uptime \
  --wps \
  --manufacturer \
  --showack 2>&1 | tee - | socat STDIO TCP-LISTEN:7000,reuseaddr,fork,setsid) 2>&1 &
PID=$!

# </dev/null >/dev/null

echo "Started with PID=${PID}"
