# StarShop

Website bán hoa theo mô hình chuỗi cửa hàng.

**Công nghệ:** Java 17 · Spring Boot 3.5 (đóng gói war) · JSP/JSTL (Jakarta) · Bootstrap 5 · SiteMesh 3 ·
Spring Data JPA + MySQL · Spring Security + JWT · Spring Mail · WebSocket (STOMP) · Cloudinary

## Yêu cầu

- JDK 17 trở lên
- MySQL 8 (đã tạo sẵn database `starshop`, charset `utf8mb4`)
- Không cần cài Maven: dùng Maven Wrapper (`mvnw` / `mvnw.cmd`) có sẵn trong project

## 1. Chuẩn bị database

Database đã được tạo sẵn. Nếu máy mới chưa có:

```sql
CREATE DATABASE starshop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Bảng được Hibernate tự tạo/cập nhật khi chạy ứng dụng (`ddl-auto: update`).

## 2. Cấu hình

Ứng dụng mặc định chạy với profile `local`. Có hai cách cấu hình kết nối DB, **chọn một**:

### Cách 1 – File `application-local.yml` (khuyên dùng khi dev)

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

Sau đó sửa `username` / `password` trong file vừa tạo. File này đã nằm trong `.gitignore`, **không commit**.

### Cách 2 – Biến môi trường

| Biến | Ý nghĩa | Mặc định |
|------|---------|----------|
| `DB_URL` | JDBC URL | `jdbc:mysql://localhost:3306/starshop?...` |
| `DB_USERNAME` | Tài khoản MySQL | `root` |
| `DB_PASSWORD` | Mật khẩu MySQL | (rỗng) |
| `SPRING_PROFILES_ACTIVE` | Profile đang chạy | `local` |
| `PORT` | Cổng HTTP | `8080` |

Ví dụ (PowerShell):

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_password"
```

Ví dụ (Git Bash / Linux / macOS):

```bash
export DB_USERNAME=root
export DB_PASSWORD=your_password
```

> Giá trị trong `application-local.yml` được ưu tiên hơn giá trị mặc định trong `application.yml`.
> Các cấu hình bí mật khác (JWT, mail, Cloudinary, VNPAY) sẽ được bổ sung vào bảng trên khi làm các chức năng tương ứng.

## 3. Chạy ứng dụng

```bash
# Windows (PowerShell / CMD)
mvnw.cmd spring-boot:run

# Git Bash / Linux / macOS
./mvnw spring-boot:run
```

Mở trình duyệt: <http://localhost:8080> → trang hiển thị **"StarShop is running"**.

Chạy từ IntelliJ: mở class `com.starshop.StarShopApplication` → Run. Nếu JSP báo 404, chỉnh
*Run Configuration → Working directory* thành `$MODULE_WORKING_DIR$`.

### Dữ liệu mẫu (profile `local`)

Lần chạy đầu, `DataSeeder` tự tạo role, 2 chi nhánh, 3 nhà vận chuyển, 1 shop, 6 danh mục, 30 sản phẩm
và các tài khoản dưới đây (chỉ tạo khi bảng `users` còn trống). Mật khẩu chung: **`Starshop@123`**

| Email | Vai trò |
|-------|---------|
| `admin@starshop.vn` | ADMIN |
| `manager@starshop.vn` | MANAGER (chi nhánh StarShop Quận 1) |
| `vendor@starshop.vn` | USER + VENDOR (shop "Hoa Tươi Ánh Sao") |
| `shipper@starshop.vn` | SHIPPER (Giao Hàng Nhanh) |
| `user@starshop.vn` | USER |

Muốn tạo lại dữ liệu mẫu: xóa toàn bộ bảng trong database `starshop` rồi chạy lại ứng dụng.
Thiết kế CSDL: xem [docs/erd.md](docs/erd.md).

### Build file war

```bash
./mvnw clean package
java -jar target/starshop-0.0.1-SNAPSHOT.war
```

## Cấu trúc thư mục

```
src/main/java/com/starshop
├── config/        # Cấu hình (Security, WebSocket, Cloudinary, SiteMesh...)
├── security/      # JWT, UserDetailsService
├── controller/    # web (Guest/User), vendor, manager, admin, shipper, api
├── service/       # Interface nghiệp vụ
│   └── impl/      # Cài đặt nghiệp vụ, @Transactional
├── repository/    # Spring Data JPA
├── entity/        # JPA entity (+ enums/)
├── dto/           # DTO + validation
├── mapper/        # Entity <-> DTO
├── exception/     # Custom exception + @ControllerAdvice
└── util/
src/main/webapp/WEB-INF/views/   # JSP: web, vendor, manager, admin, shipper, auth, decorators, error
src/main/resources/static/       # css, js, img, vendor-template
```
