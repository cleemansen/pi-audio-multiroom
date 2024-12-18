#!/bin/bash

# print out every line as it executes
set -x
# armhf: raspberry pi 1 (B rev 2)
SL_VERSION=2.0.0.1486-armhf
mkdir /tmp/$SL_VERSION
wget -O /tmp/$SL_VERSION/squeezelite.tar.gz https://sourceforge.net/projects/lmsclients/files/squeezelite/linux/squeezelite-$SL_VERSION.tar.gz
tar -xvzf /tmp/squeezelite.tar.gz -C /tmp/$SL_VERSION
sudo mv /tmp/$SL_VERSION/squeezelite /usr/local/bin
sudo chmod +x /usr/local/bin/squeezelite
