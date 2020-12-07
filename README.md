# 🏔 The Oracle Peak 🏔

![CI](https://github.com/pinkstack/oracle-peak/workflows/CI/badge.svg?branch=master)


## The Mission

The purpose of the Oracale Peak experiments and research is to explore and build real-time software 
needed to visualise and understand the activity and behaviour of Wi-Fi users and their devices.

The core objectives of the projects are

1. Build or use tools for **information extraction** from WiFi Access Points (APs) and Wi-Fi Clients.

    With specific focus on key data points like `BSSID`s, `ESSID`s, `MAC`'s, Authentication algorithms, 
    manufacturer information and time related activity.
    
2. Build a **real-time** and low latency resilient data pipelining.
3. Clean, transform and [**visualize**](https://github.com/pinkstack/oracle-peak-ui) the data in respectful and anonymized way.

## Collection

### Software - Oracle Peak Agent

One of the key components of this system is [oracle-peak-agent](oracle-peak/agent), an agent that needs to be deployed
either on the device where collection is happening (Respeberry Pi in following example) or on some external 
service with the access to Bettercap (REST API).

#### Setup

1. Install Bettercap by following [this instructions](https://www.bettercap.org/installation/).
2. Install Docker by following [this instructions (for RPi v3)](https://phoenixnap.com/kb/docker-on-raspberry-pi).
3. Install the GPSD deamon with [this instructions](https://gpsd.gitlab.io/gpsd/installation.html). This step is optional.
4. Run the following command on RPi (v3) that will boot the agent. 
The agent will then read data from Bettercap's REST API endpoints transform it 
and feed to MQTT topics. 

```bash
docker run -d --name=agent \ 
    -e MQTT_CLIENT_ID=device-one \
    -e MQTT_ROOT_TOPIC=oracle-peak-staging/location-one \
    -e BETTERCAP_URL=http://127.0.0.1:8081 \
    -e GPSD_URL=gpsd://127.0.0.1:2947 \
    --network=host \
    --restart=always \
    ghcr.io/pinkstack/oracle-peak-agent-arm32v7:latest
```

#### MQTT Topics

##### `oracle-peak-staging/+/+/events`

Real-time feed of Bettercap events enriched with additional fields like `agent_version`, 
`location`, `client_id`, `collected_at` and `key`. Example payload might be something like:

```json
{
  "agent_version": "0.1.0",
  "client_id": "blackbox",
  "collected_at": "2020-11-27T09:08:01.758994Z",
  "data": {
    "alias": "",
    "essid": "Upstairs",
    "mac": "AA:AA:AA:AA:AA:AA",
    "rssi": -82,
    "vendor": "Liteon Technology Corporation"
  },
  "key": "f3c607d69ad06766b30a69a4c919f019f7fbecf772447b0537c3fb2605812d42",
  "location": "location-one",
  "tag": "wifi.client.probe",
  "time": "2020-11-27T17:37:12.048623715Z"
}
```

##### `oracle-peak-staging/+/+/session`

WiFi Access Points section of current Bettercap session. 
Additional fields are also present - `agent_version`, `location`, `client_id`, `collected_at`.

##### `oracle-peak-staging/+/+/access-points`

Similar to MTT topic `oracle-peak-staging/+/+/session`, this one contains WiFi Access Point information, 
collected in as part of session, however this topic has sessions split per individual APs. 

This topic might be useful if there is demand for purely access point data.

Some additional fields like `agent_version`, `location`, `client_id`, `collected_at` are also added.

```json
{
  "agent_version": "0.0.17",
  "alias": "",
  "authentication": "PSK",
  "channel": 8,
  "cipher": "CCMP",
  "client_id": "development-box",
  "clients": [],
  "collected_at": "2020-12-02T00:24:14.441663Z",
  "encryption": "WPA2",
  "first_seen": "2020-12-02T00:16:57.9268976+01:00",
  "frequency": 2447,
  "handshake": false,
  "hostname": "UpstairsX",
  "ipv4": "0.0.0.0",
  "ipv6": "",
  "last_seen": "2020-12-02T00:19:26.374822916+01:00",
  "location": "development-location",
  "mac": "e8:de:27:b3:53:64",
  "received": 0,
  "rssi": -82,
  "sent": 0,
  "vendor": "Tp-Link Technologies Co.,Ltd.",
  "wps": {
    "AP Setup Locked": "01",
    "Config Methods": "Ethernet, Push Button, Label",
    "Device Name": "Wireless Router TL-WR841N",
    "Manufacturer": "TP-LINK",
    "Model Name": "TL-WR841N",
    "Model Number": "9.0",
    "Primary Device Type": "AP (oui:0000f204)",
    "RF Bands": "2.4Ghz",
    "Response Type": "AP",
    "Serial Number": "1.0",
    "State": "Configured",
    "UUID-E": "1111",
    "Vendor Extension": "1111",
    "Version": "1.0"
  }
}
```

##### `oracle-peak-staging/+/+/location`

Oracle Peak Agent is also able to consume and emit GPS coordinates via GPSD daemon; 
and if configured publish them to MQTT topics. The payload looks like this:

```json
{
  "agent_version": "0.1.0",
  "client_id": "nuc",
  "collected_at": "2020-11-27T17:41:40.114371Z",
  "location": {
    "lat": 46.0000000,
    "lon": 14.0000000
  }
}
```

##### `oracle-peak-staging/+/+/last-update`

This topic can be used for emitting the timestamp on when the session is captured.
It also provides a helpful healthcheck.

##### `oracle-peak-staging/+/+/agent-version`

When doing updates this MQTT topic can be used for monitoring the version of clients that are deployed.
It is very handy and useful for debugging.

#### Drilling-down

For going deeper into specific location or device; the following topic patterns can be used:

- `oracle-peak-staging/some-location/+/agent-version`
- `oracle-peak-staging/some-location/some-device/agent-version`

#### Systemd on Agents

> 💡 You can find systemd service definition examples for Bettercap [here](utils/bettercap.service) 
> or [here](utils/bettercap-nuc.service). It might be wise to start Bettercap at system's boot sequence. 
> Meaning if device used for monitoring goes down oracle-peak-agent and Bettercap will be also
> restarted and put back into collection and emitting state.

##### Resilience

The Oracle Peak Agent has been written in a way that if either bettercap or MQTT broker are down it will partially 
restart and back-off with following settings. Making the system more resilient to networking problems.

Backoff strategy for Bettercap service 
- Minimal backoff: 10 seconds
- Maximal backoff: 2 minutes
- Random factor: 40%

Backoff strategy for MQTT broker service 
- Minimal backoff: 6 seconds
- Maximal backoff: 20 minutes
- Random factor: 20%

#### Environment variables

- `CLIENT_ID=oracle-peak-development-box`
- `LOCATION=development-location`
- `MQTT_BROKER=tcp://mqtt.eclipse.org`
- `MQTT_ROOT_TOPIC=oracle-peak-development`
- `MQTT_EMIT=true`
- `BETTERCAP_URL=http://192.168.33.33:3333`
- `BETTERCAP_USER=user`
- `BETTERCAP_PASSWORD=pass`
- `BETTERCAP_ENABLED=true`
- `BETTERCAP_EVENTS_ENABLED=true`
- `GPSD_URL=gpsd://127.0.0.1:2947`
- `GPSD_ENABLED=true`
- `AKKA_LOG_LEVEL=WARNING`

#### Images

The Oracle Peak Agent comes pre-compiled and pre-packaged for following architectures respectfully:

- [`x86_64`](https://github.com/orgs/pinkstack/packages/container/package/oracle-peak-agent) - [`ghcr.io/pinkstack/oracle-peak-agent:latest`](https://github.com/orgs/pinkstack/packages/container/package/oracle-peak-agent)
- [`arm32v7`](https://github.com/orgs/pinkstack/packages/container/package/oracle-peak-agent-arm32v7) - [`ghcr.io/pinkstack/ooracle-peak-agent-arm32v7:latest`](https://github.com/orgs/pinkstack/packages/container/package/oracle-peak-agent-arm32v7)

#### Upgrading and device management

For this research we used Ansible and [our Ansible Playbooks](playbooks) to pull and run Docker containers for oracle-peak-agent.

```bash
$ ansible-playbook playbooks/update-agents.yml
```

Obviously you would need to define your own inventory; but feel free to take inspiration in [ours](./inventory.yaml); especially if you want to
deploy this on multiple devices with multiple architectures.

### Hardware (WIP)

The current setup (although not yet finalised) for collection part of this project consists of

- [Raspberry Pi 3 Model B](https://www.raspberrypi.org/products/raspberry-pi-3-model-b/)
- [Alfa Network AWUS036NH USB 2.0 Highpower WLAN Adapter and 5dBi antenna](https://wlan-profi-shop.de/Alfa-Network-AWUS036NH/GE-RT3070-USB-20-Highpower-WLAN-Adapter-2000mW-2W-and-5dBi-antenna)
- [Lumsing 60W 6 Ports Desktop USB Charger](https://www.amazon.co.uk/Lumsing-Desktop-Charger-Intelligent-Motorola-blue/dp/B01N2LCNED)

### Visualisation layer - *Oracle Peak UI*

The real-time feeds and data are visible and browsable through the [oracle-peak-ui](https://github.com/pinkstack/oracle-peak-ui) single-page application (SPA).

## Development

### Agent - Local development

```bash
$ sbt "project agent; run"
```

## Authors

- [Oto Brglez](https://github.com/otobrglez), [@otobrglez](https://twitter.com/otobrglez)
- [Andraž Sraka](https://github.com/lowk3y), [@lowk3y](https://twitter.com/lowk3y)
