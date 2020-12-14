# Control Pi Multiroom Audio

## Project Init

https://start.spring.io/#!type=maven-project&language=kotlin&platformVersion=2.4.1.RELEASE&packaging=jar&jvmVersion=11&groupId=org.unividuell&artifactId=control-pi-audio-multiroom&name=control-pi-audio-multiroom&description=Control%20over%20Audio%20Mutliroom%20Raspberry%20Pi&packageName=org.unividuell.pi.audio.multiroom&dependencies=web,devtools,data-jdbc,flyway,postgresql,websocket,actuator,testcontainers

## Pi setup

```
# on build machine
./mvnw package
scp target/control-pi-audio-multiroom-1.0.0.jar pi@white.local:/tmp
scp control-pi-audio-multiroom.service pi@white.local:/tmp
# on execution machine
sudo apt-get install openjdk-11-jre

sudo mv /tmp/control-pi-audio-multiroom-1.0.0.jar /usr/local/bin/
sudo mv /tmp/control-pi-audio-multiroom.service /etc/systemd/system/
sudo touch /var/log/control-pi-audio-multiroom.log
sudo chown pi:pi /var/log/control-pi-audio-multiroom.log
sudo systemctl enable control-pi-audio-multiroom.service
```