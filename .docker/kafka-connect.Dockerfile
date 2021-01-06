FROM confluentinc/cp-kafka-connect:6.0.1
MAINTAINER Oto Brglez <otobrglez@gmail.com>

RUN confluent-hub install --no-prompt confluentinc/kafka-connect-mqtt:latest && \
    confluent-hub install --no-prompt mongodb/kafka-connect-mongodb:latest && \
    confluent-hub install --no-prompt confluentinc/kafka-connect-influxdb:latest && \
    confluent-hub install --no-prompt confluentinc/kafka-connect-elasticsearch:latest && \
    confluent-hub install --no-prompt confluentinc/kafka-connect-cassandra:latest && \
    confluent-hub install --no-prompt neo4j/kafka-connect-neo4j:latest && \
    confluent-hub install --no-prompt confluentinc/connect-transforms:latest


# How to build and push new image?
# docker build -t pinkstack/kafka-connect -t pinkstack/kafka-connect:latest -t pinkstack/kafka-connect:0.0.3 -f .docker/kafka-connect.Dockerfile .
# docker push pinkstack/kafka-connect:0.0.3