# candle

## Build Docker

```
docker buildx \
    build \
    --push \
    --platform linux/arm64,linux/amd64 \
    --tag ghcr.io/unividuell/candle \
    .
```

## CORS

See `./Caddyfile`.