<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Lỗi thanh toán</title>
</head>
<body>
<div class="row justify-content-center">
    <div class="col-lg-6">
        <div class="card">
            <div class="card-body text-center py-5">
                <div class="mb-3"><span class="avatar avatar-xl bg-danger-lt text-danger rounded-circle"><i class="ti ti-alert-triangle fs-1"></i></span></div>
                <h1 class="mb-2">Không xử lý được thanh toán</h1>
                <p class="text-secondary mb-0"><c:out value="${error}"/></p>
            </div>
            <div class="card-footer d-flex justify-content-center gap-2">
                <a href="<c:url value='/cart'/>" class="btn btn-primary"><i class="ti ti-shopping-cart me-1"></i>Giỏ hàng</a>
                <a href="<c:url value='/'/>" class="btn">Trang chủ</a>
            </div>
        </div>
    </div>
</div>
</body>
</html>
