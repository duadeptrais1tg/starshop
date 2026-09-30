<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Đã có lỗi xảy ra</title>
</head>
<body>
<div class="empty py-5">
    <div class="empty-header ss-error-code">500</div>
    <div class="empty-icon mb-2"><i class="ti ti-alert-triangle fs-1 text-secondary"></i></div>
    <p class="empty-title">Đã có lỗi xảy ra</p>
    <p class="empty-subtitle text-secondary">Hệ thống đang gặp sự cố. Vui lòng thử lại sau ít phút.</p>
    <div class="empty-action">
        <a href="<c:url value='/'/>" class="btn btn-primary"><i class="ti ti-home me-1"></i> Về trang chủ</a>
    </div>
</div>
</body>
</html>
