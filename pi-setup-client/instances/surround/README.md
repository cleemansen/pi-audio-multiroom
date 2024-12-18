# ALSA

## Testing / debugging

1. Plug in the 7.1 USB soundcard.
2. Move `pi-setup-client/instances/surround/current/etc/asound.conf` to `/etc/asound.conf`
2. list soundcards `aplay -l` -> `card 2: ICUSBAUDIO7D [ICUSBAUDIO7D], device 0: USB Audio [USB Audio]`
3. list devices (PCMs): `aplay -L`
3. check your speaker/channels directly via soundcard: `speaker-test -c 6 -D surround51:CARD=ICUSBAUDIO7D -t wav`
4. check your upmix (stereo to 5.1) via PCM: `speaker-test -c 2 -D duplex -t wav`
5. verify your default device is the same: `speaker-test -c 2 -D default -t wav`
