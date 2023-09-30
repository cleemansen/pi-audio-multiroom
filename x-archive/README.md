# Archive

In 2022 I did a rewrite of the project. This folder holds a archive.

Things that were replaced:

1. the name `pictl` was retired
1. no need for a JVM anymore (on some devices the `pict-ktor` is still active and handles things like shutdown)
1. the UI speaks directly to the cometd-endpoint of LMS instead via own pictl-backend (`pictl-ktor`)

## UI

The [subproject `pictl-vue`](pictl-vue) is the HMI. It talks to the following API.

## API

The [subproject `pictl-ktor`](pictl-ktor) abstracts the LMS and provides an API for `pictl-vue`. It runs on each Raspberry PI in each room (with different activate modules per room).

`candle` does not need this API anymore as it communicates directly w/ `LMS` via `cometd`.

## Monitoring

By [starting `pictl-monitor`](pictl-monitor) you can inspect some useful insights about running `pictl-ktor` APIs.