<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Trang chủ</title>
</head>
<body>
<%-- Nội dung trang chủ sẽ được xây dựng ở chức năng trang chủ Guest (A4) --%>
<section class="ss-hero mb-4">
    <h1 class="display-6 fw-bold mb-2">Hoa tươi cho mọi khoảnh khắc</h1>
    <p class="fs-3 mb-4 opacity-75">Chuỗi cửa hàng hoa StarShop – giao nhanh trong 2 giờ.</p>
    <a href="<c:url value='/products/search'/>" class="btn btn-light btn-lg">
        <i class="ti ti-flower me-1"></i> Xem sản phẩm
    </a>
</section>
</body>
</html>
