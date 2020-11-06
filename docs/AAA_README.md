# How to setup server side

1) Install (Confluent platform) Kafka

```bash
helm install one confluentinc/cp-helm-charts --values kubernetes/kafka-values.yaml
```

2) Install some monitoring

3) Setup connector MQTT => Kafka topic

- [MQTT 2 Kafka (Kafka Connector JSON)](mqtt-to-wifi-sessions.connector.json)

4) Neo4j