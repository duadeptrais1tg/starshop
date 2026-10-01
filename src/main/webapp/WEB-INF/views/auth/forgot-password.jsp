<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Quên mật khẩu</title>
</head>
<body>
<div class="card card-md">
    <div class="card-body">
        <div class="text-center mb-3"><i class="ti ti-lock-question text-primary" style="font-size:3rem"></i></div>
        <h2 class="h2 text-center mb-2">Quên mật khẩu</h2>
        <p class="text-secondary text-center mb-4">Nhập email đã đăng ký, chúng tôi sẽ gửi mã OTP để đặt lại mật khẩu.</p>

        <c:if test="${not empty error}">
            <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
        </c:if>

        <form method="post" action="<c:url value='/auth/forgot-password'/>">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <div class="mb-3">
                <label class="form-label" for="email">Email</label>
                <input type="email" id="email" name="email" class="form-control" maxlength="150"
                       value="<c:out value='${email}'/>" autocomplete="email" placeholder="ban@email.com" required autofocus>
            </div>
            <button type="submit" class="btn btn-primary w-100">
                <i class="ti ti-send me-1"></i> Gửi mã OTP
            </button>
        </form>
    </div>
</div>
<div class="text-center text-secondary mt-3">
    Nhớ mật khẩu rồi? <a href="<c:url value='/auth/login'/>">Đăng nhập</a>
</div>
</body>
</html>
