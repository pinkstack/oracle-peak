#!/usr/bin/env bash
set -ex

echo "Variables are"
echo $CCLOUD_API_BOOTSTRAP_SERVERS
echo $CCLOUD_API_KEY
echo $CCLOUD_API_SECRET
echo $CCLOUD_SCHEMA_KEY
echo $CCLOUD_SCHEMA_REGISTRY
echo $CCLOUD_SCHEMA_SECRET

envsubst < kafka-connect/kafka-values.yaml | \
  helm upgrade one confluentinc/cp-helm-charts --values -