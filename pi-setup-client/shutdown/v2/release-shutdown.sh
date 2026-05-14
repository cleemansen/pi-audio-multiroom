#!/bin/zsh

set -x # print out every line as it executes
set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

pip freeze > requirements.txt
tar -czvf shutdown.tar.gz app.py requirements.txt shutdown.service

scp shutdown.tar.gz install-shutdown.sh clemens@192.168.0.110:/tmp
