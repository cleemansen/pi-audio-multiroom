# Setup for Multiroom Client

## Image

1. [Raspberry Pi Imager](https://www.raspberrypi.org/software/)
1. [prepare WiFi connection](https://www.raspberrypi.org/documentation/configuration/wireless/headless.md): `cp pi-setup-client/wpa_supplicant.conf /Volumes/boot/`
1. [activate `ssh`](https://www.raspberrypi.org/documentation/remote-access/ssh/README.md): `touch /Volumes/boot/ssh`

## Raspian

```
# find IP of pi and ssh into
# check os version
cat /etc/os-release
# update OS
sudo apt-get -y update && sudo apt-get -y dist-upgrade
# change hostname (or via rasp-config)
sudo vim /etc/hostname
sudo vim /etc/hosts
# reboot

# enable public key ssh login
ssh-copy-id -i id_rsa.pub pi@lab.local # execute from local machine
# change default pw for user pi on pi
passwd


# configure soundcard
sudo alsamixer
```

### Tweaks

- [get rid of warning `-bash: warning: setlocale: LC_ALL: cannot change locale (en_US.UTF-8)`](https://raspberrypi.stackexchange.com/a/51563/79233)

## Squeezelite

http://www.gerrelt.nl/RaspberryPi/wordpress/tutorial-installing-squeezelite-player-on-raspbian/

### installing

```
# sudo apt-get install -y libflac-dev libfaad2 libmad0
mkdir /tmp/squeezelite
cd /tmp/squeezelite
# find newest version on https://sourceforge.net/projects/lmsclients/files/squeezelite/linux/
wget -O squeezelite-armv6hf.tar.gz https://sourceforge.net/projects/lmsclients/files/squeezelite/linux/squeezelite-1.9.8.1294-armhf.tar.gz
tar -xvzf squeezelite-armv6hf.tar.gz
mv squeezelite squeezelite-armv6hf

sudo mv squeezelite-armv6hf /usr/bin
sudo chmod a+x /usr/bin/squeezelite-armv6hf
```

### configure soundcard for squeezelite

```
# inspect available soundcards
sudo /usr/bin/squeezelite-armv6hf -l
# start session
sudo /usr/bin/squeezelite-armv6hf -o front:CARD=Device,DEV=0 -s 192.168.0.100 -n lab.local -d all=info -f /var/log/squeezelite.log
```
