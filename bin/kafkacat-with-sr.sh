#!/usr/bin/env bash
set -ex

./bin/kafkacat.sh \
  -s avro \
  -r https://${CCLOUD_SCHEMA_KEY_ENCODED}:${CCLOUD_SCHEMA_SECRET_ENCODED}@${CCLOUD_SCHEMA_REGISTRY_NO_PROTOCOL} \
  "$@"
