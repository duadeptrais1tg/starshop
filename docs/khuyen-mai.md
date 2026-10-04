# Khuyến mãi & mã giảm giá

Tài liệu thống nhất cách tính khuyến mãi giữa phần **quản lý khuyến mãi** (B4, B8) và phần **đặt hàng** (A8, A9, A10).
Toàn bộ nghiệp vụ nằm trong `PromotionService`; phần đặt hàng chỉ gọi service, không tự tính.

## 1. Khái niệm

| Thuộc tính | Ý nghĩa |
|---|---|
| Loại `PRODUCT_PERCENT` | Giảm `discountValue` % trên tiền hàng, tối đa `maxDiscount` ₫ (nếu có) |
| Loại `SHIPPING_DISCOUNT` | Giảm `discountValue` ₫ trên phí vận chuyển, không vượt quá phí ship |
| Phạm vi `PLATFORM` | Mọi sản phẩm (Admin tạo) |
| Phạm vi `CATEGORY` | Sản phẩm thuộc danh mục, **kể cả danh mục con** (Admin tạo) |
| Phạm vi `SHOP` | Sản phẩm của một shop (Vendor tạo – B8) |
| `minOrderValue` | Số tiền hàng tối thiểu **thuộc phạm vi** để được áp dụng |
| Không có `Coupon` | **Tự áp dụng**, không giới hạn lượt |
| Có `Coupon` | Khách phải nhập mã; có `usageLimit` (tổng lượt) và `perUserLimit` (mỗi người) |

Khi chương trình **đã có người dùng** (mã đã được dùng, hoặc khuyến mãi tự áp dụng đã bắt đầu) thì không sửa được
loại, giá trị, phạm vi, đơn tối thiểu, thời gian bắt đầu, mã – và không xóa được (chỉ tắt).

## 2. Hiển thị giá trên trang sản phẩm

```java
AutoPricing pricing = promotionService.autoPricing();          // 1 lần cho cả trang
BigDecimal sale = pricing.salePrice(shopId, categoryId, price);  // null = không có khuyến mãi
```

Chỉ tính khuyến mãi **giảm % tự áp dụng, không có đơn tối thiểu**, chọn mức giảm có lợi nhất cho khách.
`ProductCatalogService` và `ViewedProductService` đã điền sẵn vào `ProductCardDto.salePrice`.

## 3. Đặt hàng (A8) – mỗi đơn của MỘT shop

```java
// 1) Khách nhập mã ở trang checkout -> kiểm tra & tính tiền giảm (không ghi dữ liệu)
List<CartLine> lines = ...;   // (categoryId, đơn giá x số lượng) – giá tính lại ở server
CouponDiscount d = promotionService.validateCoupon(code, userId, shopId, lines, shippingFee);
// d.productDiscount(), d.shippingDiscount(), d.couponId()
// Mã không dùng được -> BusinessException với lý do tiếng Việt (hết hạn, hết lượt, chưa đủ đơn tối thiểu...)

// 2) Trong transaction tạo đơn, SAU KHI đã lưu Order (có id):
promotionService.recordUsage(d.couponId(), userId, order.getId(), d.total());
// -> tăng lượt bằng 1 câu UPDATE có điều kiện; 2 người tranh lượt cuối thì 1 người nhận BusinessException
//    "Mã giảm giá vừa hết lượt" và transaction tạo đơn phải rollback.

// 3) Hủy đơn / thanh toán thất bại (A9, A10):
promotionService.releaseUsage(order.getId());   // trả lại lượt, xóa CouponUsage
```

Lưu ý cho A8:
- Gọi lại `validateCoupon` ngay trong transaction tạo đơn (không tin kết quả lúc khách bấm "Áp dụng" vì có thể đã hết lượt).
- Lưu `order.coupon`, `order.productDiscount`, `order.shippingDiscount` theo kết quả trả về.
- Mỗi đơn (mỗi shop) dùng tối đa 1 mã.

## 4. Trang checkout (đã làm ở A8)

```java
// Danh sách mã dùng được cho đơn của một shop (mã của shop + theo danh mục + toàn sàn), giảm nhiều nhất trước
List<CouponOption> options = promotionService.availableCoupons(userId, shopId, lines, shippingFee);

// Khuyến mãi tự áp dụng cấp đơn (không cần mã): giảm % có đơn tối thiểu, giảm phí vận chuyển
OrderAutoDiscount auto = promotionService.autoOrderDiscount(shopId, lines, shippingFee);
```

- Một đơn có thể vừa được khuyến mãi tự áp dụng vừa dùng mã: tổng giảm tiền hàng không vượt tiền hàng,
  tổng giảm phí ship không vượt phí ship. `order.productDiscount` / `order.shippingDiscount` lưu tổng sau khi giới hạn.
- `validateCoupon` không đánh dấu rollback transaction của bên gọi khi mã không hợp lệ (`noRollbackFor`),
  nên có thể gọi trong transaction và bắt `BusinessException` để hiện lý do.
