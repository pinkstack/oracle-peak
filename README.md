# 🏔 The Oracle Peak 🏔

![CI](https://github.com/pinkstack/oracle-peak/workflows/CI/badge.svg?branch=master)


## The Mission

The purpose of the Oracale Peak experiments and research is to explore and build real-time software 
needed to visualise and understand the activity and behaviour of Wi-Fi users and their devices.

The core objectives of the projects are

1. Build or use tools for **information extraction** from WiFi Access Points (APs) and Wi-Fi Clients.
With specific focus on key data points like `BSSID`s, `ESSID`s, `MAC`'s, Authentication algorithms, 
manufacturer information and time related activity.
2. Build **real-time** and low latency resilient pipelining for processing.
3. Clean, transform and **visualize** the data in respectful and anonymized way.

## Collection

### Software - Oracle Peak Agent

One of the key components of this system is [oracle-peak-agent](oracle-peak/agent), an agent that needs to be deployed
either on the device where collection is happening (Respeberry Pi in following example) or on some external 
service with the access to Bettercap (REST API).

#### Setup

1. Install Bettercap by following [this instructions](https://www.bettercap.org/installation/).
2. Install Docker by following [this instructions (for RPi v3)](https://phoenixnap.com/kb/docker-on-raspberry-pi).
3. Run the following command on RPi (v3) that will boot the agent. 
The agent will then read data from Bettercap's REST API endpoints transform it 
and feed to MQTT topic of your chose. 

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

> 💡 You can find systemd service definition examples for Bettercap [here](utils/bettercap.service) or [here](utils/bettercap-nuc.service). 
> It might be wise to start Bettercap at system's boot sequence. Meaning if device used for monitoring
> goes down oracle-peak-agent and Bettercap will be also restarted and put back into collection and emitting state.

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

- `MQTT_BROKER=tcp://mqtt.eclipse.org`
- `MQTT_CLIENT_ID=oracle-peak-development-client-XXX`
- `MQTT_ROOT_TOPIC=oracle-peak-development/experiment-2`
- `MQTT_EMIT=true`
- `BETTERCAP_URL=http://192.168.33.33:3333`
- `BETTERCAP_USER=user`
- `BETTERCAP_PASSWORD=pass`
- `GPSD_URL=gpsd://127.0.0.1:2947`

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


## Development

### Agent - Local development

```bash
$ sbt "project agent; run"
```

## Authors

- [Oto Brglez](https://github.com/otobrglez), [@otobrglez](https://twitter.com/otobrglez)
- [Andraž Sraka](https://github.com/lowk3y), [@lowk3y](https://twitter.com/lowk3y)
