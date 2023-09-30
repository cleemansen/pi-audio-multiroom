#!/usr/bin/env bash
# Deploy the whole app to the target environment
#
# Best practices: https://kvz.io/bash-best-practices.html
# exit when a command fails.
set -o errexit
# exit when your script tries to use undeclared variables
set -o nounset
# exit when no argument is given
die () {
    echo >&2 "$@"
    exit 1
}
# exit when not exactly 1 argument is given
[ "$#" -eq 1 ] || die "1 argument required, $# provided. Set the hostname of the target (like 192.168.0.110)"


target="${1}"

echo "build frontend"
frontend_dest="pictl-ktor/resources/candle/"
rm -rf $frontend_dest && mkdir $frontend_dest
(cd ../candle && pnpm run build && cp -rf dist/* ../x-archive/$frontend_dest/)
echo "bundle backend (with bundled frontend)"
(cd pictl-ktor && ./mvnw clean package --quiet)
echo "deploy to $target"
(cd pictl-ktor && scp target/pictl-ktor-2.0.0-jar-with-dependencies.jar pi@"${target}":pictl.jar)
echo "restart pictl on $target"
ssh pi@"${target}" 'sudo systemctl restart pictl'
echo 'deployed :)'