# build frontend
(cd pictl-vue && npx browserslist@latest --update-db && ./node_modules/.bin/vue-cli-service build --dest ../pictl-ktor/resources/pictl-vue/)
# bundle backend (includes frontend)
(cd pictl-ktor && ./mvnw clean package)
# deploy to server
(cd pictl-ktor && scp target/pictl-ktor-1.0.0-jar-with-dependencies.jar pi@white.unividuell.org:pictl.jar)
# restart pictl
ssh pi@white.unividuell.org 'sudo systemctl restart pictl'
# done
echo 'deployed :)'