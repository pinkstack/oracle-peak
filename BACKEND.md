# Oracle Peak - Back-end and configuration

## The short version

1. Prepare Google Cloud Platform (GCP) with Google Kubernetes Engine (GKE)
2. Prepare Confluent Cloud Platform (CC) with Schema Registry
3. Deploy HELM charts to GKE Kubernetes cluster

    1. Install Influxdb
    2. Install Grafana (optional)
    3. Install Kafka Connect

        1. Configure Kafka Connectors

            1. Configure MQTT to Kafka (Source)
            2. Configure Kafka to MQTT (Sink)

## The long version

### GCP and GKE

Create an account at [Google Cloud Console](https://cloud.google.com/) and simple project.

Then follow the guideline on setting
up [Google Kubernetes Engine](https://www.google.com/search?q=gke+tutorial&oq=gke+tutorial&aqs=chrome..69i57.35071j0j7&sourceid=chrome&ie=UTF-8)
here.

```bash
gcloud beta container --project "oracle-peak" clusters create "peak" # ....
```

Connect to cluster

```bash
gcloud container clusters get-credentials peak --zone europe-west3-a --project oracle-peak
```

### Confluent Cloud Platform

Create an account at [Confluent Cloud](https://www.confluent.io/confluent-cloud/) signup page.

Then follow the [documentation here](https://docs.confluent.io/home/overview.html).

### HELM Charts

#### Install InfluxDB

```bash
kubectl create namespace tick && kubens tick
helm install snow influxdata/influxdb -n tick
helm install rain influxdata/chronograf -n tick
```

#### Install Grafana

```bash
helm install cookie bitnami/grafana -n tick
```

#### Install / setup Kafka Connect

Set environment variables that will be passed to HELM to install Kafka Connect in a way that it will be connected to
your Confluent Cloud Kafka environment.

Please also note that this setup is using modified Kafka Connect Docker image with pre-installed Kafka Connect Plugins
for MQTT and InfluxDB.

```bash
CCLOUD_API_BOOTSTRAP_SERVERS="your-confluent-cloud-setup"
CCLOUD_API_KEY="key"
CCLOUD_API_SECRET="secret"
```

Run HELM install, by passing these values:

```bash
#TODO check kafka-connect/update-deployment.sh
```

Don't forget to also set these things when upgrading setup.

```bash
#TODO check kafka-connect/update-deployment.sh

```

##### Configure Kafka Connectors

Example configuration for MQTT to Kafka connector

```json
  {
  "name": "mqtt-to-wifi-sessions",
  "config": {
    "connector.class": "io.confluent.connect.mqtt.MqttSourceConnector",
    "key.converter": "org.apache.kafka.connect.storage.StringConverter",
    "value.converter": "org.apache.kafka.connect.converters.ByteArrayConverter",
    "errors.retry.timeout": "-1",
    "errors.log.enable": "true",
    "errors.log.include.messages": "true",
    "mqtt.server.uri": "tcp://mqtt.eclipse.org",
    "confluent.topic.bootstrap.servers": "<server>",
    "kafka.topic": "development-sessions",
    "mqtt.topics": "oracle-peak-staging/+/+/session",
    "confluent.topic.client.dns.lookup": "use_all_dns_ips",
    "confluent.topic.security.protocol": "SASL_SSL",
    "confluent.topic.sasl.jaas.config": "org.apache.kafka.common.security.plain.PlainLoginModule   required username='CCLOUD_API_KEY'   password='CCLOUD_API_SECRET';",
    "confluent.topic.acks": "all",
    "confluent.topic.sasl.mechanism": "PLAIN"
  }
}
```

Table of MQTT to topic mapping

- oracle-peak-staging/+/+/session => development-sessions
- oracle-peak-staging/+/+/events => development-wifi-events
- oracle-peak-staging/+/+/access-points => development-access-points

### Development tips