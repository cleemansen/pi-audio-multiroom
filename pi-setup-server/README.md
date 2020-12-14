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
