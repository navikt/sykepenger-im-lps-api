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

Alternatively, just start Application.main() from your IDE and go to

http://localhost:8080/swagger

You can also visit the dev environment:

https://sykepenger-im-lps-api.dev-gcp.nav.cloud.nais.io/swagger

### Authentication

For local dev, start local moch oauth-server:
```
    cd docker/local
    docker-compose up -d --remove-orphans
    
```
It will be available on: http://localhost:33445/.well-known/openid-configuration


