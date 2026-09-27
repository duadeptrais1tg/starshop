<%@ page pageEncoding="UTF-8" %>
<%-- Decorator cho trang đăng nhập / đăng ký / OTP / quên mật khẩu: form ở giữa màn hình --%>
<%@ include file="partials/taglibs.jsp" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="partials/head.jsp" %>
</head>
<body class="d-flex flex-column ss-auth-bg">
<div class="page page-center">
    <div class="container container-tight py-4">
        <div class="text-center mb-4">
            <a href="<c:url value='/'/>" class="ss-brand fs-1">
                <i class="ti ti-flower"></i> StarShop
            </a>
        </div>
        <sitemesh:write property="body"/>
        <div class="text-center text-secondary mt-3">
            <a href="<c:url value='/'/>" class="link-secondary"><i class="ti ti-arrow-left"></i> Về trang chủ</a>
        </div>
    </div>
</div>
<%@ include file="partials/scripts.jsp" %>
</body>
</html>
