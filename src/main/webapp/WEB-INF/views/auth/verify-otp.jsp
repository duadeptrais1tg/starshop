<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Xác thực email</title>
</head>
<body>
<div class="card card-md">
    <div class="card-body">
        <div class="text-center mb-3"><i class="ti ti-mail-check text-primary" style="font-size:3rem"></i></div>
        <h2 class="h2 text-center mb-2">Xác thực email</h2>
        <p class="text-secondary text-center mb-4">
            Nhập mã 6 số đã gửi tới <strong><c:out value="${email}"/></strong>.<br>
            Mã có hiệu lực trong 5 phút.
        </p>

        <c:if test="${not empty message}">
            <div class="alert alert-success" role="alert"><c:out value="${message}"/></div>
        </c:if>
        <c:if test="${not empty error}">
            <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
        </c:if>

        <form method="post" action="<c:url value='/auth/verify-otp'/>" autocomplete="off">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <input type="hidden" name="email" value="<c:out value='${email}'/>">
            <div class="mb-3">
                <label class="form-label" for="code">Mã OTP</label>
                <input type="text" id="code" name="code" class="form-control form-control-lg text-center fw-bold"
                       style="letter-spacing:.5em" inputmode="numeric" pattern="\d{6}" maxlength="6"
                       autocomplete="one-time-code" placeholder="••••••" required autofocus>
            </div>
            <button type="submit" class="btn btn-primary w-100">
                <i class="ti ti-check me-1"></i> Kích hoạt tài khoản
            </button>
        </form>

        <form method="post" action="<c:url value='/auth/resend-otp'/>" class="text-center mt-3">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <input type="hidden" name="email" value="<c:out value='${email}'/>">
            <span class="text-secondary">Chưa nhận được mã?</span>
            <button type="submit" id="resend-btn" class="btn btn-link p-0 align-baseline"
                    data-wait="${resendWaitSeconds}">Gửi lại mã</button>
        </form>
    </div>
</div>
<div class="text-center text-secondary mt-3">
    Sai email? <a href="<c:url value='/auth/register'/>">Đăng ký lại</a>
</div>

<script>
    // Khóa nút "Gửi lại mã" và đếm ngược theo số giây server trả về (server vẫn kiểm tra lại 60 giây)
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
