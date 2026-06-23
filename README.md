# ☕ TutorNet Backend - Spring Boot REST API

Đây là phần xử lý logic nghiệp vụ và cơ sở dữ liệu (Backend) của dự án **TutorNet**, cung cấp hệ thống kết nối Gia sư, Học sinh và Phụ huynh. 

---

## 🛠️ Công nghệ & Thư viện sử dụng (Tech Stack)

* **Framework chính**: Spring Boot 4.0.6 (Java 21)
* **Cơ sở dữ liệu**: PostgreSQL
* **ORM**: Spring Data JPA (Hibernate)
* **Database Migrations**: Flyway Database Migration
* **Bảo mật & Phân quyền**: Spring Security (JWT Stateless & Custom RBAC Permission System)
* **Giao tiếp Realtime**: Netty-SocketIO & Spring WebSockets
* **Xuất văn bản**: Thymeleaf & OpenHTMLtoPDF (Tạo PDF hợp đồng điện tử), Apache POI (Xuất Excel)
* **Tiện ích**: Lombok, MapStruct DTO Mapper

---

## ⚙️ Yêu cầu môi trường (Prerequisites)

* **Java Development Kit (JDK) 21**
* **PostgreSQL Server v15+**
* **Redis Server** (Tùy chọn, dùng để tăng tốc độ Cache và quản lý Session)

---

## 🚀 Hướng dẫn cài đặt & Khởi chạy ứng dụng

### 1. Khởi tạo Cơ sở dữ liệu
Truy cập vào PostgreSQL CLI hoặc pgAdmin, tạo một cơ sở dữ liệu mới mang tên `tutor_net`:
```sql
CREATE DATABASE tutor_net;
```

### 2. Cấu hình Biến môi trường (Environment Variables)
Ứng dụng sử dụng cấu hình mặc định trong file [application.yml](src/main/resources/application.yml). Bạn cần thiết lập các biến môi trường sau để kết nối với cơ sở dữ liệu và hệ thống gửi thư:

| Tên biến môi trường | Giá trị mặc định | Mô tả |
| :--- | :--- | :--- |
| `DB_USERNAME` | `postgres` | Tài khoản PostgreSQL |
| `DB_PASSWORD` | `onepiece2016` | Mật khẩu tài khoản PostgreSQL |
| `JWT_SECRET` | `my-very-secret-key-that-is-at-least-256-bits-long-for-hs256` | Khóa bí mật dùng để mã hóa và xác minh JWT Token |
| `SPRING_MAIL_USERNAME` | `vietthanh051103@gmail.com` | Email hệ thống gửi thông báo (SMTP) |
| `SPRING_MAIL_PASSWORD` | `lwcc llsv xttw emra` | Mật khẩu ứng dụng 16 chữ số của Google Gmail |
| `REDIS_PASSWORD` | *(Trống)* | Mật khẩu Redis cache |
| `APP_ALLOWED_ORIGINS` | `http://localhost:3000` | URL của ứng dụng Frontend (Next.js) cho phép CORS |

---

### 3. Các lệnh chạy ứng dụng (Gradle)

Mở terminal trong thư mục `tutor-net-spring-boot-api` và thực hiện các lệnh sau:

* **Chạy ứng dụng ở chế độ Phát triển (Development)**:
  * Trên Windows (PowerShell/CMD):
    ```powershell
    .\gradlew.bat bootRun
    ```
  * Trên Linux/macOS:
    ```bash
    ./gradlew bootRun
    ```

* **Dọn dẹp và đóng gói ứng dụng (Build JAR)**:
  * Trên Windows (PowerShell/CMD):
    ```powershell
    .\gradlew.bat clean build -x test
    ```
  * Trên Linux/macOS:
    ```bash
    ./gradlew clean build -x test
    ```
    *File JAR được đóng gói sẽ nằm trong thư mục `build/libs/tutor-net-0.0.1-SNAPSHOT.jar`.*

* **Khởi chạy ứng dụng bằng file JAR đã đóng gói**:
  ```bash
  java -jar build/libs/tutor-net-0.0.1-SNAPSHOT.jar
  ```

---

## 🔌 Các cổng (Ports) kết nối & Endpoint chính

* **Cổng mặc định**: `8080` (HTTP)
* **Base API Endpoint**: `http://localhost:8080/api/v1`
* **WebSocket Endpoint**: `ws://localhost:8080/ws/websocket`
* **Tài liệu API (Swagger UI)**: `http://localhost:8080/swagger-ui/index.html` (Nếu có cài đặt OpenApi)
* **Thư mục lưu trữ tệp tin upload**: `./uploads` (Ảnh đại diện, chứng chỉ gia sư, tài liệu học tập)