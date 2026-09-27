<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Không có quyền truy cập</title>
</head>
<body>
<div class="empty py-5">
    <div class="empty-header ss-error-code">403</div>
    <div class="empty-icon mb-2"><i class="ti ti-lock fs-1 text-secondary"></i></div>
    <p class="empty-title">Không có quyền truy cập</p>
    <p class="empty-subtitle text-secondary">Tài khoản của bạn không được phép xem trang này. Hãy đăng nhập bằng tài khoản phù hợp.</p>
    <div class="empty-action">
        <a href="<c:url value='/'/>" class="btn btn-primary"><i class="ti ti-home me-1"></i> Về trang chủ</a>
    </div>
</div>
</body>
</html>
