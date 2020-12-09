# KSQLDB notes

```sql
CREATE STREAM wifi_events_json (
        key STRING,
        agent_version STRING,
        client_id STRING,
        location STRING,
        tag STRING,
        collected_at STRING, -- this should be set
        `time` VARCHAR,
        `data` STRUCT<
            alias STRING,
            essid STRING,
            mac STRING,
            rssi INTEGER,
            vendor STRING,
            authentication STRING,
            channel INTEGER,
            cipher STRING,
            -- clients
            encryption STRING,
            first_seen STRING,
            frequency INTEGER,
            -- handshake
            hostname STRING,
            ipv4 STRING,
            ipv6 STRING,
            -- last_seen
            -- meta (map)
            received BIGINT,
            -- wps
            sent BIGINT>
) WITH (
    KAFKA_TOPIC='development-wifi-events',
    VALUE_FORMAT='json',
    -- timestamp='collected_at',
    -- timestamp_format='yyyy-MM-dd''T''HH:mm:ss.SSSSSS''Z''',
    key='key'
);
```


```
SET 'auto.offset.reset' = 'earliest';
```
