#!/bin/bash

set -x # print out every line as it executes
set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

mkdir -p ~/squeezelite-v2
sudo touch /var/log/squeezelite.log && sudo chown clemens:clemens /var/log/squeezelite.log
cd ~/squeezelite-v2
tar -xzvf /tmp/squeezelite-v2.tar.gz -C ~/squeezelite-v2
sudo mv squeezelite.service /etc/systemd/system

./update-squeezelite.sh
