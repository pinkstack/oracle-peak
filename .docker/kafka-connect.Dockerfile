FROM confluentinc/cp-kafka-connect:5.5.0
MAINTAINER Oto Brglez <otobrglez@gmail.com>

ENV CONNECT_PLUGIN_PATH="/usr/share/java,/usr/share/confluent-hub-components"

RUN sed -i 's;http://archive.debian.org/debian;http://deb.debian.org/debian;' /etc/apt/sources.list

RUN echo "deb http://archive.debian.org/debian/ jessie main" > /etc/apt/sources.list && \
    echo "deb http://security.debian.org jessie/updates main" >> /etc/apt/sources.list && \
    echo "deb [arch=amd64] https://s3-us-west-2.amazonaws.com/staging-confluent-packages-5.5.0/deb/5.5 stable main" \
        >> /etc/apt/sources.list

RUN apt-get update -yy && \
    apt-get install apt-utils vim curl -yy --force-yes

RUN confluent-hub install --no-prompt confluentinc/kafka-connect-mqtt:latest && \
    confluent-hub install --no-prompt mongodb/kafka-connect-mongodb:latest && \
    confluent-hub install --no-prompt confluentinc/kafka-connect-influxdb:latest && \
    confluent-hub install --no-prompt confluentinc/kafka-connect-elasticsearch:latest && \
    confluent-hub install --no-prompt confluentinc/kafka-connect-cassandra:latest && \
    confluent-hub install --no-prompt neo4j/kafka-connect-neo4j:latest

# How to build and push new image?
# docker build -t pinkstack/kafka-connect -t pinkstack/kafka-connect:latest -t pinkstack/kafka-connect:0.0.1 -f .docker/kafka-connect.Dockerfile .
# docker push pinkstack/kafka-connect:0.0.1