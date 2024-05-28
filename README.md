# Hello NAIS

This is a bare bone repository containing a simple web server.
The repository is used by the hello nais tutorial in the nais documentation.

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
