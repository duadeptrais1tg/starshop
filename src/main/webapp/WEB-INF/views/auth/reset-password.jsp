<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Đặt lại mật khẩu</title>
</head>
<body>
<div class="card card-md">
    <div class="card-body">
        <h2 class="h2 text-center mb-2">Đặt lại mật khẩu</h2>
        <p class="text-secondary text-center mb-4">
            Nhập mã OTP gửi tới <strong><c:out value="${form.email}"/></strong> và mật khẩu mới.
        </p>

        <c:if test="${not empty message}">
            <div class="alert alert-success" role="alert"><c:out value="${message}"/></div>
        </c:if>
        <c:if test="${not empty error}">
            <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
        </c:if>

        <form:form modelAttribute="form" method="post" action="${pageContext.request.contextPath}/auth/reset-password"
                   novalidate="novalidate" autocomplete="off">
            <form:hidden path="email"/>
            <form:errors path="email" cssClass="text-danger small d-block mb-2"/>
            <div class="mb-3">
                <label class="form-label required" for="code">Mã OTP</label>
                <form:input path="code" id="code" cssClass="form-control text-center fw-bold"
                            cssErrorClass="form-control text-center fw-bold is-invalid" maxlength="6"
                            inputmode="numeric" autocomplete="one-time-code" placeholder="••••••" style="letter-spacing:.5em"/>
                <form:errors path="code" cssClass="invalid-feedback"/>
            </div>
            <div class="mb-3">
                <label class="form-label required" for="password">Mật khẩu mới</label>
                <form:password path="password" id="password" cssClass="form-control" cssErrorClass="form-control is-invalid"
                               maxlength="100" autocomplete="new-password"/>
                <form:errors path="password" cssClass="invalid-feedback"/>
                <small class="form-hint">Tối thiểu 8 ký tự, gồm cả chữ và số.</small>
            </div>
            <div class="mb-3">
                <label class="form-label required" for="confirmPassword">Nhập lại mật khẩu mới</label>
                <form:password path="confirmPassword" id="confirmPassword" cssClass="form-control"
                               cssErrorClass="form-control is-invalid" maxlength="100" autocomplete="new-password"/>
                <form:errors path="confirmPassword" cssClass="invalid-feedback"/>
            </div>
            <button type="submit" class="btn btn-primary w-100">
                <i class="ti ti-key me-1"></i> Đổi mật khẩu
            </button>
        </form:form>

        <form method="post" action="<c:url value='/auth/forgot-password'/>" class="text-center mt-3">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <input type="hidden" name="email" value="<c:out value='${form.email}'/>">
            <span class="text-secondary">Chưa nhận được mã?</span>
            <button type="submit" id="resend-btn" class="btn btn-link p-0 align-baseline"
                    data-wait="${empty resendWaitSeconds ? 0 : resendWaitSeconds}">Gửi lại mã</button>
        </form>
    </div>
</div>
<div class="text-center text-secondary mt-3">
    <a href="<c:url value='/auth/login'/>">Quay lại đăng nhập</a>
</div>

<script>
    // Đếm ngược nút "Gửi lại mã" (server vẫn tự giới hạn 60 giây)
    (function () {
        var btn = document.getElementById('resend-btn');
        var seconds = parseInt(btn.getAttribute('data-wait'), 10) || 0;
        var label = btn.textContent;
        function tick() {
            if (seconds > 0) {
                btn.disabled = true;
                btn.textContent = label + ' (' + seconds + 's)';
                seconds--;
                setTimeout(tick, 1000);
            } else {
                btn.disabled = false;
                btn.textContent = label;
            }
        }
        tick();
    })();
</script>
</body>
</html>
