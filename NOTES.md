#  Notes

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

### Management

To manage and operate the collection nodes please use the following Ansible based scripts and tools.

```bash
mkvirtualenv --python=/usr/local/Cellar/python@3.8/3.8.5/bin/python3 oracle-peak
pip install -r requirements.txt --upgrade pip
```

#### Playbooks

- [wifi-enable-monitoring.yml](playbooks/wifi-enable-monitoring.yml) - Put WiFi card into "monitoring mode".
- [turn-leds-off.yml](playbooks/turn-leds-off.yml) - Turn all RPi LED's OFF
- [turn-leds-on.yml](playbooks/turn-leds-on.yml) - Turn all RPi LED's ON

```bash
$ sudo airmon-ng start wlx00c0caac1f73

docker run -it --rm -p 8081:8081 --privileged --net=host bettercap/dev -caplet http-ui -eval "set api.rest on; set wifi.interface wlx00c0caac1f73; wifi.recon on" --no-colors

```

