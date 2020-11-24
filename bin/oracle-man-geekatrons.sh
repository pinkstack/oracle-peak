#!/usr/bin/env bash
set -ex
ssh -v -2 \
  -L 0.0.0.0:2948:0.0.0.0:2947 \
  -L 0.0.0.0:8082:0.0.0.0:8081 \
  oracle-man-geekatrons cat