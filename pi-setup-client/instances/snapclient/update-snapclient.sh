#!/bin/bash

set -x # print out every line as it executes
set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

# armhf: raspberry pi 1 (B rev 2) / zero w (1)
# https://github.com/badaix/snapcast/releases/download/v0.32.3/snapclient_0.32.3-1_armhf_trixie.deb
SC_VERSION=0.35.0
wget -O /tmp/snapclient.deb https://github.com/badaix/snapcast/releases/download/v$SC_VERSION/snapclient_$SC_VERSION-1_armhf_trixie.deb

sudo apt install /tmp/snapclient.deb

