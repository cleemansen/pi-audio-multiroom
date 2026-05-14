# Snapclient Setup

Anleitung, um `snapclient` auf einem neuen Raspbian-System so aufzusetzen, dass
es den ALSA-Upmix (Stereo → 5.1/6 Kanäle) aus
[`../surround/current/etc/asound.conf`](../surround/current/etc/asound.conf)
tatsächlich benutzt.

## Voraussetzung

`/etc/asound.conf` ist deployed (siehe `../surround/README.md`) und der
Default-Pfad funktioniert:

```bash
speaker-test -c 2 -D default -twav
# während des Tests in einem zweiten Terminal:
cat /proc/asound/card0/pcm0p/sub0/hw_params
# Erwartung: channels: 6
```

(Karten-Nummer ggf. anpassen — siehe `aplay -l`.)

## Installation

1. `snapclient` installieren:
   ```bash
   sudo apt-get install snapclient
   ```
2. `/etc/default/snapclient` editieren und die Option auf das Upmix-PCM
   setzen:
   ```bash
   sudo nano /etc/default/snapclient
   ```
   Zeile:
   ```
   SNAPCLIENT_OPTS="-s plug:upmix"
   ```
3. Service neu starten:
   ```bash
   sudo systemctl restart snapclient
   ```

## Warum `-s plug:upmix`?

Snapclients eingebauter Default `-s default` öffnet die Karte empirisch **nicht**
über die Plug/Route-Kette aus der `asound.conf` — am Hardware-Device kommen dann
nur 2 Kanäle an, der Upmix wird umgangen.

`plug:upmix` zwingt snapclient deterministisch auf das `pcm.upmix` (Route 2 → 6)
aus der `asound.conf`. Der `plug:`-Wrapper davor ist die Sicherheits-Stufe für
Rate/Format-Anpassung — bei unserer Konfig (dmixer fix auf 48 kHz, snapclient
nativ 48 kHz) ist Resampling im Normalfall ein No-Op, aber `plug:` schadet nicht
und schützt vor Rate-Mismatch, falls sich der Server-Stream mal ändert.

## Verifikation / Debugging

Während snapclient gerade etwas abspielt, in einem Terminal:

```bash
cat /proc/asound/card0/pcm0p/sub0/hw_params
```

Erwartete Ausgabe:

```
access: MMAP_INTERLEAVED
format: S16_LE
subformat: STD
channels: 6
rate: 48000 (48000/1)
period_size: 2048
buffer_size: 8192
```

Entscheidend sind zwei Werte:

- **`channels: 6`** — beweist, dass der Stream durch den Upmix-Route-Block läuft.
  Steht da `channels: 2`, geht snapclient an `asound.conf` vorbei (z. B. weil
  `SNAPCLIENT_OPTS` doch wieder `-s default` oder `-s hw:…` enthält).
- **`rate: 48000`** — bestätigt, dass dmixer wie in der `asound.conf` festgelegt
  auf 48 kHz läuft (matched snapclients native Rate, kein Resampling auf dem Pi).

Weitere nützliche Checks:

```bash
# Welches PCM-Device hat snapclient gerade offen?
sudo ls -la /proc/$(pgrep -x snapclient)/fd/ | grep -E 'snd|pcm'

# Verfügbare PCMs auflisten:
aplay -L

# Karten-Index ermitteln (für hw_params-Pfad oben):
aplay -l
```
