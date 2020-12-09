#!/usr/bin/env bash
set -ex

echo "Variables are"
echo $CCLOUD_API_BOOTSTRAP_SERVERS
echo $CCLOUD_API_KEY
echo $CCLOUD_API_SECRET
echo $CCLOUD_SCHEMA_KEY
echo $CCLOUD_SCHEMA_REGISTRY
echo $CCLOUD_SCHEMA_SECRET

helm upgrade one confluentinc/cp-helm-charts \
  --values kafka-connect/kafka-values.yaml \
  --set cp-kafka-connect.kafka.bootstrapServers=SASL_SSL://${CCLOUD_API_BOOTSTRAP_SERVERS} \
  --set cp-ksql-server.kafka.bootstrapServers="SASL_SSL://${CCLOUD_API_BOOTSTRAP_SERVERS}" \
  --set cp-kafka-connect.configurationOverrides."bootstrap\.servers"=${CCLOUD_API_BOOTSTRAP_SERVERS} \
  --set cp-kafka-connect.configurationOverrides."producer\.bootstrap\.servers"=${CCLOUD_API_BOOTSTRAP_SERVERS} \
  --set cp-kafka-connect.configurationOverrides."consumer\.bootstrap\.servers"=${CCLOUD_API_BOOTSTRAP_SERVERS} \
  --set cp-kafka-connect.configurationOverrides."reporter\.admin\.bootstrap\.servers"=${CCLOUD_API_BOOTSTRAP_SERVERS} \
  --set cp-kafka-connect.configurationOverrides."reporter\.producer\.bootstrap.servers"=${CCLOUD_API_BOOTSTRAP_SERVERS} \
  --set cp-kafka-connect.configurationOverrides."sasl\.jaas\.config"="org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${CCLOUD_API_KEY}\" password=\"${CCLOUD_API_SECRET}\";" \
  --set cp-kafka-connect.configurationOverrides."producer\.sasl\.jaas\.config"="org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${CCLOUD_API_KEY}\" password=\"${CCLOUD_API_SECRET}\";" \
  --set cp-kafka-connect.configurationOverrides."consumer\.sasl.jaas\.config"="org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${CCLOUD_API_KEY}\" password=\"${CCLOUD_API_SECRET}\";" \
  --set cp-kafka-connect.configurationOverrides."reporter\.admin\.sasl\.jaas\.config"="org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${CCLOUD_API_KEY}\" password=\"${CCLOUD_API_SECRET}\";" \
  --set cp-kafka-connect.configurationOverrides."reporter\.producer\.sasl\.jaas\.config"="org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${CCLOUD_API_KEY}\" password=\"${CCLOUD_API_SECRET}\";" \
  --set cp-kafka-connect.configurationOverrides."value\.converter"="io.confluent.connect.avro.AvroConverter" \
  --set cp-kafka-connect.configurationOverrides."value\.converter\.basic\.auth\.credentials\.source"="USER_INFO" \
  --set cp-kafka-connect.configurationOverrides."value\.converter\.schema\.registry\.basic\.auth\.user\.info"="${CCLOUD_SCHEMA_KEY}:${CCLOUD_SCHEMA_SECRET}" \
  --set cp-kafka-connect.configurationOverrides."value\.converter\.schema\.registry\.url"=${CCLOUD_SCHEMA_REGISTRY} \
  --set cp-ksql-server.configurationOverrides."sasl\.jaas\.config"="org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${CCLOUD_API_KEY}\" password=\"${CCLOUD_API_SECRET}\";" \
  --set cp-ksql-server.configurationOverrides."ksql\.schema\.registry\.basic\.auth\.credentials\.source"="USER_INFO" \
  --set cp-ksql-server.configurationOverrides."ksql\.schema\.registry\.basic\.auth\.user\.info"="${CCLOUD_SCHEMA_KEY}:${CCLOUD_SCHEMA_SECRET}" \
  --set cp-ksql-server.configurationOverrides."ksql\.schema\.registry\.url"=${CCLOUD_SCHEMA_REGISTRY} \
  --set cp-ksql-server.configurationOverrides."basic\.auth\.credentials\.source"="USER_INFO" \
  --set cp-ksql-server.configurationOverrides."schema\.registry\.basic\.auth\.user\.info"="${CCLOUD_SCHEMA_KEY}:${CCLOUD_SCHEMA_SECRET}" \
  --set cp-ksql-server.configurationOverrides."schema\.registry\.basic\.auth\.credentials\.source"="USER_INFO" \
  --set cp-ksql-server.configurationOverrides."schema\.registry\.basic\.auth\.user\.info"="${CCLOUD_SCHEMA_KEY}:${CCLOUD_SCHEMA_SECRET}" \
  --set cp-ksql-server.configurationOverrides."schema\.registry\.url"=${CCLOUD_SCHEMA_REGISTRY}
