#!/bin/zsh

# echo each command
set -x
# abort script on failing commands
set -e


pip freeze > requirements.txt
tar -czvf shutdown.tar.gz app.py requirements.txt shutdown.service

scp shutdown.tar.gz install-shutdown.sh clemens@192.168.0.110:/tmp
