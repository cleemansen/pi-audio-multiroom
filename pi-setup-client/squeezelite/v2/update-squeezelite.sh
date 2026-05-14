#!/bin/bash

set -x # print out every line as it executes
set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

# armhf: raspberry pi 1 (B rev 2)
SL_VERSION=2.0.0.1486-armhf
mkdir /tmp/$SL_VERSION
wget -O /tmp/$SL_VERSION/squeezelite.tar.gz https://sourceforge.net/projects/lmsclients/files/squeezelite/linux/squeezelite-$SL_VERSION.tar.gz
tar -xvzf /tmp/$SL_VERSION/squeezelite.tar.gz -C /tmp/$SL_VERSION
sudo mv /tmp/$SL_VERSION/squeezelite /usr/local/bin
sudo chmod +x /usr/local/bin/squeezelite

sudo systemctl daemon-reload
sudo systemctl enable squeezelite.service
sudo systemctl restart squeezelite.service
