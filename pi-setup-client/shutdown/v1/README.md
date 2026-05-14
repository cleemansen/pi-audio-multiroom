# shutdown by pictl-ktor

Goal: shutdown the system (aka the pi) via REST API

That works pretty nice. Have a look at the route `http://192.168.0.110:8080/ctl-hardware/shutdown/me?delay=PT18S`.
The downside was to setup a JVM on the RPI.

---

## pictl-ktor

`pictl-ktor` provides shutdown endpoint (with abortion support) [2023-09-30].

### Process

1. NFC tag _Lola_ gets scanned by Pixel 7
1. Home Assistant triggers action `rest_command.shutdown_orange` (`RESTful` command):
```
rest_command:
    shutdown_orange:
        url: "http://192.168.0.110:8080/ctl-hardware/shutdown/me?delay=PT18S"
        method: POST
```
1. `pictl-ktor` handles this http request and executes the following steps:
    1. `if (remaining.seconds > 10 && remaining.seconds.mod(2) == 0)`: `beep(frequency = 800)`
    1. `if (remaining.seconds <= 10)`: `beep(frequency = 1250)`
    1. `if (remaining <= 0)`: `sudo shutdown -P now`
    1. (if another http request happens to the same endpoint and remaining is > 0: cancellation)
    1. `beep`: `"timeout", "0.2s", "speaker-test", "--test", "sine", "--frequency", "$frequency", "--nloops", "1", "--scale", "140", "--channels", "1"`

## Shutdown via squeezelite (deprecated!)

1. add `-S` option to squeezelite cmd (via `/usr/local/bin/squeezelite_settings.sh`)
    ```
    # If you want to use different squeezelite options, not set by this script, use the next line:
    SL_ADDITIONAL_OPTIONS="-S /usr/local/bin/squeezelite-power.sh"
    ```
1. create file `/usr/local/bin/squeezelite-power.sh`
1. create file ``