# JpopRadar

A web app for tracking J-pop concerts — browse upcoming events, filter by artist or date, and view full ticket/venue details via an interactive calendar.

## Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.2 / Java 21 / Maven |
| Frontend | Vue 3 / Vite / Pinia / Vue Router / Axios |
| Database | H2 in-memory (dev) |

## Windows Start Guide (One Command)

A `start.bat` script is included at the project root. It compiles and launches both the backend and frontend in one step.

**Prerequisites:**
- Java (Temurin 25) installed at `C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot`
- Maven available on `PATH`
- Node.js / npm available on `PATH`
- Frontend dependencies installed (`npm install` inside `frontend/` — first time only)

**Run:**

```bat
start.bat
```

Or run the PowerShell script directly:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File start.ps1
```

Both processes launch in the background. Output is written to log files:

| Service | Log |
|---|---|
| Backend stdout | `backend/logs/backend.log` |
| Backend stderr | `backend/logs/backend-err.log` |
| Frontend stdout | `frontend/logs/frontend.log` |
| Frontend stderr | `frontend/logs/frontend-err.log` |

Once running:
- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080/api/concerts`
- H2 Console: `http://localhost:8080/h2-console`

To stop both services, kill the PIDs stored in `.pids` (created automatically by the script):

```powershell
Get-Content .pids | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }
```

---

## Quick Start (Manual)

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

> Runs at `http://localhost:8080`
> H2 console: `http://localhost:8080/h2-console` (URL: `jdbc:h2:mem:jpopradar`, user: `sa`, no password)

### Frontend

```bash
cd frontend
npm install
npm run dev
```

> Runs at `http://localhost:5173`

## REST API

Base URL: `/api/concerts`

| Method | Path | Description |
|---|---|---|
| GET | `/` | All concerts |
| GET | `/{id}` | Concert by ID |
| POST | `/` | Create concert |
| PUT | `/{id}` | Update concert |
| DELETE | `/{id}` | Delete concert |
| GET | `/upcoming` | Future concerts sorted by date |
| GET | `/by-city?city=` | Filter by city |
| GET | `/by-artist?artist=` | Filter by artist |
| GET | `/scan` | Rich concert data from JSON file |

## Architecture

```
Backend:  Controller → Service → Repository → JPA Entity → H2
Frontend: View → Pinia Store → api.js (Axios) → /api proxy → Backend
```

Concert data for the main browsing experience is served from `concertsScan.json` via `/api/concerts/scan`. The database-backed endpoints support CRUD operations on a simplified `Concert` entity.

## Project Structure

```
JpopRadar/
├── backend/
│   └── src/main/
│       ├── java/com/jpopradar/
│       │   ├── controller/ConcertController.java
│       │   ├── service/ConcertService.java
│       │   ├── repository/ConcertRepository.java
│       │   └── model/Concert.java
│       └── resources/
│           ├── application.properties
│           ├── data.sql
│           └── concertsScan.json
└── frontend/
    └── src/
        ├── services/api.js
        ├── stores/concertStore.js
        ├── router/index.js
        ├── views/
        │   ├── HomeView.vue
        │   ├── ConcertsView.vue
        │   └── ConcertDetailView.vue
        └── components/
            ├── ConcertCard.vue
            └── ConcertCalendar.vue
```

## Known Issues

- **`./mvnw spring-boot:run` fails** — classloader incompatibility between system Maven 3.9.12 (Chocolatey) and `spring-boot-maven-plugin` 3.2.3. Use the manual 3-step launch above.
- **Java version** — Must set `JAVA_HOME` to Temurin 25. Maven defaults to Amazon Corretto 8 internally, which causes "wrong class file version" errors.
