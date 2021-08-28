#!/usr/bin/env bash
# Deploy the whole app to the target environment
#
# Best practices: https://kvz.io/bash-best-practices.html
# exit when a command fails.
set -o errexit
# exit when your script tries to use undeclared variables
set -o nounset

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