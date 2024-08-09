# Sykepenger IM LPS API

## Local Development

### Prerequisites

- [ ] Docker CLI - via one of these alternatives:
   - [Colima](https://github.com/abiosoft/colima) - Colima command-line tool (recommended)
   - [Rancher](https://rancherdesktop.io) - Rancher desktop
   - [Podman](https://podman-desktop.io) - Podman desktop
   - [Docker desktop](https://www.docker.com/products/docker-desktop/) - Docker desktop (requires license)

### Build

1. Build Docker image:

    ```shell
    docker build . -t hello-nais
    ```

2. Run Docker image:

    ```shell
    docker run -p 8080:8080 hello-nais
    ```

### Authentication

For local dev, start local mock oauth-server:
```
    cd docker/local
    docker-compose up -d --remove-orphans
    
```
It will be available on: http://localhost:33445/.well-known/openid-configuration

### Testing locally


Add these env variables to your IDE run setup:

```
MASKINPORTEN_SCOPES==nav:inntektsmelding/lps.write;MASKINPORTEN_WELL_KNOWN_URL=http://localhost:33445/maskinporten/.well-known/openid-configuration
```

Or, alternatively, use maskinporten test directly: (no need for local mock oauth-server in this case)
```
MASKINPORTEN_SCOPES=nav:inntektsmelding/lps.write;MASKINPORTEN_WELL_KNOWN_URL=https://test.maskinporten.no/.well-known/oauth-authorization-server
```
and start Application.main() from your IDE and go to

http://localhost:8080/swagger

Get a token from mock server (or use postman towards test.maskinporten.no):
```
ACCESSTOKEN=`curl -X POST -H "Content-Type: application/x-www-form-urlencoded" -d "client_id=testid&scope=nav:inntektsmelding/lps.write&client_secret=testpwd&grant_type=client_credentials" "localhost:33445/maskinporten/token" | grep access_token | cut -d ":" -f2 | cut -d "\"" -f2`
```

Or use postman and get a token from test.maskinporten.no - to set up postman, follow this guide:
https://github.com/navikt/nav-ekstern-api-dok/blob/main/api-dok/teste-delegerbart-api/teste-delegerbart-api.md

Make a call:
```
curl -v -H "Authorization: Bearer $ACCESSTOKEN" http://localhost:8080/forespoersler
```

You can also visit the dev environment:

https://sykepenger-im-lps-api.ekstern.dev.nav.no/swagger


