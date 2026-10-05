# Thanh toán VNPAY (sandbox)

## 1. Lấy tài khoản sandbox

1. Mở https://sandbox.vnpayment.vn/devreg/ và điền form đăng ký merchant test
   (tên website, email nhận thông tin, URL website có thể ghi `http://localhost:8080`).
2. VNPAY gửi email gồm:
   - **vnp_TmnCode** (mã website, 8 ký tự)
   - **vnp_HashSecret** (chuỗi bí mật để ký)
   - Tài khoản đăng nhập trang quản trị merchant: https://sandbox.vnpayment.vn/merchantv2/
3. Thẻ test để thanh toán trên trang sandbox (ngân hàng **NCB**):

   | Trường | Giá trị |
   |---|---|
   | Số thẻ | 9704198526191432198 |
   | Tên chủ thẻ | NGUYEN VAN A |
   | Ngày phát hành | 07/15 |
   | Mật khẩu OTP | 123456 |

   Các thẻ test khác (thẻ hết số dư, thẻ bị khóa...) có trong trang "Thông tin thẻ test" của VNPAY.

## 2. Cấu hình (KHÔNG commit)

Cách 1 – biến môi trường:

```powershell
$env:VNPAY_TMN_CODE = "MA_TMN_CODE"
$env:VNPAY_HASH_SECRET = "CHUOI_HASH_SECRET"
.\mvnw.cmd spring-boot:run
```

Cách 2 – thêm vào `src/main/resources/application-local.yml` (file này đã nằm trong .gitignore):

```yaml
vnpay:
  tmn-code: MA_TMN_CODE
  hash-secret: CHUOI_HASH_SECRET
```

Các giá trị khác có mặc định trong `application.yml`, chỉ đổi khi cần:

| Biến môi trường | Mặc định |
|---|---|
| `VNPAY_PAY_URL` | https://sandbox.vnpayment.vn/paymentv2/vpcpay.html |
| `VNPAY_RETURN_URL` | http://localhost:8080/payment/vnpay/return |
| `VNPAY_EXPIRE_MINUTES` | 15 |

Chưa cấu hình tmnCode / hashSecret thì ở trang checkout, VNPAY hiện "Sắp có" và chỉ đặt được COD.

## 3. Luồng thanh toán

1. Khách chọn VNPAY ở `/checkout` → bấm "Đặt hàng và thanh toán".
2. Server tạo đơn (trạng thái NEW, **giữ hàng**: trừ tồn kho, ghi lượt mã) và một `Payment` VNPAY trạng thái PENDING,
   rồi chuyển khách sang VNPAY bằng URL có chữ ký HMAC-SHA512.
3. VNPAY báo kết quả theo 2 đường, cả hai đều kiểm tra chữ ký, mã giao dịch, số tiền:
   - **Return URL** `/payment/vnpay/return`: trình duyệt của khách quay về.
   - **IPN** `/api/payment/vnpay/ipn`: VNPAY gọi thẳng server (cấu hình ở trang merchant, mục "Cấu hình IPN URL").
     Khi chạy localhost, VNPAY không gọi được IPN, nên Return URL cũng xử lý kết quả. Muốn thử IPN thật
     thì cần URL public (ví dụ ngrok: `ngrok http 8080` rồi khai báo `https://<id>.ngrok-free.app/api/payment/vnpay/ipn`).
4. Mỗi giao dịch chỉ được xử lý **một lần**: dòng `Payment` bị khóa (SELECT ... FOR UPDATE) khi xử lý,
   callback đến sau thấy đã xử lý thì bỏ qua (IPN trả `RspCode=02`).
5. Kết quả:
   - **Thành công** → `Payment` PAID (lưu mã giao dịch VNPAY), đơn giữ trạng thái NEW chờ shop xác nhận.
   - **Thất bại / khách hủy** → `Payment` FAILED / CANCELLED, các đơn chuyển CANCELLED qua `OrderService`
     (hoàn tồn kho, trả lượt mã), sản phẩm được trả lại giỏ hàng, trang kết quả báo lỗi cho khách.
   - **Khách bỏ dở** (đóng trang VNPAY): trong thời hạn, khách bấm "Tiếp tục thanh toán" ở trang kết quả;
     quá hạn + 5 phút, job `PaymentExpiryJob` (chạy mỗi phút) tự hủy như trường hợp thất bại.

## 4. Lưu ý cho các chức năng khác

- Đơn VNPAY chưa thanh toán (`payment.status = PENDING`) vẫn ở trạng thái NEW: màn hình đơn của **vendor** và **shipper**
  chỉ nên hiện đơn COD hoặc đơn đã thanh toán (`payment.status = PAID`).
- Mọi thay đổi trạng thái đơn gọi `OrderService.changeStatus(...)`; chuyển sang CANCELLED tự hoàn tồn kho và lượt mã.
- Đơn đã thanh toán VNPAY mà bị hủy / trả hàng thì cần hoàn tiền (chức năng hoàn tiền / trả hàng xử lý `Payment` REFUNDED).
