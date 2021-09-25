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
[ "$#" -eq 1 ] || die "1 argument required, $# provided. Set the hostname of the target (like white.unividuell.org)"


target="${1}"

echo "build frontend"
(cd pictl-vue && npx browserslist@latest --update-db && ./node_modules/.bin/vue-cli-service build --dest ../pictl-ktor/resources/pictl-vue/)
echo "bundle backend (with bundled frontend)"
(cd pictl-ktor && ./mvnw clean package)
echo "deploy to $target"
(cd pictl-ktor && scp target/pictl-ktor-1.0.0-jar-with-dependencies.jar pi@"${target}":pictl.jar)
echo "restart pictl on $target"
ssh pi@"${target}" 'sudo systemctl restart pictl'
echo 'deployed :)'