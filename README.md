# Omada – OeBS API

REST API for Omada Identity Management sin integrasjon med Oracle E-Business Suite (OeBS).  
Implementerer SCIM 2.0-grensesnitt for synkronisering av brukere og grupper.

---

## Innhold

- [Teknologi](#teknologi)
- [Lokalt oppsett](#lokalt-oppsett)
- [Kjøring](#kjøring)
- [SCIM 2.0 endepunkter](#scim-20-endepunkter)
- [Testdata](#testdata)
- [Branch-strategi](#branch-strategi)
- [CI/CD og deploy](#cicd-og-deploy)
- [Miljøer](#miljøer)

---

## Teknologi

| Hva | Versjon |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.6 |
| Apache SCIMple | 1.0.0-M1 |
| Oracle JDBC | 23.x |
| Jetty | 12.1.6 |

---

## Lokalt oppsett

### Forutsetninger

- **GSA** — påkrevd for tilgang til Oracle-databasen
- **Java 21**
- **Maven 3.9+**

### Database

Applikasjonen er tett koblet mot Oracle E-Business Suite og kan **ikke** kjøres med en generisk lokal database (f.eks. H2) fordi den er avhengig av:

- Oracle-spesifikke database views (`XXRTV_OMADA_AKTIVE_BRUKERE_V`, `XXRTV_OMADA_SCIM_GRPS_V` m.fl.)
- En PL/SQL-pakke (`XXRTV_INT_OMADA_INSERT_MESSAGE`) med OeBS-spesifikk logikk
- Oracle SQL-dialekt (HQL-spørringer kompilert mot Oracle)

**Alternativ 1 — Koble til OeBS-databasen direkte (anbefalt)**

Krever GSA. Sett `DB_URL` til ønsket miljø (u1, q1 eller prod) via `.env`-filen.
Credentials hentes fra Vault eller en kollega.

**Alternativ 2 — Mock tjenestelaget (kun UI/API-utvikling)**

Hvis du kun jobber med API-strukturen og ikke trenger ekte data, kan du
lage en Spring-profil `mock` som erstatter repository-implementasjonene
med statiske testdata. Dette er ikke implementert per nå, men kan
legges til ved behov.

### Miljøvariabler

Følgende variabler må settes før oppstart. Hent verdiene fra Vault eller en kollega:

| Variabel | Beskrivelse | Eksempel |
|---|---|---|
| `APPS_USER` | OeBS DB-brukernavn | `OMADA_API` |
| `APPS_PASSWORD` | OeBS DB-passord | *(fra Vault)* |
| `DB_URL` | JDBC-URL til Oracle | *(fra Vault)* |
| `PLSQL_ORG_ID` | Oracle operating unit ID | *(fra Vault)* |

Valgfrie variabler (har defaults lokalt):

| Variabel | Default | Beskrivelse |
|---|---|---|
| `PLSQL_INSERT_PROCEDURE` | `APPS.XXRTV_INT_OMADA_INSERT_MESSAGE.InsertOmadaMessage` | Fullt kvalifisert prosedyrenavn |

> **JWT-validering er deaktivert lokalt** — alle endepunkter er åpne uten token.

---

## Kjøring

### IntelliJ

1. Åpne **Run/Debug Configurations**
2. Velg `Application` (main-klassen `no.nav.oebs.api.Application`)
3. Sett **Active profiles**: `local`
4. Legg til **Environment variables**:
   ```
   APPS_USER=OMADA_API;APPS_PASSWORD=<passord>;DB_URL=<db-url>
   ```
5. Kjør

### Terminal (Maven)

Lag en `.env`-fil i prosjektets rotmappe — denne er i `.gitignore` og skal **aldri** committes:

```bash
cp .env.example .env
# Rediger .env og fyll inn verdiene
```

Kjør med `.env`-filen:

```bash
set -a && source .env && set +a
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Container (Docker)

Bygg image:

```bash
mvn clean package -DskipTests
docker build -t oebs-omada-api:local .
```

Kjør med `.env`-filen:

```bash
docker run --rm \
  --env-file .env \
  -e SPRING_PROFILES_ACTIVE=local \
  -e SCIM_BASE_URL=http://localhost:8080 \
  -p 8080:8080 \
  oebs-omada-api:local
```

> `.env`-filen er listet i `.gitignore` — DB-passord skal aldri ligge i kildekoden eller commites.

### Verifiser at applikasjonen er oppe

```bash
curl http://localhost:8080/internal/isalive
# → 200 OK

curl http://localhost:8080/scim/v2/Schemas
# → SCIM Schema-liste
```

---

## SCIM 2.0 endepunkter

Base URL lokalt: `http://localhost:8080/scim/v2`

| Metode | Endepunkt | Beskrivelse                             |
|---|---|-----------------------------------------|
| `GET` | `/Users` | List alle aktive brukere (paginert)     |
| `GET` | `/Users/{id}` | Hent bruker på navId (f.eks. `A123456`) |
| `POST` | `/Users` | Opprett bruker i OeBS                   |
| `PUT` | `/Users/{id}` | Oppdater bruker i OeBS                  |
| `DELETE` | `/Users/{id}` | Slett bruker i OeBS                     |
| `GET` | `/Groups` | List alle grupper (paginert)            |
| `GET` | `/Groups/{id}` | Hent gruppe med medlemmer               |
| `GET` | `/Schemas` | SCIM-skjemadefinisjon                   |
| `GET` | `/ResourceTypes` | SCIM resource types                     |
| `GET` | `/ServiceProviderConfig` | SCIM server capabilities                |

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### Content-Type

Alle SCIM-kall skal bruke:
```
Content-Type: application/scim+json
```

---

## Testdata

### Opprett bruker — `POST /scim/v2/Users`

```json
{
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User",
    "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
    "urn:ietf:params:scim:schemas:extension:nav:oebs:2.0:User"
  ],
  "id": "A123456",
  "externalId": "ABCD1234",
  "userName": "ABCD1234",
  "active": true,
  "name": {
    "givenName": "Kari",
    "familyName": "Nordmann",
    "formatted": "Kari Nordmann"
  },
  "displayName": "Kari Nordmann",
  "emails": [{ "value": "kari.nordmann@nav.no", "type": "work", "primary": true }],
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
    "department": "1234",
    "division": "52"
  },
  "urn:ietf:params:scim:schemas:extension:nav:oebs:2.0:User": {
    "fullmakt": "INNKJØP"
  }
}
```

### Endre bruker — `PUT /scim/v2/Users/A123456`

Samme struktur som POST, med oppdaterte feltverdier.

### Slett bruker — `DELETE /scim/v2/Users/S108633`

Ingen body — id hentes fra URL.

---

## Branch-strategi

Vi bruker én varig branch: `main`.

- Alt arbeid gjøres i korte feature-brancher
- Endringer merges til `main` via pull request
- Vi bruker ikke miljøspesifikke brancher

---

## CI/CD og deploy

Deploy styres fra `/.github/workflows/build-deploy.yaml`.

- `pull_request` mot `main`: bygger, kjører tester og kjører Sonar
- `push` til `main`: bygger, kjører Sonar og deployer til prod
- `workflow_dispatch`: manuell deploy til `u1`, `q1` eller `prod`

Når du kjører manuell deploy, velg ønsket miljø i `environment`-input.

---

## Miljøer

| Miljø | URL | NAIS secret |
|---|---|---|
| Lokal | `http://localhost:8080` | — |
| u1 (dev) | `https://oebs-omada-api-u1.intern.dev.nav.no` | `oebs-omada-u1` |
| q1 (dev) | `https://oebs-omada-api-q1.intern.dev.nav.no` | `oebs-omada-q1` |
| prod | `https://oebs-omada-api.intern.nav.no` | `oebs-omada` |

