# Control Pi Multiroom Audio (Ktor variant)

## Pi setup

```
# on build machine
./mvnw package
scp target/pictl-ktor-1.0.0-jar-with-dependencies.jar pi@white.local:pictl.jar
scp pictl.service pi@white.local:~/

# on execution machine
sudo apt-get install openjdk-11-jre

# sudo mv /tmp/pictl.jar /usr/local/bin/
sudo mv /tmp/pictl.service /etc/systemd/system/
sudo touch /var/log/pictl.log
sudo chown pi:pi /var/log/pictl.log
# test service with sudo systemctl start pictl.service
sudo systemctl enable pictl.service
```