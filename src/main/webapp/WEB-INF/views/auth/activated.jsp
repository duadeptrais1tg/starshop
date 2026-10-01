<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Kích hoạt thành công</title>
</head>
<body>
<div class="card card-md">
    <div class="card-body text-center py-5">
        <i class="ti ti-circle-check text-success" style="font-size:4rem"></i>
        <h2 class="h2 mt-3 mb-2">Kích hoạt tài khoản thành công!</h2>
        <p class="text-secondary mb-4">Bạn có thể đăng nhập và bắt đầu mua sắm hoa tươi.</p>
        <a href="<c:url value='/auth/login'/>" class="btn btn-primary w-100">
            <i class="ti ti-login me-1"></i> Đăng nhập
        </a>
    </div>
</div>
</body>
</html>
