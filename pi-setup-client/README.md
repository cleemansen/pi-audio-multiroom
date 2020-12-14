# Setup for Multiroom Client

## PiBackery

## Raspian

```
# update OS
sudo apt-get -y update && sudo apt-get -y dist-upgrade
# configure soundcard
sudo alsamixer
```

## Squeezelite

http://www.gerrelt.nl/RaspberryPi/wordpress/tutorial-installing-squeezelite-player-on-raspbian/

### installing

```
sudo apt-get install -y libflac-dev libfaad2 libmad0
mkdir /tmp/squeezelite
cd /tmp/squeezelite
# find newest version on https://sourceforge.net/projects/lmsclients/files/squeezelite/linux/
wget -O squeezelite-armhf.tar.gz https://sourceforge.net/projects/lmsclients/files/squeezelite/linux/squeezelite-1.9.8.1294-armhf.tar.gz
tar -xvzf squeezelite-armhf.tar.gz
mv squeezelite squeezelite-armv6hf

sudo mv squeezelite-armv6hf /usr/bin
sudo chmod a+x /usr/bin/squeezelite-armv6hf
```

### configure soundcard for squeezelite

```
sudo /usr/bin/squeezelite-armv6hf -l

```
