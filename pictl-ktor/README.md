# Control Pi Multiroom Audio (Ktor variant)

## Pi setup

### Java

#### ARMv7 (eg Raspberry Pi 3)

```
sudo apt-get install openjdk-11-jre
```

#### ARMv6 (eg Raspberry Pi Zero)

https://webtechie.be/post/2020-08-27-azul-zulu-java-11-and-gluon-javafx-11-on-armv6-raspberry-pi/

```
sudo wget https://cdn.azul.com/zulu-embedded/bin/zulu11.43.100-ca-jdk11.0.9.1-linux_aarch32hf.tar.gz
# follow instructions on mentioned blog-post
```

### pictl

```
# on build machine
# rebuild pictl-vue if neccessary! 
./mvnw package
scp target/pictl-ktor-1.0.0-jar-with-dependencies.jar pi@lab.local:pictl.jar
scp pictl.service pi@lab.local:~/

# on execution machine
sudo mv ~/pictl.service /etc/systemd/system/
sudo touch /var/log/pictl.log
sudo chown pi:pi /var/log/pictl.log
# test service with sudo systemctl start pictl.service
sudo systemctl enable pictl.service
# sudo systemctl start|stop|restart pictl.service
```

## LMS Squeezebox API

### JSON RPC

- [Backend source code](https://github.com/Logitech/slimserver/blob/public/8.0/Slim/Web/JSONRPC.pm)
- Examples
  - [Node: Status, Player, Current Song](https://github.com/legrosmanu/rest-api-squeezebox/blob/master/src/integration/player/SongPlayed.js)
  - [Gist: Overview, some kind of introduction](https://gist.github.com/samtherussell/335bf9ba75363bd167d2470b8689d9f2)
- [Official documentation; everybody mention this CLI-API commands also works for JSON RPC (to be verified)](http://htmlpreview.github.io/?https://raw.githubusercontent.com/Logitech/slimserver/public/8.0/HTML/EN/html/docs/cli-api.html)

### cometd

- [Backend source code](https://github.com/Logitech/slimserver/blob/public/8.0/Slim/Web/Cometd.pm)
- [CometD documentation](https://docs.cometd.org/current/reference/)
- Debugging backend
  - [logging](https://wiki.slimdevices.com/index.php/Logitech_Media_Server_log_file.html)