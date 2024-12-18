#!/bin/zsh

# print out every line as it executes
set -x

pip freeze > requirements.txt
tar -czvf shutdown.tar.gz app.py requirements.txt shutdown.service

scp shutdown.tar.gz clemens@192.168.0.110:/tmp


