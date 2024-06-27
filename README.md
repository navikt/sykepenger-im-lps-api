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

### Testing


Add these env variables to your IDE run setup:

```
MASKINPORTEN_SCOPES=aud-localhost;MASKINPORTEN_WELL_KNOWN_URL=http://localhost:33445/default/.well-known/openid-configuration
```

Or, alternatively, use maskinporten test directly: 
```
MASKINPORTEN_SCOPES=nav:inntektsmelding/lps.write;MASKINPORTEN_WELL_KNOWN_URL=https://test.maskinporten.no/.well-known/oauth-authorization-server
```
and start Application.main() from your IDE and go to

http://localhost:8080/swagger

Get a token from mock server (or use postman towards test.maskinporten.no):
```
ACCESSTOKEN=`curl -X POST -H "Content-Type: application/x-www-form-urlencoded" -d "client_id=testid&scope=aud-localhost&client_secret=testpwd&grant_type=client_credentials" "localhost:33445/default/token" | grep access_token | cut -d ":" -f2 | cut -d "\"" -f2`
```
Make a call:
```
curl -v -H "Authorization: Bearer $ACCESSTOKEN" http://localhost:8080/forespoersler
```

You can also visit the dev environment:

https://sykepenger-im-lps-api.dev-gcp.nav.cloud.nais.io/swagger




