# The Oracle Peak 🏔

## The Mission

The purpose of the Oracale Peak experiments and research is to explore and build real-time software 
needed to visualise and understand the activity and behaviour of WiFi users and their devices.

The core objectives of the projects are

1. Build or use tools for **information extraction** from WiFi Access Points (APs) and WiFi Clients.
With specific focus on key data points like `BSSID`s, `ESSID`s, `MAC`'s, Authentication algorithms, 
manufacturer information and time related activity.
2. Build **real-time** and low latency resilient pipelining for processing.
3. Clean, transform and **visualize** the data in respectful and anonymized way.

## Collection

### Hardware

The current setup (although not yet finalised) for collection part of this project consists of

- [Raspberry Pi 3 Model B](https://www.raspberrypi.org/products/raspberry-pi-3-model-b/)
- [Alfa Network AWUS036NH USB 2.0 Highpower WLAN Adapter and 5dBi antenna](https://wlan-profi-shop.de/Alfa-Network-AWUS036NH/GE-RT3070-USB-20-Highpower-WLAN-Adapter-2000mW-2W-and-5dBi-antenna)
- [Lumsing 60W 6 Ports Desktop USB Charger](https://www.amazon.co.uk/Lumsing-Desktop-Charger-Intelligent-Motorola-blue/dp/B01N2LCNED)

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


## Authors

- [Oto Brglez](https://github.com/otobrglez)
- [Andraž Sraka](https://github.com/lowk3y)