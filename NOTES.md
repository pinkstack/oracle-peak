## Notes

> This document contains random blubs and peaces,...

### WiFi Config and troubleshooting

- How to get list of available WiFi Devices?
    ```bash
    sudo lshw -C network # => MAC (under serial)
    sudoo ifconfig # look for MAC
    ```
- How to start monitoring?
    ```bash
    sudo airmon-ng start wlan1
    ip link show
    ip a
    ```    
- How to get buttercap?
    ```bash
    sudo apt-get install build-essential \
        libpcap-dev libusb-1.0-0-dev \
        libnetfilter-queue-dev golang -yy
  
    go get -v -u github.com/bettercap/bettercap
    sudo /home/pi/go/bin/bettercap -caplet http-ui -eval "set api.rest.websocket true; set wifi.interface wlan1; wifi.recon on"
    ```
- How to configure Neo4j? 
  ```cypher 
  CREATE CONSTRAINT ON (device:Device) ASSERT device.mac IS UNIQUE;
  CREATE CONSTRAINT ON (device:Device) ASSERT exists(device.mac)
 
 MERGE (ap:Device {mac: "64:6e:ea:74:3b:ax"}) 
 	// ON CREATE SET ap.hostname = "dodo" 
 	ON MATCH SET ap.hostname = "xxxx" 
 RETURN ap;
 MERGE (c_1:Device {mac: "d4:e6:b7:3e:9f:ax3", hostname: ""})
 MERGE (c_2:Device {mac: "a8:9f:ba:e2:5d:5xa", hostname: "xx"});
 MERGE (ap)-[:CONNECTED]->(c_1);
 MERGE (ap)-[:CONNECTED]->(c_2);
 
  ```