# JpopRadar — Technical Specification

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [Architecture](#3-architecture)
4. [Data Model](#4-data-model)
5. [Backend Specification](#5-backend-specification)
6. [Frontend Specification](#6-frontend-specification)
7. [REST API Contract](#7-rest-api-contract)
8. [Data Flow](#8-data-flow)
9. [Configuration](#9-configuration)
10. [File Structure](#10-file-structure)
11. [Development Setup](#11-development-setup)
12. [Known Issues & Workarounds](#12-known-issues--workarounds)

---

## 1. Project Overview

JpopRadar is a web application for tracking Japanese pop (J-pop) concerts. Users can browse upcoming concerts, filter by artist or city, view detailed event information including ticket prices and vendors, and navigate concerts via an interactive calendar.

The application consists of:
- A **Spring Boot REST API** backend that serves concert data
- A **Vue 3 Single-Page Application** frontend for browsing and discovery

Concert data is sourced from a pre-scanned JSON file (`concertsScan.json`) containing rich event details (dates, venues, ticket info, pricing), and is also persisted in an H2 in-memory database via JPA entities.

---

## 2. Technology Stack

### Backend

| Component | Technology | Version |
|---|---|---|
| Language | Java | 21 |
| Framework | Spring Boot | 3.2.3 |
| Build Tool | Maven | 3.x |
| ORM | Spring Data JPA + Hibernate | 6.x |
| Database (Dev) | H2 In-Memory | 2.x |
| Server Port | — | 8080 |

### Frontend

| Component | Technology | Version |
|---|---|---|
| Language | JavaScript (ES2022+) | — |
| Framework | Vue | 3.4 |
| Build Tool | Vite | 5.2 |
| State Management | Pinia | 2.1 |
| Routing | Vue Router | 4.3 |
| HTTP Client | Axios | 1.6 |
| Dev Server Port | — | 5173 |

---

## 3. Architecture

The application follows a classic client-server architecture with clear separation of concerns on both sides.

**Backend layered architecture:**
```
HTTP Request
    ↓
Controller Layer   — Receives HTTP, validates input, delegates to service
    ↓
Service Layer      — Business logic, orchestrates repository calls
    ↓
Repository Layer   — Spring Data JPA interfaces, issues SQL queries
    ↓
Model Layer        — JPA @Entity classes mapped to DB tables
    ↓
H2 In-Memory DB
```

**Frontend layered architecture:**
```
User Interaction
    ↓
View (*.vue)        — Page-level components, render UI, dispatch store actions
    ↓
Pinia Store         — Centralized reactive state, loading/error tracking
    ↓
api.js (Axios)      — Single HTTP abstraction layer, maps to backend endpoints
    ↓
Vite Dev Proxy      — Transparently forwards /api/* to http://localhost:8080
    ↓
Backend REST API
```

---

## 4. Data Model

### 4.1 Concert Entity (Database)

The `Concert` JPA entity is the primary database-backed model.

| Field | Type | Constraints | Description |
|---|---|---|---|
| `id` | `Long` | PK, auto-generated | Unique identifier |
| `artist` | `String` | NOT NULL | Artist or band name |
| `venue` | `String` | — | Venue name |
| `city` | `String` | — | City where concert is held |
| `concertDate` | `LocalDate` | — | Date of the concert |
| `ticketUrl` | `String` | — | URL for purchasing tickets |

> **Note:** The field is named `concertDate` (not `date`) to avoid a reserved keyword conflict in Hibernate 6 HQL. Using `date` as a field name causes query generation failures.

### 4.2 Concert Scan Object (JSON)

The `concertsScan.json` file contains richer concert objects used for the main browsing experience. These are **not** persisted to the database; they are served directly as raw JSON.

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Unique identifier |
| `artist` | `String` | Artist or band name |
| `title` | `String` | Concert title / event name |
| `venue` | `String` | Full venue name |
| `ticket_status` | `String` | e.g. `"sold_out"`, `"available"` |
| `dates` | `String[]` | Array of date strings in `YYYY.MM.DD` format |
| `open_time` | `String` | Doors open time |
| `start_time` | `String` | Show start time |
| `prices` | `String[]` | Array of price strings |
| `ticket_sale_date` | `String` | When tickets go on sale |
| `ticket_vendors` | `Object[]` | Array of `{ name, url }` vendor objects |
| `official_site` | `String` | Official artist/event site |
| `detail_url` | `String` | Source detail page URL |
| `contact` | `String` | Contact information |

---

## 5. Backend Specification

### 5.1 Entry Point

**File:** `backend/src/main/java/com/jpopradar/JpopRadarApplication.java`

Standard `@SpringBootApplication` entry. No custom bootstrapping logic beyond Spring defaults.

### 5.2 Model

**File:** `backend/src/main/java/com/jpopradar/model/Concert.java`

- Annotated with `@Entity`, `@Table(name = "concerts")`
- ID uses `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- `concertDate` is a `LocalDate` (stored as `DATE` column)
- All fields have standard getters/setters (or Lombok if configured)

### 5.3 Repository

**File:** `backend/src/main/java/com/jpopradar/repository/ConcertRepository.java`

Extends `JpaRepository<Concert, Long>`. Provides:

| Method | Query | Description |
|---|---|---|
| `findByCityIgnoreCase(String city)` | `WHERE UPPER(city) = UPPER(?)` | Filter by city |
| `findByArtistIgnoreCase(String artist)` | `WHERE UPPER(artist) = UPPER(?)` | Filter by artist |
| `findByConcertDateAfterOrderByConcertDateAsc(LocalDate date)` | `WHERE concertDate > ? ORDER BY concertDate ASC` | Upcoming concerts |

Inherited from `JpaRepository`: `findAll()`, `findById()`, `save()`, `deleteById()`, etc.

### 5.4 Service

**File:** `backend/src/main/java/com/jpopradar/service/ConcertService.java`

Annotated with `@Service`. Wraps all repository calls. Methods:

| Method | Returns | Description |
|---|---|---|
| `getAllConcerts()` | `List<Concert>` | All concerts |
| `getConcertById(Long id)` | `Optional<Concert>` | Single concert by ID |
| `createConcert(Concert)` | `Concert` | Persist new concert |
| `updateConcert(Long id, Concert)` | `Concert` | Update existing; throws if not found |
| `deleteConcert(Long id)` | `void` | Delete by ID |
| `getConcertsByCity(String city)` | `List<Concert>` | Case-insensitive city filter |
| `getConcertsByArtist(String artist)` | `List<Concert>` | Case-insensitive artist filter |
| `getUpcomingConcerts()` | `List<Concert>` | Future concerts sorted ASC |

### 5.5 Controller

**File:** `backend/src/main/java/com/jpopradar/controller/ConcertController.java`

- Annotated with `@RestController`, `@RequestMapping("/api/concerts")`
- `@CrossOrigin(origins = "http://localhost:5173")` allows requests from the Vue dev server
- Injects `ConcertService` via constructor injection
- Also reads `concerts.scan.file` property path to serve `concertsScan.json`

Full endpoint table in section 7.

### 5.6 Database Configuration

- **Dev**: H2 in-memory (`jdbc:h2:mem:jpopradar`)
  - Schema: auto-created by Hibernate (`ddl-auto=create-drop`) on startup, dropped on shutdown
  - `spring.jpa.defer-datasource-initialization=true` ensures Hibernate creates schema before any SQL init scripts run
  - H2 web console enabled at `/h2-console` (JDBC URL: `jdbc:h2:mem:jpopradar`, user: `sa`, no password)
- **Production**: Switch datasource properties to MySQL or PostgreSQL (see section 9)

---

## 6. Frontend Specification

### 6.1 Application Entry

**File:** `frontend/src/main.js`

Bootstraps the Vue 3 application:
1. Creates app from `App.vue`
2. Installs Pinia (`createPinia()`)
3. Installs Vue Router
4. Mounts to `#app` in `index.html`

### 6.2 Root Component

**File:** `frontend/src/App.vue`

- Renders sticky navigation bar with brand name "JpopRadar" and links to Home (`/`) and Concerts (`/concerts`)
- Contains `<RouterView>` for page-level component slot

### 6.3 Router

**File:** `frontend/src/router/index.js`

| Route | Component | Loading | Description |
|---|---|---|---|
| `/` | `HomeView` | Eager | Home page with calendar |
| `/concerts` | `ConcertsView` | Lazy | Concert list |
| `/concerts/:id` | `ConcertDetailView` | Lazy | Concert detail |

Query parameter on `/concerts`: `?date=YYYY-MM-DD` — Pre-filters the list to a specific date when navigated from the calendar.

### 6.4 API Service Layer

**File:** `frontend/src/services/api.js`

Single Axios instance with `baseURL: '/api'`. All HTTP calls are centralised here.

| Export | HTTP | Path | Description |
|---|---|---|---|
| `concertApi.getAll()` | GET | `/concerts` | All DB concerts |
| `concertApi.getById(id)` | GET | `/concerts/{id}` | Single DB concert |
| `concertApi.create(concert)` | POST | `/concerts` | Create concert |
| `concertApi.update(id, concert)` | PUT | `/concerts/{id}` | Update concert |
| `concertApi.delete(id)` | DELETE | `/concerts/{id}` | Delete concert |
| `concertApi.getUpcoming()` | GET | `/concerts/upcoming` | Upcoming DB concerts |
| `concertApi.getByCity(city)` | GET | `/concerts/by-city` | Filter by city |
| `concertApi.getByArtist(artist)` | GET | `/concerts/by-artist` | Filter by artist |
| `concertApi.getScan()` | GET | `/concerts/scan` | Rich JSON data |

### 6.5 Pinia Store

**File:** `frontend/src/stores/concertStore.js`

Store ID: `concerts`

**State:**

| Property | Type | Initial | Description |
|---|---|---|---|
| `concerts` | `Concert[]` | `[]` | DB-backed concert list |
| `scanConcerts` | `ScanConcert[]` | `[]` | Rich JSON concert list |
| `currentConcert` | `ScanConcert \| null` | `null` | Currently viewed concert |
| `loading` | `boolean` | `false` | Async operation in progress |
| `error` | `string \| null` | `null` | Last error message |

**Actions:**

| Action | Async | Side Effects | Description |
|---|---|---|---|
| `fetchAll()` | Yes | Sets `concerts` | Load all DB concerts |
| `fetchById(id)` | Yes | Sets `currentConcert` from `scanConcerts` | Find scan concert by ID |
| `fetchScan()` | Yes | Sets `scanConcerts` | Load all scan concerts |
| `fetchUpcoming()` | Yes | Sets `scanConcerts` to future events | Filter scan to future dates |
| `fetchByCity(city)` | Yes | Sets `concerts` | Filter DB concerts by city |
| `fetchByArtist(artist)` | Yes | Sets `concerts` | Filter DB concerts by artist |
| `create(concert)` | Yes | Appends to `concerts` | Create and persist |
| `update(id, concert)` | Yes | Updates item in `concerts` | Update and persist |
| `remove(id)` | Yes | Removes from `concerts` | Delete from DB |

> All actions set `loading = true` before the request and `loading = false` (plus `error` if failed) in the `finally` block.

### 6.6 Views

#### HomeView (`frontend/src/views/HomeView.vue`)

- **Lifecycle**: On `mounted`, calls `concertStore.fetchUpcoming()`
- **Layout**: Hero section (headline + CTA) + two-column responsive grid
  - Left column: `ConcertCalendar` component
  - Right column: Preview of first 3 upcoming concerts (using `ConcertCard`)
- **Interaction**: Clicking a calendar date navigates to `/concerts?date=YYYY-MM-DD`

#### ConcertsView (`frontend/src/views/ConcertsView.vue`)

- **Lifecycle**: On `mounted`, calls `concertStore.fetchScan()`
- **Features**:
  - Text search input: real-time artist name filtering (case-insensitive)
  - Month dropdown: filter concerts to a specific month
  - Date chip: shown when navigated from calendar with `?date=` param; click to clear
  - Concert list rendered with `ConcertCard` components
- **Filtering logic** (computed, client-side on `scanConcerts`):
  1. Apply artist text filter (substring match)
  2. Apply month filter (extract month from dates array via regex)
  3. Apply date chip filter (exact date match in dates array via regex)
  4. Sort by earliest date in each concert's `dates` array

#### ConcertDetailView (`frontend/src/views/ConcertDetailView.vue`)

- **Lifecycle**: On `mounted`, resolves `route.params.id`; if `scanConcerts` not loaded, calls `fetchScan()` first, then calls `fetchById(id)`
- **Layout**: Two-column grid (main content + sticky sidebar)
- **Sections**:
  1. Header: artist name, concert title, sold-out badge (if `ticket_status === 'sold_out'`)
  2. Venue & Dates: venue name, list of formatted dates (YYYY.MM.DD → localized Japanese date string), open/start times
  3. Tickets: price list, ticket sale date, vendor link buttons
  4. Links: official site and source detail URL
  5. Contact: contact info string
- **Back button**: navigates to `/concerts`

### 6.7 Components

#### ConcertCard (`frontend/src/components/ConcertCard.vue`)

- **Props**: `concert` (ScanConcert object)
- **Renders**: Artist name, concert title, venue, formatted first date from `dates[]`, sold-out badge
- **Action**: "View Details" button routes to `/concerts/{concert.id}`

#### ConcertCalendar (`frontend/src/components/ConcertCalendar.vue`)

- **Props**: `concerts` (ScanConcert array)
- **Emits**: `date-click` — payload: `String` in `YYYY-MM-DD` format
- **Internal state**: `currentYear` (Number), `currentMonth` (Number, 0-indexed)
- **Features**:
  - Previous / next month navigation buttons
  - 7×6 grid (42 cells) with Mon–Sun column headers
  - Highlights today's date
  - Highlights dates that have at least one concert with a subtle indicator dot
  - Hover tooltip on concert-date cells listing artist names for that date
  - Clicking any day cell emits `date-click`
- **Date parsing**: Extracts dates from `concert.dates[]` using regex `/\d{4}\.\d{2}\.\d{2}/`, converts to `YYYY-MM-DD` for comparison

### 6.8 Styling

**File:** `frontend/src/assets/main.css`

- Global CSS reset (`*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0 }`)
- **Theme**: Dark — background `#0f0f13`, primary text `#e8e8f0`
- **Navigation**: Sticky top bar with semi-transparent backdrop blur
- **Buttons**: `.btn-primary` (purple accent), `.btn-secondary` (outlined)
- **Cards**: Box shadow with purple glow on hover (`box-shadow` with `rgba(139,92,246,...)`)
- **Status classes**: `.loading` (muted text), `.error` (red accent)
- **Responsive breakpoint**: `@media (max-width: 768px)` — collapses multi-column layouts to single column
- **Typography**: System font stack with Inter (loaded via Google Fonts) as primary

---

## 7. REST API Contract

### Base URL
- Development: `http://localhost:8080/api/concerts`
- Via Vite proxy: `/api/concerts`

### Endpoints

#### `GET /api/concerts`
Returns all concert records from the database.

- **Response**: `200 OK`, `Concert[]` JSON array
- **Example response**:
```json
[
  {
    "id": 1,
    "artist": "YOASOBI",
    "venue": "Makuhari Messe",
    "city": "Chiba",
    "concertDate": "2025-06-15",
    "ticketUrl": "https://example.com/ticket/1"
  }
]
```

---

#### `GET /api/concerts/{id}`
Returns a single concert by ID.

- **Path param**: `id` (Long)
- **Response**: `200 OK` with Concert JSON, or `404 Not Found`

---

#### `POST /api/concerts`
Creates a new concert record.

- **Request body**: Concert JSON (without `id`)
- **Response**: `201 Created` with the created Concert (including generated `id`)

---

#### `PUT /api/concerts/{id}`
Replaces an existing concert record.

- **Path param**: `id` (Long)
- **Request body**: Full Concert JSON (without `id`)
- **Response**: `200 OK` with updated Concert, or `404 Not Found`

---

#### `DELETE /api/concerts/{id}`
Deletes a concert record.

- **Path param**: `id` (Long)
- **Response**: `204 No Content`

---

#### `GET /api/concerts/upcoming`
Returns all concerts where `concertDate` is after today, sorted ascending by date.

- **Response**: `200 OK`, `Concert[]`

---

#### `GET /api/concerts/by-city?city={city}`
Filters concerts by city (case-insensitive exact match).

- **Query param**: `city` (String)
- **Response**: `200 OK`, `Concert[]`

---

#### `GET /api/concerts/by-artist?artist={artist}`
Filters concerts by artist (case-insensitive exact match).

- **Query param**: `artist` (String)
- **Response**: `200 OK`, `Concert[]`

---

#### `GET /api/concerts/scan`
Serves the raw `concertsScan.json` file as a JSON response. This is the primary data source for the frontend browsing experience.

- **Response**: `200 OK`, `ScanConcert[]` (raw file contents)
- **Data size**: ~73KB, 100+ concert objects

---

## 8. Data Flow

### Browsing Flow (Main Use Case)

```
1. User opens HomeView
   HomeView.mounted() → concertStore.fetchUpcoming()
   → api.getScan() → GET /api/concerts/scan → reads concertsScan.json
   ← sets scanConcerts[], filters to future dates

2. ConcertCalendar receives scanConcerts as prop
   → builds date map: { "YYYY-MM-DD": ["Artist1", "Artist2"] }
   → renders calendar with concert indicators

3. User clicks a date on the calendar
   HomeView date-click handler → router.push('/concerts?date=YYYY-MM-DD')

4. ConcertsView.mounted() → concertStore.fetchScan() (if not already loaded)
   → reads date query param → initializes date chip filter
   → computed filteredConcerts applies artist + month + date filters

5. User clicks "View Details" on a ConcertCard
   → router.push('/concerts/:id')

6. ConcertDetailView.mounted()
   → if scanConcerts empty: concertStore.fetchScan()
   → concertStore.fetchById(id) → finds in scanConcerts by id
   → renders full detail view
```

---

## 9. Configuration

### Backend — `application.properties`

```properties
# Application name
spring.application.name=jpopradar

# H2 In-memory datasource (dev)
spring.datasource.url=jdbc:h2:mem:jpopradar
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA / Hibernate
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop       # Drop and recreate on every restart
spring.jpa.defer-datasource-initialization=true  # Schema before SQL init
spring.jpa.show-sql=true                         # Log SQL to console

# H2 Web Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Server
server.port=8080

# Concert scan data file path (relative to working directory)
concerts.scan.file=src/main/resources/concertsScan.json
```

**Switching to MySQL:**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/jpopradar
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=your_user
spring.datasource.password=your_password
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update   # or validate in production
```

### Frontend — `vite.config.js`

```javascript
server: {
  port: 5173,
  proxy: {
    '/api': 'http://localhost:8080'   // All /api/* forwarded to backend
  }
}
```

---

## 10. File Structure

```
JpopRadar/
├── CLAUDE.md                          # Claude Code instructions
├── SPEC_EN.md                         # This document
├── SPEC_ZH.md                         # Traditional Chinese spec
├── README.md                          # Project readme
├── tutorial.md                        # Development notes (Chinese)
│
├── backend/
│   ├── pom.xml                        # Maven build config
│   └── src/main/
│       ├── java/com/jpopradar/
│       │   ├── JpopRadarApplication.java          # Entry point
│       │   ├── controller/
│       │   │   └── ConcertController.java          # REST endpoints
│       │   ├── service/
│       │   │   └── ConcertService.java             # Business logic
│       │   ├── repository/
│       │   │   └── ConcertRepository.java          # Data access
│       │   └── model/
│       │       └── Concert.java                    # JPA entity
│       └── resources/
│           ├── application.properties              # App config
│           ├── data.sql                            # Seed data (optional)
│           └── concertsScan.json                   # Rich concert data (~73KB)
│
└── frontend/
    ├── index.html                     # HTML entry
    ├── package.json                   # NPM config
    ├── vite.config.js                 # Vite config
    └── src/
        ├── main.js                    # App bootstrap
        ├── App.vue                    # Root component
        ├── assets/
        │   └── main.css               # Global styles
        ├── router/
        │   └── index.js               # Route definitions
        ├── services/
        │   └── api.js                 # Axios HTTP layer
        ├── stores/
        │   └── concertStore.js        # Pinia store
        ├── views/
        │   ├── HomeView.vue           # Home page
        │   ├── ConcertsView.vue       # Concert list
        │   └── ConcertDetailView.vue  # Concert detail
        └── components/
            ├── ConcertCard.vue        # Concert card component
            └── ConcertCalendar.vue    # Interactive calendar
```

---

## 11. Development Setup

### Prerequisites

| Tool | Required Version |
|---|---|
| Java (JDK) | 21+ (Temurin 25 confirmed working) |
| Maven | 3.x (system install or `./mvnw`) |
| Node.js | 18+ |
| npm | 9+ |

### Starting the Backend

Due to a classloader incompatibility between the system Maven and `spring-boot-maven-plugin` 3.2.3 on this machine, `./mvnw spring-boot:run` does not work. Use the manual classpath launch:

```bash
cd backend

# Step 1: Compile
JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-25.0.1.8-hotspot" mvn compile -q

# Step 2: Resolve classpath
JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-25.0.1.8-hotspot" \
  mvn dependency:build-classpath -Dmdep.outputFile=target/classpath.txt -q

# Step 3: Run
CP=$(cat target/classpath.txt)
"C:/Program Files/Eclipse Adoptium/jdk-25.0.1.8-hotspot/bin/java" \
  -cp "target/classes;$CP" com.jpopradar.JpopRadarApplication
```

Backend starts at: `http://localhost:8080`
H2 Console: `http://localhost:8080/h2-console`

### Starting the Frontend

```bash
cd frontend
npm install    # First time only
npm run dev
```

Frontend starts at: `http://localhost:5173`

---

## 12. Known Issues & Workarounds

### Issue 1: Maven Plugin Classloader Bug

**Symptom:** `./mvnw spring-boot:run` and `mvn package` fail with `PluginContainerException`

**Cause:** System Maven 3.9.12 (installed via Chocolatey) has a classloader incompatibility with `spring-boot-maven-plugin` 3.2.3

**Workaround:** Use the 3-step manual compile + classpath + java launch described in section 11.

---

### Issue 2: Java Version Mismatch

**Symptom:** Compilation fails with "wrong class file version"

**Cause:** Maven defaults to Amazon Corretto 8 internally; the project requires Java 21+

**Workaround:** Explicitly set `JAVA_HOME` to Temurin 25 path: `C:/Program Files/Eclipse Adoptium/jdk-25.0.1.8-hotspot`

---

### Issue 3: Hibernate Reserved Keyword `date`

**Symptom:** HQL query generation fails when an entity field is named `date`

**Cause:** `date` is a reserved keyword in Hibernate 6 HQL

**Workaround:** Field renamed to `concertDate` in `Concert.java`. Do not rename back to `date`.

---

### Issue 4: Two Parallel Data Sources

**Context:** The frontend uses `concertsScan.json` (via `/api/concerts/scan`) for rich data, while the database-backed `/api/concerts` endpoints hold simplified entities. They are currently not synchronized.

**Impact:** Concert detail views and the browsing/calendar experience use scan data only. The database CRUD endpoints are available but not reflected in the main UI.

**Future consideration:** Migrate scan data import into a startup service that seeds the database from `concertsScan.json`, so there is a single data source.

---

*Document generated: 2026-03-11*
