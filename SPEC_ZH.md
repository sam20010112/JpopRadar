# JpopRadar — 技術規格文件

---

## 目錄

1. [專案概覽](#1-專案概覽)
2. [技術堆疊](#2-技術堆疊)
3. [架構設計](#3-架構設計)
4. [資料模型](#4-資料模型)
5. [後端規格](#5-後端規格)
6. [前端規格](#6-前端規格)
7. [REST API 規格](#7-rest-api-規格)
8. [資料流程](#8-資料流程)
9. [組態設定](#9-組態設定)
10. [檔案結構](#10-檔案結構)
11. [開發環境建置](#11-開發環境建置)
12. [已知問題與解決方案](#12-已知問題與解決方案)

---

## 1. 專案概覽

JpopRadar 是一個追蹤日本流行音樂（J-pop）演唱會的網頁應用程式。使用者可以瀏覽即將舉辦的演唱會、依照藝人或城市篩選、查看活動詳細資訊（包含票價與購票管道），並透過互動式日曆瀏覽演唱會排程。

本應用程式由以下兩部分組成：
- **Spring Boot REST API** 後端：負責提供演唱會資料
- **Vue 3 單頁應用程式（SPA）** 前端：負責瀏覽與探索功能

演唱會資料來源為預先掃描的 JSON 檔案（`concertsScan.json`），內含豐富的活動資訊（日期、場地、購票資訊、票價），同時也透過 JPA 實體持久化至 H2 記憶體資料庫。

---

## 2. 技術堆疊

### 後端

| 元件 | 技術 | 版本 |
|---|---|---|
| 程式語言 | Java | 21 |
| 框架 | Spring Boot | 3.2.3 |
| 建置工具 | Maven | 3.x |
| ORM | Spring Data JPA + Hibernate | 6.x |
| 資料庫（開發） | H2 In-Memory | 2.x |
| 伺服器埠號 | — | 8080 |

### 前端

| 元件 | 技術 | 版本 |
|---|---|---|
| 程式語言 | JavaScript (ES2022+) | — |
| 框架 | Vue | 3.4 |
| 建置工具 | Vite | 5.2 |
| 狀態管理 | Pinia | 2.1 |
| 路由 | Vue Router | 4.3 |
| HTTP 客戶端 | Axios | 1.6 |
| 開發伺服器埠號 | — | 5173 |

---

## 3. 架構設計

本應用程式採用經典的客戶端—伺服器架構，前後端各自有清楚的關注點分離。

**後端分層架構：**
```
HTTP 請求
    ↓
Controller 層   — 接收 HTTP 請求、驗證輸入、委派給 Service
    ↓
Service 層      — 業務邏輯、協調 Repository 呼叫
    ↓
Repository 層   — Spring Data JPA 介面、發出 SQL 查詢
    ↓
Model 層        — 對應資料庫表格的 JPA @Entity 類別
    ↓
H2 記憶體資料庫
```

**前端分層架構：**
```
使用者操作
    ↓
View（*.vue）    — 頁面級元件、渲染 UI、派發 Store Action
    ↓
Pinia Store      — 集中式響應狀態、追蹤 loading/error 狀態
    ↓
api.js（Axios）  — 單一 HTTP 抽象層、對應後端端點
    ↓
Vite Dev Proxy   — 透明地將 /api/* 轉發至 http://localhost:8080
    ↓
後端 REST API
```

---

## 4. 資料模型

### 4.1 Concert 實體（資料庫）

`Concert` JPA 實體是主要的資料庫持久化模型。

| 欄位 | 型別 | 限制 | 說明 |
|---|---|---|---|
| `id` | `Long` | PK，自動產生 | 唯一識別碼 |
| `artist` | `String` | NOT NULL | 藝人或樂團名稱 |
| `venue` | `String` | — | 場地名稱 |
| `city` | `String` | — | 演唱會所在城市 |
| `concertDate` | `LocalDate` | — | 演唱會日期 |
| `ticketUrl` | `String` | — | 購票連結 |

> **注意：** 欄位命名為 `concertDate` 而非 `date`，是為了避免與 Hibernate 6 HQL 保留關鍵字衝突。若使用 `date` 作為欄位名稱，將導致查詢產生失敗。

### 4.2 Concert 掃描物件（JSON）

`concertsScan.json` 檔案包含更豐富的演唱會物件，用於主要的瀏覽體驗。這些資料**不**持久化至資料庫，而是直接以原始 JSON 形式提供。

| 欄位 | 型別 | 說明 |
|---|---|---|
| `id` | `String` | 唯一識別碼 |
| `artist` | `String` | 藝人或樂團名稱 |
| `title` | `String` | 演唱會標題或活動名稱 |
| `venue` | `String` | 完整場地名稱 |
| `ticket_status` | `String` | 例如 `"sold_out"`、`"available"` |
| `dates` | `String[]` | 日期字串陣列（格式：`YYYY.MM.DD`） |
| `open_time` | `String` | 開場時間 |
| `start_time` | `String` | 開演時間 |
| `prices` | `String[]` | 票價字串陣列 |
| `ticket_sale_date` | `String` | 購票開始日期 |
| `ticket_vendors` | `Object[]` | 購票管道陣列，每項包含 `{ name, url }` |
| `official_site` | `String` | 官方網站連結 |
| `detail_url` | `String` | 來源詳細頁面連結 |
| `contact` | `String` | 聯絡資訊 |

---

## 5. 後端規格

### 5.1 進入點

**檔案：** `backend/src/main/java/com/jpopradar/JpopRadarApplication.java`

標準的 `@SpringBootApplication` 進入點，除 Spring 預設行為外無額外初始化邏輯。

### 5.2 模型

**檔案：** `backend/src/main/java/com/jpopradar/model/Concert.java`

- 標記 `@Entity`、`@Table(name = "concerts")`
- ID 使用 `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- `concertDate` 為 `LocalDate`（儲存為 `DATE` 欄位）
- 所有欄位具備標準 getter/setter（若設定 Lombok 則自動產生）

### 5.3 資料存取層（Repository）

**檔案：** `backend/src/main/java/com/jpopradar/repository/ConcertRepository.java`

繼承自 `JpaRepository<Concert, Long>`，提供以下方法：

| 方法 | 查詢邏輯 | 說明 |
|---|---|---|
| `findByCityIgnoreCase(String city)` | `WHERE UPPER(city) = UPPER(?)` | 依城市篩選 |
| `findByArtistIgnoreCase(String artist)` | `WHERE UPPER(artist) = UPPER(?)` | 依藝人篩選 |
| `findByConcertDateAfterOrderByConcertDateAsc(LocalDate date)` | `WHERE concertDate > ? ORDER BY concertDate ASC` | 即將到來的演唱會 |

繼承自 `JpaRepository`：`findAll()`、`findById()`、`save()`、`deleteById()` 等。

### 5.4 業務邏輯層（Service）

**檔案：** `backend/src/main/java/com/jpopradar/service/ConcertService.java`

標記 `@Service`，封裝所有 Repository 呼叫，提供以下方法：

| 方法 | 回傳型別 | 說明 |
|---|---|---|
| `getAllConcerts()` | `List<Concert>` | 全部演唱會 |
| `getConcertById(Long id)` | `Optional<Concert>` | 依 ID 取得單一演唱會 |
| `createConcert(Concert)` | `Concert` | 新增演唱會 |
| `updateConcert(Long id, Concert)` | `Concert` | 更新演唱會；找不到時拋出例外 |
| `deleteConcert(Long id)` | `void` | 依 ID 刪除 |
| `getConcertsByCity(String city)` | `List<Concert>` | 不分大小寫的城市篩選 |
| `getConcertsByArtist(String artist)` | `List<Concert>` | 不分大小寫的藝人篩選 |
| `getUpcomingConcerts()` | `List<Concert>` | 按日期升冪排序的未來演唱會 |

### 5.5 控制器層（Controller）

**檔案：** `backend/src/main/java/com/jpopradar/controller/ConcertController.java`

- 標記 `@RestController`、`@RequestMapping("/api/concerts")`
- `@CrossOrigin(origins = "http://localhost:5173")` 允許 Vue 開發伺服器的請求
- 透過建構子注入 `ConcertService`
- 讀取 `concerts.scan.file` 屬性路徑以提供 `concertsScan.json`

完整端點表格請見第 7 節。

### 5.6 資料庫組態

- **開發環境**：H2 記憶體資料庫（`jdbc:h2:mem:jpopradar`）
  - Schema：啟動時由 Hibernate 自動建立（`ddl-auto=create-drop`），關閉時自動刪除
  - `spring.jpa.defer-datasource-initialization=true` 確保 Hibernate 在 SQL 初始化腳本執行前完成 Schema 建立
  - H2 網頁控制台啟用於 `/h2-console`（JDBC URL：`jdbc:h2:mem:jpopradar`，使用者：`sa`，無密碼）
- **正式環境**：切換 datasource 屬性至 MySQL 或 PostgreSQL（見第 9 節）

---

## 6. 前端規格

### 6.1 應用程式進入點

**檔案：** `frontend/src/main.js`

Vue 3 應用程式啟動流程：
1. 從 `App.vue` 建立應用程式實例
2. 安裝 Pinia（`createPinia()`）
3. 安裝 Vue Router
4. 掛載至 `index.html` 中的 `#app`

### 6.2 根元件

**檔案：** `frontend/src/App.vue`

- 渲染固定頂部導航列，包含品牌名稱「JpopRadar」及首頁（`/`）與演唱會（`/concerts`）連結
- 包含 `<RouterView>` 作為頁面級元件的插槽

### 6.3 路由

**檔案：** `frontend/src/router/index.js`

| 路由 | 元件 | 載入方式 | 說明 |
|---|---|---|---|
| `/` | `HomeView` | Eager（立即載入） | 含日曆的首頁 |
| `/concerts` | `ConcertsView` | Lazy（延遲載入） | 演唱會列表 |
| `/concerts/:id` | `ConcertDetailView` | Lazy（延遲載入） | 演唱會詳情 |

`/concerts` 支援查詢參數：`?date=YYYY-MM-DD` — 從日曆點擊導航時，預先篩選特定日期的演唱會。

### 6.4 API 服務層

**檔案：** `frontend/src/services/api.js`

使用單一 Axios 實例，`baseURL` 設為 `/api`。所有 HTTP 呼叫集中在此。

| 匯出方法 | HTTP | 路徑 | 說明 |
|---|---|---|---|
| `concertApi.getAll()` | GET | `/concerts` | 全部資料庫演唱會 |
| `concertApi.getById(id)` | GET | `/concerts/{id}` | 單一資料庫演唱會 |
| `concertApi.create(concert)` | POST | `/concerts` | 新增演唱會 |
| `concertApi.update(id, concert)` | PUT | `/concerts/{id}` | 更新演唱會 |
| `concertApi.delete(id)` | DELETE | `/concerts/{id}` | 刪除演唱會 |
| `concertApi.getUpcoming()` | GET | `/concerts/upcoming` | 即將到來的資料庫演唱會 |
| `concertApi.getByCity(city)` | GET | `/concerts/by-city` | 依城市篩選 |
| `concertApi.getByArtist(artist)` | GET | `/concerts/by-artist` | 依藝人篩選 |
| `concertApi.getScan()` | GET | `/concerts/scan` | 豐富 JSON 資料 |

### 6.5 Pinia 狀態管理

**檔案：** `frontend/src/stores/concertStore.js`

Store ID：`concerts`

**狀態（State）：**

| 屬性 | 型別 | 初始值 | 說明 |
|---|---|---|---|
| `concerts` | `Concert[]` | `[]` | 資料庫演唱會列表 |
| `scanConcerts` | `ScanConcert[]` | `[]` | 豐富 JSON 演唱會列表 |
| `currentConcert` | `ScanConcert \| null` | `null` | 目前查看的演唱會 |
| `loading` | `boolean` | `false` | 非同步操作進行中 |
| `error` | `string \| null` | `null` | 最後一次錯誤訊息 |

**動作（Actions）：**

| 動作 | 非同步 | 副作用 | 說明 |
|---|---|---|---|
| `fetchAll()` | 是 | 設定 `concerts` | 載入全部資料庫演唱會 |
| `fetchById(id)` | 是 | 從 `scanConcerts` 設定 `currentConcert` | 從 scanConcerts 依 ID 尋找 |
| `fetchScan()` | 是 | 設定 `scanConcerts` | 載入全部掃描演唱會 |
| `fetchUpcoming()` | 是 | 將 `scanConcerts` 設定為未來活動 | 篩選未來日期 |
| `fetchByCity(city)` | 是 | 設定 `concerts` | 依城市篩選資料庫演唱會 |
| `fetchByArtist(artist)` | 是 | 設定 `concerts` | 依藝人篩選資料庫演唱會 |
| `create(concert)` | 是 | 附加至 `concerts` | 新增並持久化 |
| `update(id, concert)` | 是 | 更新 `concerts` 中的項目 | 更新並持久化 |
| `remove(id)` | 是 | 從 `concerts` 移除 | 從資料庫刪除 |

> 所有 Action 在請求前設定 `loading = true`，並在 `finally` 區塊中設定 `loading = false`（若失敗則同時設定 `error`）。

### 6.6 頁面元件（Views）

#### HomeView（`frontend/src/views/HomeView.vue`）

- **生命週期**：`mounted` 時呼叫 `concertStore.fetchUpcoming()`
- **版面**：英雄區段（標題 + CTA）+ 雙欄響應式網格
  - 左欄：`ConcertCalendar` 元件
  - 右欄：前 3 筆即將到來演唱會預覽（使用 `ConcertCard`）
- **互動**：點擊日曆日期導航至 `/concerts?date=YYYY-MM-DD`

#### ConcertsView（`frontend/src/views/ConcertsView.vue`）

- **生命週期**：`mounted` 時呼叫 `concertStore.fetchScan()`
- **功能**：
  - 文字搜尋輸入：即時藝人名稱篩選（不分大小寫）
  - 月份下拉選單：篩選特定月份的演唱會
  - 日期標籤：從日曆帶有 `?date=` 參數導航時顯示；點擊可清除
  - 演唱會列表使用 `ConcertCard` 元件渲染
- **篩選邏輯**（基於 `scanConcerts` 的客戶端計算屬性）：
  1. 套用藝人文字篩選（子字串比對）
  2. 套用月份篩選（透過 regex 從 dates 陣列提取月份）
  3. 套用日期標籤篩選（透過 regex 在 dates 陣列中精確比對日期）
  4. 依每個演唱會 `dates` 陣列中最早的日期排序

#### ConcertDetailView（`frontend/src/views/ConcertDetailView.vue`）

- **生命週期**：`mounted` 時解析 `route.params.id`；若 `scanConcerts` 尚未載入，先呼叫 `fetchScan()` 後再呼叫 `fetchById(id)`
- **版面**：雙欄網格（主要內容 + 固定側邊欄）
- **區段**：
  1. 標頭：藝人名稱、演唱會標題、完售標籤（若 `ticket_status === 'sold_out'`）
  2. 場地與日期：場地名稱、格式化日期列表（YYYY.MM.DD → 本地化日文日期字串）、開場/開演時間
  3. 購票：票價列表、購票開始日期、購票管道按鈕連結
  4. 連結：官方網站與來源詳情連結
  5. 聯絡：聯絡資訊字串
- **返回按鈕**：導航回 `/concerts`

### 6.7 可重用元件（Components）

#### ConcertCard（`frontend/src/components/ConcertCard.vue`）

- **Props**：`concert`（ScanConcert 物件）
- **渲染**：藝人名稱、演唱會標題、場地、從 `dates[]` 格式化的第一個日期、完售標籤
- **操作**：「查看詳情」按鈕路由至 `/concerts/{concert.id}`

#### ConcertCalendar（`frontend/src/components/ConcertCalendar.vue`）

- **Props**：`concerts`（ScanConcert 陣列）
- **Emits**：`date-click` — 載荷：`YYYY-MM-DD` 格式的 `String`
- **內部狀態**：`currentYear`（Number）、`currentMonth`（Number，0 為基準）
- **功能**：
  - 上月/下月導航按鈕
  - 7×6 網格（42 格）含週一至週日欄位標頭
  - 高亮標示今日日期
  - 以小圓點指示器標示有演唱會的日期
  - 滑鼠懸停於演唱會日期格時顯示藝人名稱工具提示
  - 點擊任意日期格觸發 `date-click` 事件
- **日期解析**：使用 regex `/\d{4}\.\d{2}\.\d{2}/` 從 `concert.dates[]` 提取日期，轉換為 `YYYY-MM-DD` 進行比對

### 6.8 樣式

**檔案：** `frontend/src/assets/main.css`

- 全域 CSS 重置（`*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0 }`）
- **主題**：深色 — 背景 `#0f0f13`、主要文字 `#e8e8f0`
- **導航列**：固定頂部，含半透明背景模糊效果
- **按鈕**：`.btn-primary`（紫色強調色）、`.btn-secondary`（外框式）
- **卡片**：滑鼠懸停時顯示紫色光暈陰影（`box-shadow` 使用 `rgba(139,92,246,...)`）
- **狀態樣式**：`.loading`（柔化文字色）、`.error`（紅色強調色）
- **響應式斷點**：`@media (max-width: 768px)` — 將多欄版面折疊為單欄
- **字型**：系統字型堆疊，主要字型為 Inter（透過 Google Fonts 載入）

---

## 7. REST API 規格

### 基礎 URL
- 開發環境：`http://localhost:8080/api/concerts`
- 透過 Vite Proxy：`/api/concerts`

### 端點

#### `GET /api/concerts`
回傳資料庫中的全部演唱會記錄。

- **回應**：`200 OK`，`Concert[]` JSON 陣列
- **回應範例**：
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
依 ID 回傳單一演唱會。

- **路徑參數**：`id`（Long）
- **回應**：`200 OK` 含 Concert JSON，或 `404 Not Found`

---

#### `POST /api/concerts`
新增演唱會記錄。

- **請求主體**：Concert JSON（不含 `id`）
- **回應**：`201 Created` 含已建立的 Concert（包含產生的 `id`）

---

#### `PUT /api/concerts/{id}`
取代現有演唱會記錄。

- **路徑參數**：`id`（Long）
- **請求主體**：完整 Concert JSON（不含 `id`）
- **回應**：`200 OK` 含更新後的 Concert，或 `404 Not Found`

---

#### `DELETE /api/concerts/{id}`
刪除演唱會記錄。

- **路徑參數**：`id`（Long）
- **回應**：`204 No Content`

---

#### `GET /api/concerts/upcoming`
回傳 `concertDate` 在今日之後的全部演唱會，依日期升冪排序。

- **回應**：`200 OK`，`Concert[]`

---

#### `GET /api/concerts/by-city?city={city}`
依城市篩選演唱會（不分大小寫精確比對）。

- **查詢參數**：`city`（String）
- **回應**：`200 OK`，`Concert[]`

---

#### `GET /api/concerts/by-artist?artist={artist}`
依藝人篩選演唱會（不分大小寫精確比對）。

- **查詢參數**：`artist`（String）
- **回應**：`200 OK`，`Concert[]`

---

#### `GET /api/concerts/scan`
以 JSON 回應提供原始 `concertsScan.json` 檔案。這是前端瀏覽體驗的主要資料來源。

- **回應**：`200 OK`，`ScanConcert[]`（原始檔案內容）
- **資料大小**：約 73KB，100 筆以上演唱會物件

---

## 8. 資料流程

### 瀏覽流程（主要使用情境）

```
1. 使用者開啟 HomeView
   HomeView.mounted() → concertStore.fetchUpcoming()
   → api.getScan() → GET /api/concerts/scan → 讀取 concertsScan.json
   ← 設定 scanConcerts[]，篩選至未來日期

2. ConcertCalendar 接收 scanConcerts 作為 prop
   → 建立日期對應表：{ "YYYY-MM-DD": ["藝人1", "藝人2"] }
   → 渲染含演唱會指示器的日曆

3. 使用者點擊日曆上的日期
   HomeView date-click 處理器 → router.push('/concerts?date=YYYY-MM-DD')

4. ConcertsView.mounted() → concertStore.fetchScan()（若尚未載入）
   → 讀取日期查詢參數 → 初始化日期標籤篩選器
   → computed filteredConcerts 套用藝人 + 月份 + 日期篩選

5. 使用者點擊 ConcertCard 上的「查看詳情」
   → router.push('/concerts/:id')

6. ConcertDetailView.mounted()
   → 若 scanConcerts 為空：concertStore.fetchScan()
   → concertStore.fetchById(id) → 在 scanConcerts 中依 id 尋找
   → 渲染完整詳情頁面
```

---

## 9. 組態設定

### 後端 — `application.properties`

```properties
# 應用程式名稱
spring.application.name=jpopradar

# H2 記憶體資料庫（開發用）
spring.datasource.url=jdbc:h2:mem:jpopradar
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA / Hibernate
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop       # 每次重啟時刪除並重建
spring.jpa.defer-datasource-initialization=true  # Schema 建立後再執行 SQL
spring.jpa.show-sql=true                         # 在控制台記錄 SQL

# H2 網頁控制台
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# 伺服器
server.port=8080

# 演唱會掃描資料檔案路徑（相對於工作目錄）
concerts.scan.file=src/main/resources/concertsScan.json
```

**切換至 MySQL：**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/jpopradar
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=your_user
spring.datasource.password=your_password
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update   # 正式環境建議使用 validate
```

### 前端 — `vite.config.js`

```javascript
server: {
  port: 5173,
  proxy: {
    '/api': 'http://localhost:8080'   // 所有 /api/* 轉發至後端
  }
}
```

---

## 10. 檔案結構

```
JpopRadar/
├── CLAUDE.md                          # Claude Code 指示
├── SPEC_EN.md                         # 英文規格文件
├── SPEC_ZH.md                         # 本文件
├── README.md                          # 專案說明
├── tutorial.md                        # 開發筆記（中文）
│
├── backend/
│   ├── pom.xml                        # Maven 建置設定
│   └── src/main/
│       ├── java/com/jpopradar/
│       │   ├── JpopRadarApplication.java          # 進入點
│       │   ├── controller/
│       │   │   └── ConcertController.java          # REST 端點
│       │   ├── service/
│       │   │   └── ConcertService.java             # 業務邏輯
│       │   ├── repository/
│       │   │   └── ConcertRepository.java          # 資料存取
│       │   └── model/
│       │       └── Concert.java                    # JPA 實體
│       └── resources/
│           ├── application.properties              # 應用程式設定
│           ├── data.sql                            # 種子資料（可選）
│           └── concertsScan.json                   # 豐富演唱會資料（約 73KB）
│
└── frontend/
    ├── index.html                     # HTML 進入點
    ├── package.json                   # NPM 設定
    ├── vite.config.js                 # Vite 設定
    └── src/
        ├── main.js                    # 應用程式啟動
        ├── App.vue                    # 根元件
        ├── assets/
        │   └── main.css               # 全域樣式
        ├── router/
        │   └── index.js               # 路由定義
        ├── services/
        │   └── api.js                 # Axios HTTP 層
        ├── stores/
        │   └── concertStore.js        # Pinia Store
        ├── views/
        │   ├── HomeView.vue           # 首頁
        │   ├── ConcertsView.vue       # 演唱會列表
        │   └── ConcertDetailView.vue  # 演唱會詳情
        └── components/
            ├── ConcertCard.vue        # 演唱會卡片元件
            └── ConcertCalendar.vue    # 互動式日曆
```

---

## 11. 開發環境建置

### 前置需求

| 工具 | 需求版本 |
|---|---|
| Java（JDK） | 21+（已確認 Temurin 25 可用） |
| Maven | 3.x（系統安裝或 `./mvnw`） |
| Node.js | 18+ |
| npm | 9+ |

### 啟動後端

由於此開發機器上系統 Maven 與 `spring-boot-maven-plugin` 3.2.3 存在類別載入器不相容問題，`./mvnw spring-boot:run` 無法正常運作。請使用手動 classpath 啟動方式：

```bash
cd backend

# 步驟 1：編譯
JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-25.0.1.8-hotspot" mvn compile -q

# 步驟 2：解析 classpath
JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-25.0.1.8-hotspot" \
  mvn dependency:build-classpath -Dmdep.outputFile=target/classpath.txt -q

# 步驟 3：執行
CP=$(cat target/classpath.txt)
"C:/Program Files/Eclipse Adoptium/jdk-25.0.1.8-hotspot/bin/java" \
  -cp "target/classes;$CP" com.jpopradar.JpopRadarApplication
```

後端啟動於：`http://localhost:8080`
H2 控制台：`http://localhost:8080/h2-console`

### 啟動前端

```bash
cd frontend
npm install    # 僅首次需要
npm run dev
```

前端啟動於：`http://localhost:5173`

---

## 12. 已知問題與解決方案

### 問題 1：Maven 外掛類別載入器錯誤

**症狀：** `./mvnw spring-boot:run` 與 `mvn package` 皆因 `PluginContainerException` 失敗

**原因：** 透過 Chocolatey 安裝的系統 Maven 3.9.12 與 `spring-boot-maven-plugin` 3.2.3 存在類別載入器不相容問題

**解決方案：** 使用第 11 節描述的三步驟手動編譯 + classpath + java 啟動方式

---

### 問題 2：Java 版本不符

**症狀：** 編譯失敗，顯示「wrong class file version」

**原因：** Maven 內部預設使用 Amazon Corretto 8；本專案需要 Java 21+

**解決方案：** 明確將 `JAVA_HOME` 設定為 Temurin 25 路徑：`C:/Program Files/Eclipse Adoptium/jdk-25.0.1.8-hotspot`

---

### 問題 3：Hibernate 保留關鍵字 `date`

**症狀：** 當實體欄位命名為 `date` 時，HQL 查詢產生失敗

**原因：** `date` 為 Hibernate 6 HQL 中的保留關鍵字

**解決方案：** 在 `Concert.java` 中將欄位重命名為 `concertDate`，請勿改回 `date`

---

### 問題 4：兩套並行資料來源

**背景：** 前端透過 `/api/concerts/scan` 使用 `concertsScan.json` 取得豐富資料，而資料庫端的 `/api/concerts` 端點持有簡化版實體。目前兩者並未同步。

**影響：** 演唱會詳情頁面與瀏覽/日曆體驗僅使用掃描資料。資料庫 CRUD 端點可用，但未反映於主要 UI 中。

**未來考量：** 將掃描資料匯入遷移至啟動服務，於應用程式啟動時從 `concertsScan.json` 填充資料庫，使整個系統只有單一資料來源。

---

*文件產生日期：2026-03-11*
