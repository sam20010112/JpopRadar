# JpopRadar

A web app for browsing and tracking J-pop concerts. Browse upcoming events, filter by artist or date, and explore concert details including ticket info and venues — all through an interactive calendar interface.

## Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Backend | Spring Boot | 3.2.3 |
| Language | Java | 21 |
| Database (dev) | H2 In-Memory | 2.x |
| Frontend | Vue 3 + Vite | 3.4 / 5.2 |
| State | Pinia | 2.1 |
| HTTP Client | Axios | 1.6 |

## Prerequisites

- Java 21+ (JDK)
- Maven 3.x
- Node.js 18+, npm 9+

## Quick Start

### Backend

```bash
cd backend

# Compile
JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-25.0.1.8-hotspot" mvn compile -q

# Resolve classpath
JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-25.0.1.8-hotspot" \
  mvn dependency:build-classpath -Dmdep.outputFile=target/classpath.txt -q

# Run
CP=$(cat target/classpath.txt)
"C:/Program Files/Eclipse Adoptium/jdk-25.0.1.8-hotspot/bin/java" \
  -cp "target/classes;$CP" com.jpopradar.JpopRadarApplication
```

Backend runs at `http://localhost:8080`. H2 console at `http://localhost:8080/h2-console`.

> **Note:** `./mvnw spring-boot:run` has a known classloader issue on this setup — see [SPEC_EN.md](SPEC_EN.md#12-known-issues--workarounds).

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at `http://localhost:5173`.

## REST API

Base path: `/api/concerts`

| Method | Path | Description |
|---|---|---|
| GET | `/api/concerts` | List all concerts |
| GET | `/api/concerts/{id}` | Get concert by ID |
| POST | `/api/concerts` | Create concert |
| PUT | `/api/concerts/{id}` | Update concert |
| DELETE | `/api/concerts/{id}` | Delete concert |
| GET | `/api/concerts/upcoming` | Upcoming concerts (sorted ASC) |
| GET | `/api/concerts/by-city?city=` | Filter by city |
| GET | `/api/concerts/by-artist?artist=` | Filter by artist |
| GET | `/api/concerts/scan` | Rich JSON concert data (main data source) |

## Documentation

See [SPEC_EN.md](SPEC_EN.md) for full technical specification including architecture, data model, configuration, and known issues.
