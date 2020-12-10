#!/usr/bin/env bash
set -e

kafkacat -b ${CCLOUD_API_BOOTSTRAP_SERVERS} \
  -X security.protocol=SASL_SSL \
  -X sasl.mechanisms=PLAIN \
  -X sasl.username=${CCLOUD_API_KEY} \
  -X sasl.password=${CCLOUD_API_SECRET} \
  -X api.version.request=true \
  "$@"
