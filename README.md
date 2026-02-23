# Omada - OeBS API

REST API for Omada integrasjon med Oracle E-Business Suite (OeBS).

## Oversikt

Dette prosjektet implementerer SCIM 2.0 API for synkronisering av brukere og grupper mellom OeBS og Omada Identity Management.

## Arkitektur

### Data Flow

```
Omada Identity Management
    ↓ SCIM 2.0 REST API
Apache SCIMple (SCIM Library)
    ↓
Spring Boot Application
    ├── Service Layer (Business Logic)
    ├── Mapper Layer (Entity → SCIM)
    └── Repository Layer (Spring Data JPA)
            ↓ SELECT
Database Views (Oracle)
    ├── V_OMADA_ACTIVE_USERS
    ├── V_OMADA_USER_ALL_GROUPS
    ├── V_OMADA_ACTIVE_GROUPS
    └── V_OMADA_ACTIVE_RESPONSIBILITIES
            ↓ JOIN
E-Business Suite Tables
    ├── FND_USER
    ├── PER_ALL_PEOPLE_F
    ├── JTF_RS_GROUPS_VL
    └── FND_RESPONSIBILITY
```

### Hvorfor SELECT fra Views (ikke PL/SQL)?

**Anbefaling: Bruk SELECT fra database views med Apache SCIMple**

Vi bruker **Apache SCIMple** biblioteket som forventer Java objekter og håndterer SCIM JSON-serialisering automatisk. Derfor er det bedre å:

✅ Bruke Spring Data JPA repositories til å SELECT fra views  
✅ Mappe entities til SCIM objekter i Java  
✅ La Apache SCIMple håndtere JSON og SCIM protokoll  

Se [PLSQL-VS-JPA.md](PLSQL-VS-JPA.md) for detaljert sammenligning.

## Prosjektstruktur

```
oebs-omada-api/
├── database/
│   ├── views/                    # Database views for SCIM data
│   │   ├── v_omada_active_users.sql
│   │   ├── v_omada_active_groups.sql
│   │   ├── v_omada_user_all_groups.sql
│   │   └── deploy_all.sql
│   └── packages/                 # PL/SQL packages (referanse)
│       ├── xxrtv_omada_scim_pkg.pks
│       └── xxrtv_omada_scim_pkg.pkb
├── src/main/java/
│   └── no/nav/oebs/api/
│       └── scim/
│           ├── ScimUserEntity.java
│           ├── ScimGroupEntity.java
│           ├── repository/       # Spring Data JPA
│           │   ├── ScimUserRepository.java
│           │   └── ScimGroupRepository.java
│           ├── service/          # Business logic
│           │   ├── ScimUserService.java
│           │   └── ScimGroupService.java
│           └── mapper/           # Entity → SCIM
│               ├── ScimUserMapper.java
│               └── ScimGroupMapper.java
├── scim-examples.jsonc           # SCIM eksempler
├── SCIM-MAPPING.md               # Field mapping dokumentasjon
└── PLSQL-VS-JPA.md               # Arkitektur beslutning
```

## SCIM Mapping

### User Object

| SCIM 2.0 Field | OeBS Source | View Column |
|----------------|-------------|-------------|
| `id` | FND_USER.USER_NAME | bruker_id |
| `externalId` | PER_ALL_PEOPLE_F.PERSON_ID | nav_id |
| `userName` | FND_USER.USER_NAME | bruker_id |
| `name.givenName` | PER_ALL_PEOPLE_F.FIRST_NAME | for_navn |
| `name.familyName` | PER_ALL_PEOPLE_F.LAST_NAME | etter_navn |
| `emails[0].value` | FND_USER.EMAIL_ADDRESS | e_post |
| `active` | FND_USER.END_DATE | active_flag |
| `enterprise.department` | ? | enhets_id |
| `enterprise.division` | ? | arbeidsted_fylke |
| `groups[]` | Grupper + Ansvarsområder | scim_group_id |

### Group Object

**Grupper (G$):**
- ID format: `G$<department>@<name>` eller `G$<name>`
- DisplayName: `<department> - <name>` eller `<name>`
- Source: JTF_RS_GROUPS_VL

**Ansvarsområder (A$):**
- ID format: `A$<responsibility_key>@<name>`
- DisplayName: `*<name>`
- Source: FND_RESPONSIBILITY

## API Endpoints

### SCIM 2.0 Standard

```http
# Users
GET    /scim/v2/Users              # List alle brukere (paginert)
GET    /scim/v2/Users/{id}         # Hent en bruker
POST   /scim/v2/Users              # Opprett bruker
PUT    /scim/v2/Users/{id}         # Oppdater bruker (full)
PATCH  /scim/v2/Users/{id}         # Oppdater bruker (partial)
DELETE /scim/v2/Users/{id}         # Deaktiver bruker

# Groups
GET    /scim/v2/Groups             # List alle grupper (paginert)
GET    /scim/v2/Groups/{id}        # Hent en gruppe
POST   /scim/v2/Groups             # Opprett gruppe
PUT    /scim/v2/Groups/{id}        # Oppdater gruppe (full)
PATCH  /scim/v2/Groups/{id}        # Oppdater gruppe (partial)
DELETE /scim/v2/Groups/{id}        # Slett gruppe

# Service Provider Config
GET    /scim/v2/ServiceProviderConfig
GET    /scim/v2/Schemas
GET    /scim/v2/ResourceTypes
```

## Oppsett

### 1. Deploy Database Views

```bash
cd database/views
sqlplus username/password@database @deploy_all.sql
```

### 2. Konfigurer Application

```yaml
# src/main/resources/application.yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@//hostname:port/service
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    
scim:
  base-url: https://example.com/scim/v2
```

### 3. Kjør Applikasjon

```bash
mvn spring-boot:run
```

## Dependencies

```xml
<!-- Apache SCIMple -->
<dependency>
    <groupId>org.apache.directory.scim</groupId>
    <artifactId>scim-server</artifactId>
    <version>2.0.x</version>
</dependency>

<!-- Spring Boot -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Oracle JDBC -->
<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc8</artifactId>
</dependency>
```

## Testing

```bash
# Hent bruker
curl -X GET http://localhost:8080/scim/v2/Users/MSF4711 \
  -H "Accept: application/scim+json"

# List brukere (paginert)
curl -X GET "http://localhost:8080/scim/v2/Users?startIndex=1&count=10" \
  -H "Accept: application/scim+json"

# Hent gruppe
curl -X GET "http://localhost:8080/scim/v2/Groups/A$11@KUNDEMOTTAK" \
  -H "Accept: application/scim+json"
```

## TODO

- [ ] Implementer Resource Providers med Apache SCIMple
- [ ] Map enhetsId og arbeidsstedFylke til faktiske kolonner
- [ ] Implementer NAV custom extension (fullmakt)
- [ ] Legg til authentication/authorization
- [ ] Implementer write operasjoner (POST, PUT, PATCH, DELETE)
- [ ] Legg til caching
- [ ] Metrics og monitoring
- [ ] Integration tests med testcontainers

## Dokumentasjon

- [SCIM Mapping](SCIM-MAPPING.md) - Detaljert field mapping
- [SCIM Examples](SCIM-EXAMPLES-README.md) - JSON eksempler
- [PL/SQL vs JPA](PLSQL-VS-JPA.md) - Arkitektur beslutning
- [Database Views](database/views/README.md) - View dokumentasjon
- [SCIM Package README](src/main/java/no/nav/oebs/api/scim/README.md) - Java kode oversikt

## Lisens

Se [LICENSE.md](LICENSE.md)

