# ALSA surround51 upmix

[Stereo signal -> upmixed to 6 channels](https://lichtmetzger.de/en/2014/04/22/better-stereo-to-5-1-upmix-on-linux-alsa-asoundrc/)

**Adjustments:** My USB sound card seems special. Some channels are not numbered as excpected.

```
# These are the custom routings I specified.
# Every speaker can be routed to another speaker,
# signals can be mixed etc.
pcm.upmix {
     type route
     slave.pcm dmixer
     slave.channels 6
     #####  EINGANGSKANAL.AUSGANGSKANAL(0: links, 1: rechts) DÄMPFUNG
     ttable.0.0 1       # front links  -> 0: front links
     ttable.1.1 1       # front rechts -> 1: front rechts
     ttable.0.4 1       # front links  -> 4: rear links
     ttable.1.5 1       # front rechts -> 5: rear rechts
     ttable.0.2 0.5     # front links  -> 2: center * 0.5 (6dB Dämpfung ((0.707: 3dB))
     ttable.1.2 0.5     # front rechts -> 2: center * 0.5
     ttable.0.3 0.5     # front links  -> 3: woofer * 0.5
     ttable.1.3 0.5     # front rechts -> 3: woofer * 0.5
}
```