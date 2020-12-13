CREATE
STREAM wifi_events_json (
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

CREATE
STREAM wifi_events WITH (KAFKA_TOPIC='wifi_events',VALUE_FORMAT='AVRO')
AS
SELECT *
FROM wifi_events_json
;

-- development-access-points => access_points_json
CREATE
STREAM access_points_json (
    agent_version STRING,
    alias STRING,
    authentication STRING,
    channel INTEGER,
    cipher STRING,
    client_id STRING,
    location STRING,
    clients ARRAY<STRUCT<
        alias STRING,
        authentication STRING,
        channel INTEGER,
        cipher STRING,
        encryption STRING,
        first_seen STRING,
        frequency INTEGER,
        hostname STRING,
        ipv4 STRING,
        ipv6 STRING,
        last_seen STRING,
        mac STRING,
        received INTEGER,
        rssi INTEGER,
        sent INTEGER,
        vendor STRING
    >>,
    collected_at STRING,
    encryption STRING,
    first_seen STRING,
    frequency INTEGER,
    hostname STRING,
    ipv4 STRING,
    ipv6 STRING,
    mac STRING,
    received INTEGER,
    rssi INTEGER,
    sent INTEGER,
    vendor STRING
) WITH (
    KAFKA_TOPIC='development-access-points',
    VALUE_FORMAT='json'
);


-- development-access-points => wifi_ap_clients
CREATE STREAM wifi_ap_clients WITH (
    KAFKA_TOPIC='wifi_ap_clients',
    VALUE_FORMAT='AVRO',
    TIMESTAMP='collected_at_ts'
) AS
SELECT location                                                                      AS location,
       client_id                                                                     AS client_id,
       mac                                                                           AS ap_mac,
       alias                                                                         AS ap_alias,
       channel                                                                       AS ap_channel,
       vendor                                                                        AS ap_vendor,
       frequency                                                                     AS ap_frequency,
       rssi                                                                          AS ap_rssi,
       hostname                                                                      AS ap_hostname,
       explode(clients)->mac                                                      AS mac,
       explode(clients)->alias                                                    AS alias,
       explode(clients)->hostname                                                 AS hostname,
       explode(clients)->rssi                                                     AS rssi,
       explode(clients)->vendor                                                   AS vendor,
       explode(clients)->frequency                                                AS frequency,  -- bug!
       explode(clients)->sent                                                     AS sent,
       explode(clients)->received                                                 AS received,
       STRINGTOTIMESTAMP(collected_at, 'yyyy-MM-dd''T''HH:mm:ss.SSSSSS''Z''', 'UTC') AS collected_at_ts
FROM access_points_json
;

SET 'auto.offset.reset' = 'earliest';

CREATE
STREAM wifi_devices WITH (
    KAFKA_TOPIC='wifi_devices',
    VALUE_FORMAT='JSON'
)
AS
SELECT collected_at_ts as "time",
       'presence' as "name",
       1          as "present",
       rssi       as "rssi",
       ap_rssi    as "ap_rssi",
       map(
               'ap_mac'         :=ap_mac,
               'ap_vendor'      :=ap_vendor,
               'ap_hostname'    :=ap_hostname,
               'mac'            :=mac,
               'hostname'       :=hostname,
               'vendor'         :=vendor,
               'client_id'      :=client_id,
               'location'       :=location
           )      AS "tags"
FROM wifi_ap_clients
;
