# Setup for Multiroom Audio Server

It is best to start from the [Mutliroom Audio Client](../pi-setup-client/README.md) as you probably will also need audio output on the server.

## install Logitech Media Server (LMS)

[Great tutorial](http://www.gerrelt.nl/RaspberryPi/wordpress/tutorial-stand-alone-squeezebox-server-and-player-for-bbq/#Installing_LMS_squeezebox_server)

```
# install some libs
sudo apt-get install -y libsox-fmt-all libflac-dev libfaad2 libmad0 libio-socket-ssl-perl
sudo apt --fix-broken install -y
# get the latest nightly build (from downloads.slimdevices.com):
wget -O logitechmediaserver_arm.deb $(wget -q -O - "http://www.mysqueezebox.com/update/?version=8.0.0&revision=1&geturl=1&os=debarm")
sudo dpkg -i logitechmediaserver_arm.deb
```

## configure squeezelite on Server

Set in `/usr/local/bin/squeezelite_settings.sh`: `SB_SERVER_IP="127.0.0.1"`

## configure webradio

1. open the LMS web-UI: http://<rasp-ip>:9000
1. hover over _Favorites_
1. press the pencil icon
1. _New _Favorite_
1. enter **Name** and **URL** to stream

- https://orf-live.ors-shoutcast.at/fm4-q2a
- https://swr-dasding-live.sslcast.addradio.de/swr/dasding/live/mp3/128/stream.mp3

## https for internal server

- Cert by letsencrypt
- challenge by dns-01
- you have to **manually** renew the cert!
- instructions: https://serverfault.com/a/812038
- DNS TXT record in strato fix: https://community.letsencrypt.org/t/howto-acme-v2-dns-challenge-with-strato/57777

```
sudo snap run certbot -d *.unividuell.org --manual --preferred-challenges dns certonly
# add DNS TXT record to strato (use as key only `_acme-challenge` at "TXT und CNAME Records inklusive SPF und DKIM Einstellungen")
sudo vim /etc/caddy/Caddyfile # edit tls directive to point to gnerated cert- and key-file 
sudo chown -R caddy:caddy /etc/letsencrypt
sudo systemctl restart caddy && journalctl -u caddy.service --since "1 minute ago" -f
# add all domains as DNS record pointing to white-ip in pi-hole
```