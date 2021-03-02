# Raspberry PI B (000e)

Q4 2012 / 512MB

## WiFi

Default `wpa_supplicant` seems not to work. [Fix it by using the `wext` driver](https://unix.stackexchange.com/a/400113)
and the [`/etc/network/interfaces` file content](https://raspberrypi.stackexchange.com/a/65628/79233).

[Full story]()

The important part aka **the fix:**

1. Start fresh Buster installation (with eth0 connected)
2. open `raspi-config`
3. navigate to WiFi setup -> will fail directly with `Could not communicate with wpa_supplicant`
4. close `raspi-config`
5. execute `sudo wpa_supplicant -iwlan0 -D wext -c/etc/wpa_supplicant/wpa_supplicant.conf -B`
6. setup WiFi via `raspi-config` once again - now it should work
7. edit `/etc/network/interfaces`

```
# interfaces(5) file used by ifup(8) and ifdown(8)

# Please note that this file is written to be used with dhcpcd
# For static IP, consult /etc/dhcpcd.conf and 'man dhcpcd.conf'

# Include files from /etc/network/interfaces.d:
source-directory /etc/network/interfaces.d

allow-hotplug wlan0
#auto wlan0
iface wlan0 inet dhcp
#pre-up wpa_supplicant -B w -D wext -i wlan0 -c /etc/wpa_supplicant/wpa_supplicant.conf
#post-down killall -q wpa_supplicant
    wpa-conf /etc/wpa_supplicant/wpa_supplicant.conf
```

Downside: `dhcpcd` seems not to work completely :/ `systemctl status dhcpcd`:

```
Failed to start dhcpcd on all interfaces.
```

**But at least it works. :)**
