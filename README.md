# pictl - control your audio multiroom

With `pictl` you are able to control the audio output in multiple rooms.

## UI

The [user interface (`pictl-vue`)](pictl-vue) provides:

- current song information (title, artist, album, artwork, name of stream)
- play/pause control
- volume of each player
- player of each room (synchronized and/or single instance)
- power off the cluster / single instance

## PI Setup Instructions

Step-by-step instructions to set up the player, running on Raspberry PIs. For details
see [pi-setup-client](pi-setup-client) and [pi-setup-server](pi-setup-server). The project is powered
by [Logitech Media Server](https://github.com/Logitech/slimserver)
and [Squeezelite](https://github.com/ralph-irving/squeezelite).

## API

The [subproject `pictl-ktor`](pictl-ktor) abstracts the LMS and provides an API for `pict-vue`. It runs on each
Raspberry PI in each room (with different activate modules per room).

## Monitoring

By [starting `pict-monitor`](pictl-monitor) you can inspect some useful insights about running `pict-ktor` APIs.
