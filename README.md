Sykepenger IM LPS API
================

Dette prosjektet er et API tiltenkt Lønns- og personalsystemer som skal hente data (forespurte og innsendte inntektsmeldinger) på vegne av en bedrift. 

# Komme i gang

For lokal utvikling, start lokal mock oauth-server, kafka and postgresql:
```
    cd docker/local
    docker-compose up -d --remove-orphans
    
```
Mock-oauth-server vil bli tilgjengelig her: http://localhost:33445/.well-known/openid-configuration

Start Application.main() fra ditt IDE og gå til:

http://localhost:8080/swagger

Hent test-token fra mockserver:
```
ACCESSTOKEN=`curl -X POST -H "Content-Type: application/x-www-form-urlencoded" -d "client_id=testid&scope=nav:inntektsmelding/lps.write&client_secret=testpwd&grant_type=client_credentials" "localhost:33445/maskinporten/token" | grep access_token | cut -d ":" -f2 | cut -d "\"" -f2`
```
Utfør et kall:
```
curl -v -H "Authorization: Bearer $ACCESSTOKEN" http://localhost:8080/forespoersler
```

Du kan også bruke test.maskinporten.no - endre i så fall 
maskinporten.wellknownUrl = "https://test.maskinporten.no/.well-known/oauth-authorization-server" i application.conf

`application.conf`  lar deg også toggle av og på funksjoner, samt endre parametere. Disse vil bli overskrevet i dev / prod med respektive NAIS-miljøvariabler, der disse finnes.


For å sette opp postman til å hente token fra maskinporten kan du følge denne guiden: 

https://github.com/navikt/nav-ekstern-api-dok/blob/main/api-dok/teste-delegerbart-api/teste-delegerbart-api.md


Dev-miljøet er tilgjengelig på Internett:

https://sykepenger-im-lps-api.ekstern.dev.nav.no/swagger


### Tests:
Testene benytter h2-database og et separat flyway migrate-script i `src/test/resources/db/migration`
Endringer / tillegg i standard-scriptene må legges til i denne fila. 
Konfigurasjonsfila `src/test/resources/application.conf` lar deg overstyre parametere ved behov.

---

## Bygging av docker-image:

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

# OpenApi dokumentasjon

Prosjektet bruker Ktor OpenAPI-plugin i IntelliJ for å generere OpenAPI-dokumentasjon. 
Siden den genererte filen trenger oppdatering, er det lagt til en Gradle-task som modifiserer denne filen. Kjør
```shell
./gradlew modifyOpenApi
``` 
for å modifisere OpenAPI-dokumentasjonen.

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #helse-arbeidsgiver.


