#!/bin/bash

# echo each command
set -x
# abort script on failing commands
set -e

mkdir -p ~/shutdown
sudo touch /var/log/shutdown.log && sudo chown clemens:clemens /var/log/shutdown.log
cd ~/shutdown
tar -xzvf /tmp/shutdown.tar.gz -C ~/shutdown
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt

sudo mv shutdown.service /etc/systemd/system
sudo systemctl daemon-reload
sudo systemctl enable shutdown.service
sudo systemctl restart shutdown.service
