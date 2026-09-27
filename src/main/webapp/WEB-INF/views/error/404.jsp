<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Không tìm thấy trang</title>
</head>
<body>
<div class="empty py-5">
    <div class="empty-header ss-error-code">404</div>
    <div class="empty-icon mb-2"><i class="ti ti-flower-off fs-1 text-secondary"></i></div>
    <p class="empty-title">Không tìm thấy trang</p>
    <p class="empty-subtitle text-secondary">Trang bạn tìm không tồn tại hoặc đã bị gỡ.</p>
    <c:if test="${not empty path}"><p class="text-secondary small">Đường dẫn: <code><c:out value="${path}"/></code></p></c:if>
    <div class="empty-action">
        <a href="<c:url value='/'/>" class="btn btn-primary"><i class="ti ti-home me-1"></i> Về trang chủ</a>
    </div>
</div>
</body>
</html>
