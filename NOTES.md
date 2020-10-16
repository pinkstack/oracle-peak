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