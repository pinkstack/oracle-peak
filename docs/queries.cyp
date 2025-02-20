// For "development-access-points"
MERGE (accessPoint:AccessPoint:Device {mac: event.mac, hostname: event.hostname})
  ON CREATE SET accessPoint.created = timestamp()
  ON MATCH SET accessPoint.lastSeen = timestamp()

// simpler

MERGE (ap:AccessPoint {mac: event.mac})
  ON CREATE SET ap.created = timestamp(), ap.hostname = event.hostname
  ON MATCH SET ap.lastSeen = timestamp()


// For "wifi_devices"



MATCH(accessPoint:AccessPoint:Device {mac: event.tags.ap_mac, hostname: event.tags.ap_hostname}),
     (client:Client:Device {mac: event.tags.mac})
MERGE (accessPoint)<-[r:CONNECTED]-(client)
  ON CREATE SET accessPoint.created = timestamp(), client.created = timestamp()
  ON MATCH SET accessPoint.lastSeen = timestamp(), client.lastSeen = timestamp()

MERGE
  (accessPoint:AccessPoint:Device {mac: event.tags.ap_mac, hostname: event.tags.ap_hostname})
    <-[r:CONNECTED]-(client:Client:Device {mac: event.tags.mac, hostname: event.tags.hostname})
  ON CREATE SET accessPoint.created = timestamp(), client.created = timestamp()
  ON MATCH SET accessPoint.lastSeen = timestamp(), client.lastSeen = timestamp()

MATCH (ap:AccessPoint {mac: event.tags.ap_mac}),
      (client:Client {mac: event.tags.mac})
MERGE (ap)<-[r:CONNECTED]-(client)
  ON CREATE SET client.created = timestamp()
  ON MATCH SET client.lasSeen = timestamp()

CREATE CONSTRAINT ON (n:AccessPoint) ASSERT n.mac IS UNIQUE;

CREATE CONSTRAINT ON (n:Client) ASSERT n.mac IS UNIQUE;

MATCH (n)
DETACH DELETE n;


/// RUN
MERGE (ap:AccessPoint {mac: event.mac})
  ON CREATE SET ap.created = timestamp(), ap.hostname = event.hostname
  ON MATCH SET ap.lastSeen = timestamp()
;


// We have bug here. timestamps can (should be removed)
MATCH (ap:AccessPoint {mac: event.tags.ap_mac})
MERGE (ap)<-[r:CONNECTED]-(client:Client {mac: event.tags.mac})
  ON CREATE SET client.created = timestamp()
  ON MATCH SET client.lastSeen = timestamp();

/// s2


MERGE (ap:AccessPoint {mac: event.mac})<-[:BELONGS_TO]-(location:Location {location: event.location})
  ON CREATE SET ap.created = timestamp(), ap.hostname = event.hostname
  ON MATCH SET ap.lastSeen = timestamp()


//
MATCH (ap:AccessPoint {hostname: event.data.essid})
MERGE (ap)<-[r:PROBING]-(client:Client {mac: event.data.mac})

// S3
// q1
MATCH (ap:AccessPoint {mac: event.mac}), (l:Location {location: event.location})
MERGE (ap)-[r:DETECTED]->(l)



// q2

MERGE (l:Location {location: event.location})
MERGE (ap:AccessPoint {mac: event.mac, hostname: event.hostname})
MERGE (ap)<-[:BELONGS_TO]-(l)

MERGE (ap:AccessPoint {mac: event.tags.ap_mac, hostname: event.tags.ap_hostname})
MERGE (client:Client {mac: event.tags.mac})
MERGE (ap)<-[r:CONNECTED]-(client)

MATCH (existingAp:AccessPoint {hostname: event.data.essid})
MERGE (ap:AccessPoint {mac: existingAp.mac, hostname: event.data.essid})
MERGE (client:Client {mac: event.data.mac})
MERGE (ap)<-[r:PROBING]-(client)