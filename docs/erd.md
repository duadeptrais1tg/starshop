# StarShop – Thiết kế cơ sở dữ liệu

## 1. Nguyên tắc chung

- Mọi entity kế thừa `BaseEntity` (`@MappedSuperclass`): `id` (BIGINT, auto increment), `created_at`, `updated_at`
  (tự điền bằng `@CreationTimestamp` / `@UpdateTimestamp` của Hibernate).
- Tên bảng/cột tiếng Anh, dạng `snake_case`. Bảng đơn hàng đặt tên `orders` (vì `ORDER` là từ khóa SQL).
- Tiền: `BigDecimal` ↔ `DECIMAL(15,2)`. Tỉ lệ %: `DECIMAL(5,2)`.
- Enum lưu dạng chuỗi (`@Enumerated(EnumType.STRING)`) để không lệch dữ liệu khi thêm giá trị mới.
- Không xóa cứng dữ liệu đã phát sinh giao dịch (sản phẩm, shop, danh mục…): dùng cờ `active` / trạng thái.
- **Snapshot trong đơn hàng:** tên, ảnh, giá sản phẩm, địa chỉ nhận hàng, phí ship, % chiết khấu được **chép vào đơn**
  lúc đặt, để sau này sản phẩm đổi giá hoặc user sửa địa chỉ thì đơn cũ vẫn đúng.
- Một số cột thống kê được **lưu sẵn** trên `products` (`sold_count`, `rating_avg`, `review_count`, `favorite_count`)
  để sắp xếp "bán chạy / đánh giá cao / yêu thích nhất" nhanh, cập nhật trong service khi có đơn giao thành công,
  đánh giá mới, bấm thích.

### Quyết định của nhóm

| Vấn đề | Quyết định |
|---|---|
| Store ↔ Shop | Mỗi Shop thuộc 1 Store; Admin gán Store khi duyệt shop (`store_id` NULL khi còn chờ duyệt). Manager quản lý dữ liệu của các shop trong Store của mình |
| Vendor ↔ Shop | Mỗi vendor có đúng 1 shop (`shops.owner_id` UNIQUE) |
| User hủy đơn | Chỉ khi đơn còn `NEW`. Đã `CONFIRMED` thì liên hệ shop |
| Carrier | Dùng chung toàn chuỗi (không gắn Store). Admin quản lý, Manager chỉ xem |

## 2. Enum

| Enum | Giá trị |
|------|---------|
| `RoleName` | `USER`, `VENDOR`, `MANAGER`, `ADMIN`, `SHIPPER` (Guest = chưa đăng nhập, không lưu) |
| `OrderStatus` | `NEW`, `CONFIRMED`, `PICKED_UP`, `SHIPPING`, `DELIVERED`, `CANCELLED`, `RETURN_REQUESTED`, `REFUNDED` |
| `PaymentMethod` | `COD`, `VNPAY`, `MOMO` |
| `PaymentStatus` | `PENDING`, `PAID`, `FAILED`, `CANCELLED`, `REFUNDED` |
| `PromotionType` | `PRODUCT_PERCENT` (giảm % giá sản phẩm), `SHIPPING_DISCOUNT` (giảm phí vận chuyển) |
| `PromotionScope` | `PLATFORM` (toàn sàn), `CATEGORY` (theo danh mục), `SHOP` (theo shop) |
| `ShopStatus` | `PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED` |
| `OtpType` | `REGISTER`, `RESET_PASSWORD` |
| `MediaType` | `IMAGE`, `VIDEO` |
| `AssignmentStatus` | `ASSIGNED`, `DELIVERING`, `DELIVERED`, `FAILED` |
| `ReturnStatus` | `PENDING`, `APPROVED`, `REJECTED` |
| `NotificationType` | `ORDER`, `SHOP`, `PROMOTION`, `SYSTEM` |

Luồng trạng thái đơn hàng:
`NEW → CONFIRMED → PICKED_UP → SHIPPING → DELIVERED`;
nhánh phụ `NEW/CONFIRMED → CANCELLED`, `DELIVERED → RETURN_REQUESTED → REFUNDED`.

## 3. Danh sách entity

### Nhóm người dùng & xác thực

| Entity (bảng) | Cột chính | Quan hệ / ràng buộc |
|---|---|---|
| **User** (`users`) | `full_name`, `email` (UK), `phone`, `password` (BCrypt), `avatar_url`, `avatar_public_id`, `enabled` (đã kích hoạt OTP), `locked` | N-N `Role` qua `user_roles`; N-1 `Store` (chỉ MANAGER); N-1 `Carrier` (chỉ SHIPPER) |
| **Role** (`roles`) | `name` (`RoleName`, UK) | |
| **OtpToken** (`otp_tokens`) | `code` (6 số), `type` (`OtpType`), `expires_at`, `failed_attempts`, `used` | N-1 `User`. Gửi lại sau 60s dựa vào `created_at` |
| **Address** (`addresses`) | `receiver_name`, `phone`, `province`, `district`, `ward`, `detail`, `is_default` | N-1 `User` |

### Nhóm chuỗi cửa hàng & shop

| Entity (bảng) | Cột chính | Quan hệ / ràng buộc |
|---|---|---|
| **Store** (`stores`) | `name` (UK), `address`, `phone`, `active` | Chi nhánh của chuỗi. 1-N `User` (manager), 1-N `Shop` |
| **Shop** (`shops`) | `name` (UK), `slug` (UK), `description`, `logo_url`, `banner_url`, `pickup_address`, `phone`, `status` (`ShopStatus`), `status_reason` | 1-1 `User` (owner, vendor); N-1 `Store` |
| **ShopCommission** (`shop_commissions`) | `rate` (0–100 %), `effective_from`, `effective_to` | N-1 `Shop`, **`shop_id` = NULL là mức mặc định toàn sàn** |
| **Carrier** (`carriers`) | `name` (UK), `shipping_fee` (≥ 0), `active` | 1-N `User` (shipper) |

### Nhóm sản phẩm

| Entity (bảng) | Cột chính | Quan hệ / ràng buộc |
|---|---|---|
| **Category** (`categories`) | `name` (UK), `slug` (UK), `image_url`, `active` | N-1 `Category` (cha, tùy chọn) |
| **Product** (`products`) | `name`, `slug` (UK), `description` (TEXT), `price` (> 0), `original_price`, `stock` (≥ 0), `active`, `sold_count`, `rating_avg`, `review_count`, `favorite_count` | N-1 `Shop`, N-1 `Category` |
| **ProductImage** (`product_images`) | `url`, `public_id` (Cloudinary), `thumbnail`, `sort_order` | N-1 `Product` |
| **Favorite** (`favorites`) | | N-1 `User`, N-1 `Product`, UK (`user_id`, `product_id`) |
| **ViewedProduct** (`viewed_products`) | `viewed_at` | N-1 `User`, N-1 `Product`, UK (`user_id`, `product_id`) — xem lại thì cập nhật `viewed_at` |

### Nhóm giỏ hàng & đơn hàng

| Entity (bảng) | Cột chính | Quan hệ / ràng buộc |
|---|---|---|
| **Cart** (`carts`) | | 1-1 `User` |
| **CartItem** (`cart_items`) | `quantity` (≥ 1) | N-1 `Cart`, N-1 `Product`, UK (`cart_id`, `product_id`) |
| **Order** (`orders`) | `code` (UK, mã đơn hiển thị), `status`, `payment_method`, snapshot địa chỉ (`receiver_name`, `receiver_phone`, `shipping_address`), `subtotal`, `product_discount`, `shipping_fee`, `shipping_discount`, `total`, `commission_rate` (snapshot), `note`, `cancel_reason`, `delivered_at` | N-1 `User` (người mua), N-1 `Shop` (**mỗi đơn thuộc 1 shop** — checkout tự tách theo shop), N-1 `Carrier`, N-1 `Payment`, N-1 `Coupon` (tùy chọn) |
| **OrderItem** (`order_items`) | snapshot `product_name`, `product_image`, `unit_price`, `quantity`, `line_total`, `reviewed` | N-1 `Order`, N-1 `Product` |
| **OrderStatusHistory** (`order_status_histories`) *(đề xuất thêm)* | `from_status`, `to_status`, `note` | N-1 `Order`, N-1 `User` (người đổi). Phục vụ "lịch sử trạng thái" ở trang chi tiết đơn |
| **ReturnRequest** (`return_requests`) *(đề xuất thêm)* | `reason`, `status` (`ReturnStatus`), `reject_reason`; ảnh lưu bảng phụ `return_request_images` | 1-1 `Order` |
| **Payment** (`payments`) | `method`, `status`, `amount`, `txn_ref` (UK, mã gửi cổng thanh toán), `transaction_no` (mã của VNPAY/MOMO), `paid_at` | 1-N `Order`: một lần thanh toán online trả cho **nhiều đơn** tách từ cùng một checkout; COD thì mỗi đơn một payment |
| **ShipperAssignment** (`shipper_assignments`) | `status` (`AssignmentStatus`), `fail_reason`, `cod_collected`, `collected_at`, `delivered_at` | N-1 `Order`, N-1 `User` (shipper), N-1 `User` (người phân công). Một đơn có thể phân công lại khi giao thất bại |

### Nhóm khuyến mãi

| Entity (bảng) | Cột chính | Quan hệ / ràng buộc |
|---|---|---|
| **Promotion** (`promotions`) | `name`, `description`, `type` (`PromotionType`), `scope` (`PromotionScope`), `discount_value` (% hoặc số tiền giảm ship), `max_discount`, `min_order_value`, `start_at`, `end_at` (> start), `active` | N-1 `Shop` (khi scope = SHOP), N-1 `Category` (khi scope = CATEGORY), N-1 `User` (người tạo) |
| **Coupon** (`coupons`) | `code` (UK), `usage_limit`, `used_count`, `per_user_limit`, `active` | N-1 `Promotion`. Promotion **không có coupon** = tự áp dụng (hiển thị nhãn giảm giá); **có coupon** = phải nhập mã |
| **CouponUsage** (`coupon_usages`) | `discount_amount` | N-1 `Coupon`, N-1 `User`, N-1 `Order`. Hủy đơn → xóa bản ghi và giảm `used_count` |

### Nhóm đánh giá & thông báo

| Entity (bảng) | Cột chính | Quan hệ / ràng buộc |
|---|---|---|
| **Review** (`reviews`) | `rating` (1–5), `content` (TEXT, ≥ 50 ký tự) | N-1 `User`, N-1 `Product`, 1-1 `OrderItem` (UK — mỗi sản phẩm trong mỗi đơn đánh giá 1 lần) |
| **ReviewMedia** (`review_media`) | `url`, `public_id`, `media_type` (`MediaType`) | N-1 `Review` (tối đa 5 ảnh + 1 video, kiểm tra ở service) |
| **Notification** (`notifications`) | `title`, `content`, `link`, `type` (`NotificationType`), `is_read` | N-1 `User` |

## 4. ERD

```mermaid
erDiagram
    USERS }o--o{ ROLES : "user_roles"
    STORES ||--o{ USERS : "manager"
    CARRIERS ||--o{ USERS : "shipper"
    USERS ||--o{ OTP_TOKENS : has
    USERS ||--o{ ADDRESSES : has
    USERS ||--o| SHOPS : owns
    STORES ||--o{ SHOPS : contains
    SHOPS ||--o{ SHOP_COMMISSIONS : has
    CATEGORIES ||--o{ CATEGORIES : parent
    SHOPS ||--o{ PRODUCTS : sells
    CATEGORIES ||--o{ PRODUCTS : groups
    PRODUCTS ||--o{ PRODUCT_IMAGES : has
    USERS ||--o{ FAVORITES : likes
    PRODUCTS ||--o{ FAVORITES : "liked by"
    USERS ||--o{ VIEWED_PRODUCTS : views
    PRODUCTS ||--o{ VIEWED_PRODUCTS : "viewed by"
    USERS ||--|| CARTS : has
    CARTS ||--o{ CART_ITEMS : contains
    PRODUCTS ||--o{ CART_ITEMS : "in"
    USERS ||--o{ ORDERS : places
    SHOPS ||--o{ ORDERS : receives
    CARRIERS ||--o{ ORDERS : ships
    PAYMENTS ||--|{ ORDERS : pays
    COUPONS |o--o{ ORDERS : "applied to"
    ORDERS ||--|{ ORDER_ITEMS : contains
    PRODUCTS ||--o{ ORDER_ITEMS : "ordered as"
    ORDERS ||--o{ ORDER_STATUS_HISTORIES : logs
    ORDERS ||--o| RETURN_REQUESTS : has
    ORDERS ||--o{ SHIPPER_ASSIGNMENTS : "assigned via"
    USERS ||--o{ SHIPPER_ASSIGNMENTS : delivers
    SHOPS |o--o{ PROMOTIONS : "shop scope"
    CATEGORIES |o--o{ PROMOTIONS : "category scope"
    PROMOTIONS ||--o{ COUPONS : has
    COUPONS ||--o{ COUPON_USAGES : used
    USERS ||--o{ COUPON_USAGES : uses
    ORDERS ||--o| COUPON_USAGES : records
    ORDER_ITEMS ||--o| REVIEWS : reviewed
    USERS ||--o{ REVIEWS : writes
    PRODUCTS ||--o{ REVIEWS : receives
    REVIEWS ||--o{ REVIEW_MEDIA : has
    USERS ||--o{ NOTIFICATIONS : receives

    USERS {
        bigint id PK
        varchar full_name
        varchar email UK
        varchar phone
        varchar password
        varchar avatar_url
        varchar avatar_public_id
        boolean enabled
        boolean locked
        bigint store_id FK
        bigint carrier_id FK
    }
    ROLES {
        bigint id PK
        varchar name UK
    }
    OTP_TOKENS {
        bigint id PK
        bigint user_id FK
        varchar code
        varchar type
        datetime expires_at
        int failed_attempts
        boolean used
    }
    ADDRESSES {
        bigint id PK
        bigint user_id FK
        varchar receiver_name
        varchar phone
        varchar province
        varchar district
        varchar ward
        varchar detail
        boolean is_default
    }
    STORES {
        bigint id PK
        varchar name UK
        varchar address
        varchar phone
        boolean active
    }
    SHOPS {
        bigint id PK
        bigint owner_id FK "UK"
        bigint store_id FK
        varchar name UK
        varchar slug UK
        text description
        varchar logo_url
        varchar banner_url
        varchar pickup_address
        varchar phone
        varchar status
        varchar status_reason
    }
    SHOP_COMMISSIONS {
        bigint id PK
        bigint shop_id FK "NULL = mac dinh"
        decimal rate
        date effective_from
        date effective_to
    }
    CARRIERS {
        bigint id PK
        varchar name UK
        decimal shipping_fee
        boolean active
    }
    CATEGORIES {
        bigint id PK
        bigint parent_id FK
        varchar name UK
        varchar slug UK
        varchar image_url
        boolean active
    }
    PRODUCTS {
        bigint id PK
        bigint shop_id FK
        bigint category_id FK
        varchar name
        varchar slug UK
        text description
        decimal price
        decimal original_price
        int stock
        boolean active
        int sold_count
        decimal rating_avg
        int review_count
        int favorite_count
    }
    PRODUCT_IMAGES {
        bigint id PK
        bigint product_id FK
        varchar url
        varchar public_id
        boolean thumbnail
        int sort_order
    }
    FAVORITES {
        bigint id PK
        bigint user_id FK
        bigint product_id FK
    }
    VIEWED_PRODUCTS {
        bigint id PK
        bigint user_id FK
        bigint product_id FK
        datetime viewed_at
    }
    CARTS {
        bigint id PK
        bigint user_id FK "UK"
    }
    CART_ITEMS {
        bigint id PK
        bigint cart_id FK
        bigint product_id FK
        int quantity
    }
    ORDERS {
        bigint id PK
        varchar code UK
        bigint user_id FK
        bigint shop_id FK
        bigint carrier_id FK
        bigint payment_id FK
        bigint coupon_id FK
        varchar status
        varchar payment_method
        varchar receiver_name
        varchar receiver_phone
        varchar shipping_address
        decimal subtotal
        decimal product_discount
        decimal shipping_fee
        decimal shipping_discount
        decimal total
        decimal commission_rate
        varchar note
        varchar cancel_reason
        datetime delivered_at
    }
    ORDER_ITEMS {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        varchar product_name
        varchar product_image
        decimal unit_price
        int quantity
        decimal line_total
        boolean reviewed
    }
    ORDER_STATUS_HISTORIES {
        bigint id PK
        bigint order_id FK
        bigint changed_by FK
        varchar from_status
        varchar to_status
        varchar note
    }
    RETURN_REQUESTS {
        bigint id PK
        bigint order_id FK "UK"
        varchar reason
        varchar status
        varchar reject_reason
    }
    PAYMENTS {
        bigint id PK
        varchar method
        varchar status
        decimal amount
        varchar txn_ref UK
        varchar transaction_no
        datetime paid_at
    }
    SHIPPER_ASSIGNMENTS {
        bigint id PK
        bigint order_id FK
        bigint shipper_id FK
        bigint assigned_by FK
        varchar status
        varchar fail_reason
        boolean cod_collected
        datetime collected_at
        datetime delivered_at
    }
    PROMOTIONS {
        bigint id PK
        bigint shop_id FK
        bigint category_id FK
        bigint created_by FK
        varchar name
        varchar type
        varchar scope
        decimal discount_value
        decimal max_discount
        decimal min_order_value
        datetime start_at
        datetime end_at
        boolean active
    }
    COUPONS {
        bigint id PK
        bigint promotion_id FK
        varchar code UK
        int usage_limit
        int used_count
        int per_user_limit
        boolean active
    }
    COUPON_USAGES {
        bigint id PK
        bigint coupon_id FK
        bigint user_id FK
        bigint order_id FK
        decimal discount_amount
    }
    REVIEWS {
        bigint id PK
        bigint user_id FK
        bigint product_id FK
        bigint order_item_id FK "UK"
        int rating
        text content
    }
    REVIEW_MEDIA {
        bigint id PK
        bigint review_id FK
        varchar url
        varchar public_id
        varchar media_type
    }
    NOTIFICATIONS {
        bigint id PK
        bigint user_id FK
        varchar title
        varchar content
        varchar link
        varchar type
        boolean is_read
    }
```

> Các bảng đều có thêm `created_at`, `updated_at` (từ `BaseEntity`), không vẽ lại trong sơ đồ cho gọn.
