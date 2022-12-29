# audio multiroom

With ~~`pictl`~~ `candle` you are able to control the audio output in multiple rooms.

## UI

The [user interface (`candle`)](candle) provides:

- current song information (title, artist, album, artwork, name of stream)
- fast title lookup action at spotify
- play/pause control
- volume of each player
- player of each room (synchronized and/or single instance)
- TODO: power off the cluster / single instance

## Thin Server Setup Instructions

Use project [open-smart-home](https://github.com/cleemansen/open-smart-home). 

Part of this project is 
1. a _logitech media server_ instance
1. a _squeezelite_ client
1. _candle_ instance from this project

## PI Setup Instructions

Step-by-step instructions to set up the player, running on Raspberry PIs. For details
see [pi-setup-client](pi-setup-client) and [pi-setup-server](pi-setup-server). The project is powered
by [Logitech Media Server](https://github.com/Logitech/slimserver)
and [Squeezelite](https://github.com/ralph-irving/squeezelite).
