#!/bin/bash

set -x

mkdir -p ~/shutdown
cd ~/shutdown
tar -xzvf /tmp/shutdown.tar.gz -C ~/shutdown
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt

sudo mv shutdown.service /etc/systemd/system
sudo systemctl daemon-reload
sudo systemctl enable shutdown.service
sudo systemctl start shutdown.service
