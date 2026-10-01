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
| `JWT_SECRET` | Khóa ký JWT, chuỗi ngẫu nhiên ≥ 32 ký tự (**bắt buộc**) | (không có) |
| `JWT_EXPIRATION` | Thời hạn đăng nhập, ví dụ `8h`, `30m` | `8h` |
| `JWT_COOKIE_SECURE` | `true` khi chạy HTTPS | `false` |
| `MAIL_USERNAME` | Tài khoản SMTP (Gmail) gửi email OTP | (rỗng) |
| `MAIL_PASSWORD` | Mật khẩu ứng dụng Gmail (không phải mật khẩu đăng nhập) | (rỗng) |
| `MAIL_HOST` / `MAIL_PORT` | Máy chủ SMTP | `smtp.gmail.com` / `587` |
| `MAIL_DEV_LOG_OTP` | `true`: chưa có SMTP thì in OTP ra console (chỉ dùng khi dev) | `false` |
| `CLOUDINARY_CLOUD_NAME` | Cloud name (Cloudinary Dashboard) | (rỗng) |
| `CLOUDINARY_API_KEY` | API key Cloudinary | (rỗng) |
| `CLOUDINARY_API_SECRET` | API secret Cloudinary | (rỗng) |
| `CLOUDINARY_FOLDER` | Thư mục gốc chứa file upload | `starshop` |

> Chưa cấu hình Cloudinary thì ứng dụng vẫn chạy, chỉ chức năng upload ảnh/video báo lỗi.

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
> Các cấu hình bí mật khác (VNPAY, MOMO) sẽ được bổ sung vào bảng trên khi làm các chức năng tương ứng.

## 3. Chạy ứng dụng

```bash
# Windows (PowerShell / CMD)
mvnw.cmd spring-boot:run

# Git Bash / Linux / macOS
./mvnw spring-boot:run
```

Mở trình duyệt: <http://localhost:8080> → trang chủ StarShop.

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

## Quy ước viết JSP

- Dòng đầu mỗi trang: `<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>`
  (`pageEncoding` để không lỗi tiếng Việt; `session="false"` vì ứng dụng stateless, đăng nhập bằng JWT).
  File được `<%@ include %>` cũng phải có `<%@ page pageEncoding="UTF-8" %>`.
- Mọi form `POST` phải có CSRF token:
  `<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">`
- AJAX thay đổi dữ liệu: `fetch(url, {method: 'POST', headers: StarShop.csrfHeaders({...}), ...})`.

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
