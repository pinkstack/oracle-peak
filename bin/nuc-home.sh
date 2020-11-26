#!/usr/bin/env bash
set -ex
ssh -v -2 \
  -L 0.0.0.0:2949:0.0.0.0:2947 \
  -L 0.0.0.0:8083:0.0.0.0:8081 \
  nuc-ngrok cat