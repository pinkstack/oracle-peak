# Setup

## Kafka

### Kafka Connector Configurations

### `wifi-aps-client-sizes`

```json
{
  "name" : "wifi-aps-client-sizes",
  "config" : {
    "confluent.topic.bootstrap.servers" : "kafka-one-cp-kafka-headless:9092",
    "connector.class" : "io.confluent.connect.mqtt.MqttSourceConnector",
    "kafka.topic" : "wifi-aps-client-sizes",
    "key.converter" : "org.apache.kafka.connect.storage.StringConverter",
    "mqtt.server.uri" : "tcp://mqtt.eclipse.org",
    "mqtt.topics" : "oracle-peak-staging/location-one/+/wifi-aps-client-size",
    "name" : "wifi-aps-client-sizes",
    "value.converter" : "org.apache.kafka.connect.converters.ByteArrayConverter"
  }
}
```

### `wifi-aps-sizes`

```json
{
  "name" : "wifi-aps-sizes",
  "config" : {
    "confluent.topic.bootstrap.servers" : "kafka-one-cp-kafka-headless:9092",
    "connector.class" : "io.confluent.connect.mqtt.MqttSourceConnector",
    "kafka.topic" : "wifi-aps-sizes",
    "key.converter" : "org.apache.kafka.connect.storage.StringConverter",
    "mqtt.server.uri" : "tcp://mqtt.eclipse.org",
    "mqtt.topics" : "oracle-peak-staging/location-one/+/wifi-aps-size",
    "name" : "wifi-aps-sizes",
    "value.converter" : "org.apache.kafka.connect.converters.ByteArrayConverter"
  }
}
```

### `wifi-sessions`

```json
{
  "name" : "wifi-sessions",
  "config" : {
    "confluent.topic.bootstrap.servers" : "kafka-one-cp-kafka-headless:9092",
    "connector.class" : "io.confluent.connect.mqtt.MqttSourceConnector",
    "kafka.topic" : "wifi-sessions",
    "key.converter" : "org.apache.kafka.connect.storage.StringConverter",
    "mqtt.server.uri" : "tcp://mqtt.eclipse.org",
    "mqtt.topics" : "oracle-peak-staging/location-one/+/session",
    "name" : "wifi-sessions",
    "value.converter" : "org.apache.kafka.connect.converters.ByteArrayConverter"
  }
}
```