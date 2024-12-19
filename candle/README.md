# candle

## Build Docker

```
docker buildx \
    build \
    --push \
    --platform linux/arm64,linux/amd64 \
    --tag ghcr.io/unividuell/candle:ha \
    .
```

Docker package registry [login/auth](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry#authenticating-with-a-personal-access-token-classic) 

## CORS

See `./Caddyfile`.