# JpopRadar 錯誤修復教學

## 問題描述

所有 API 端點（包含 `/api/songs`、`/api/concerts` 等）一律回傳 HTTP 500 錯誤。

## 根本原因

### Spring ApplicationContext 啟動失敗

Spring Boot 的運作機制是：若任何一個 Bean 在初始化時發生例外，整個 ApplicationContext 就會中止載入。結果就是所有 HTTP 請求都會得到 500 回應，即便是跟出錯的 Bean 毫無關係的端點也不例外。

### Hibernate 6 保留關鍵字衝突

問題的核心在於 `Concert` 實體類別（Entity）中有一個欄位名為 `date`，型別為 `LocalDate`。

```java
// 問題所在
private LocalDate date;
```

在 Spring Boot 3.x 所搭載的 Hibernate 6 中，`DATE` 是一個保留的 HQL/JPQL 關鍵字，被用作日期擷取函式（date extraction function）。

當 Spring Data JPA 在啟動時驗證 Repository 中的衍生查詢方法（derived query method）：

```java
// ConcertRepository.java 中的問題查詢
List<Concert> findByDateAfterOrderByDateAsc(LocalDate date);
```

Hibernate 的 HQL 解析器會試圖將這個方法名稱轉換成 JPQL 語句，過程中 `date` 這個欄位名稱與內建的 `DATE` 關鍵字產生衝突，導致解析失敗、拋出例外，進而使整個 ApplicationContext 無法啟動。

---

## 修復方式

解決方法是將 `date` 欄位重新命名為 `concertDate`，避開關鍵字衝突。Hibernate 的 `SpringPhysicalNamingStrategy` 會自動將駝峰式命名（camelCase）轉換為蛇形命名（snake_case），因此資料庫欄位會自動對應到 `concert_date`。

### 修改的檔案

#### 1. `Concert.java`（實體類別）

欄位宣告、建構子、getter、setter 全部更名：

```java
// 修改前
private LocalDate date;

public Concert(String artist, String venue, String city, LocalDate date, String ticketUrl) {
    this.date = date;
}

public LocalDate getDate() { return date; }
public void setDate(LocalDate date) { this.date = date; }
```

```java
// 修改後
private LocalDate concertDate;

public Concert(String artist, String venue, String city, LocalDate concertDate, String ticketUrl) {
    this.concertDate = concertDate;
}

public LocalDate getConcertDate() { return concertDate; }
public void setConcertDate(LocalDate concertDate) { this.concertDate = concertDate; }
```

#### 2. `ConcertRepository.java`（資料存取層）

衍生查詢方法更名，讓 Hibernate HQL 解析器不再碰到保留字：

```java
// 修改前
List<Concert> findByDateAfterOrderByDateAsc(LocalDate date);

// 修改後
List<Concert> findByConcertDateAfterOrderByConcertDateAsc(LocalDate concertDate);
```

#### 3. `ConcertService.java`（服務層）

更新呼叫 setter/getter 與 Repository 方法的地方：

```java
// 修改前
concert.setDate(updated.getDate());
return concertRepository.findByDateAfterOrderByDateAsc(LocalDate.now().minusDays(1));

// 修改後
concert.setConcertDate(updated.getConcertDate());
return concertRepository.findByConcertDateAfterOrderByConcertDateAsc(LocalDate.now().minusDays(1));
```

#### 4. `data.sql`（種子資料）

因為資料庫欄位名稱跟著改變，INSERT 語句的欄位名稱也需要同步更新：

```sql
-- 修改前
INSERT INTO concerts (artist, venue, city, date, ticket_url) VALUES ...

-- 修改後
INSERT INTO concerts (artist, venue, city, concert_date, ticket_url) VALUES ...
```

---

## 啟動系統

### 已知問題：Maven 版本衝突

此專案環境中，Chocolatey 安裝的 Maven 3.9.12 與 `spring-boot-maven-plugin 3.2.3` 存在 classloader 衝突，導致 `mvn spring-boot:run` 及 `mvn package` 均無法正常執行，會拋出 `PluginContainerException`。

### 後端啟動方法（繞過 Maven 插件）

由於 `mvn compile` 本身運作正常（不涉及 Spring Boot Maven 插件），可以先編譯再直接用 `java` 執行：

```bash
# 步驟一：編譯原始碼
cd backend
mvn compile -q

# 步驟二：產生完整的相依性 classpath 清單
mvn dependency:build-classpath -Dmdep.outputFile=target/classpath.txt -q

# 步驟三：直接以 Java 執行（Linux/macOS 以冒號 : 分隔；Windows 以分號 ; 分隔）
CP=$(cat target/classpath.txt)
java -cp "target/classes;$CP" com.jpopradar.JpopRadarApplication
```

### 前端啟動方法

```bash
cd frontend
npm install   # 首次執行時需要
npm run dev
```

前端 Vite 開發伺服器預設在 `http://localhost:5173`，若該埠已被佔用則自動遞增（如 5174）。

---

## 驗證結果

系統啟動後，可透過以下端點確認修復成功：

| 端點 | 預期結果 |
|------|----------|
| `GET http://localhost:8080/api/songs` | 回傳 5 首歌曲（HTTP 200） |
| `GET http://localhost:8080/api/concerts` | 回傳 6 場演唱會（HTTP 200） |
| `GET http://localhost:8080/api/concerts/upcoming` | 回傳未來演唱會清單（HTTP 200） |
| `http://localhost:8080/h2-console` | H2 資料庫控制台，可確認 `concert_date` 欄位存在 |

---

## 總結

| 面向 | 說明 |
|------|------|
| 根本原因 | `date` 欄位名稱與 Hibernate 6 HQL 保留關鍵字衝突 |
| 影響範圍 | ApplicationContext 無法啟動，導致全部 API 回傳 500 |
| 修復策略 | 將 `date` 重新命名為 `concertDate`，共修改 4 個檔案 |
| 資料庫影響 | 欄位自動由 `date` 對應至 `concert_date`（snake_case 轉換） |
