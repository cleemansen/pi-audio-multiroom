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

```
(cors) {
  # kudos: https://kalnytskyi.com/posts/setup-cors-caddy-2/
  @cors_preflight method OPTIONS
  @cors header Origin {args.0}

  handle @cors_preflight {
    header Access-Control-Allow-Origin "{args.0}"
    header Access-Control-Allow-Methods "GET, POST, PUT, PATCH, DELETE"
    header Access-Control-Allow-Headers "Content-Type"
    header Access-Control-Max-Age "3600"
    header Access-Control-Allow-Credentials "true"
    respond "" 204
  }

  handle @cors {
    header Access-Control-Allow-Origin "{args.0}"
    header Access-Control-Expose-Headers "Link"
    header Access-Control-Allow-Credentials "true"
  }
}


:9001 {
  import cors http://thin.unividuell.org:9002
  reverse_proxy lms:9000
  log {
    output file /var/log/caddy/9001-access.log
    format console
  }
}

:9011 {
  import cors http://localhost:3000
  reverse_proxy lms:9000
  log {
    output file /var/log/caddy/9011-access.log
    format console
  }
}
```