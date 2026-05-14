#!/bin/zsh

set -x # print out every line as it executes
set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

tar -czvf squeezelite-v2.tar.gz update-squeezelite.sh squeezelite.service

scp squeezelite-v2.tar.gz install-squeezelite.sh clemens@192.168.0.110:/tmp
