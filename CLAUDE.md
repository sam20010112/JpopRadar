# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JpopRadar is a Japanese pop music tracking app with a **Spring Boot REST API** backend and a **Vue 3 SPA** frontend.

## Commands

### Backend (`/backend`)
```bash
# Run the dev server (port 8080)
./mvnw spring-boot:run

# Run tests (no tests exist yet)
./mvnw test

# Run a single test class
./mvnw test -Dtest=SongServiceTest

# Build JAR
./mvnw package
```

### Frontend (`/frontend`)
```bash
# Install dependencies
npm install

# Run dev server (port 5173, proxies /api to :8080)
npm run dev

# Build for production
npm run build
```

## Architecture

### Backend — Spring Boot (Java 21, Maven)

Standard Spring layered architecture:

```
controller/   — @RestController classes, maps HTTP routes, delegates to services
service/      — Business logic
repository/   — Spring Data JPA interfaces (extends JpaRepository)
model/        — JPA @Entity classes
```

- All controllers are under `/api/*` and annotated with `@CrossOrigin(origins = "http://localhost:5173")` for local dev.
- Database: H2 in-memory (`spring.jpa.hibernate.ddl-auto=create-drop` — schema is recreated on every restart). Seed data is in `src/main/resources/data.sql` and re-runs each start.
- H2 console available at `http://localhost:8080/h2-console` in dev (JDBC URL: `jdbc:h2:mem:jpopradar`, user: `sa`, no password).
- To swap to MySQL/Postgres: update `spring.datasource.*` and `spring.jpa.database-platform` in `application.properties`.

#### REST API (`/api/songs`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/songs` | List all songs |
| GET | `/api/songs/{id}` | Get song by ID |
| POST | `/api/songs` | Create song |
| PUT | `/api/songs/{id}` | Update song |
| DELETE | `/api/songs/{id}` | Delete song |
| GET | `/api/songs/by-artist?artist=` | Filter by artist (case-insensitive) |
| GET | `/api/songs/by-genre?genre=` | Filter by genre (case-insensitive) |

#### Song model fields
`id`, `title`, `artist`, `album`, `releaseYear`, `genre`

### Frontend — Vue 3 + Vite

```
src/
  services/api.js     — Axios instance + all API calls (single source of truth for HTTP)
  stores/songStore.js — Pinia store: songs[], currentSong, loading, error state
  views/              — Page-level components, mounted by the router
  components/         — Reusable UI components, receive props/emit events
  router/index.js     — Vue Router route definitions
```

Data flow: **View → Store → Service (api.js) → Backend API**

Vite proxies `/api` requests to `http://localhost:8080` in dev, so no CORS config is needed on the frontend side.
