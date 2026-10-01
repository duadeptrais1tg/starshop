<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Đăng nhập</title>
</head>
<body>
<div class="card card-md">
    <div class="card-body">
        <h2 class="h2 text-center mb-4">Đăng nhập tài khoản</h2>

        <c:if test="${not empty message}">
            <div class="alert alert-success" role="alert"><c:out value="${message}"/></div>
        </c:if>
        <c:if test="${not empty error}">
            <div class="alert alert-danger" role="alert">
                <c:out value="${error}"/>
                <c:if test="${not empty notActivatedEmail}">
                    <c:url var="verifyUrl" value="/auth/verify-otp"><c:param name="email" value="${notActivatedEmail}"/></c:url>
                    <div class="mt-2">
                        <a href="${verifyUrl}" class="btn btn-sm btn-outline-danger">
                            <i class="ti ti-mail-check me-1"></i> Nhập mã OTP / gửi lại mã
                        </a>
                    </div>
                </c:if>
            </div>
        </c:if>

        <%-- form:form tự thêm input ẩn _csrf --%>
        <form:form modelAttribute="form" method="post" action="${pageContext.request.contextPath}/auth/login"
                   novalidate="novalidate">
            <form:hidden path="redirect"/>
            <div class="mb-3">
                <label class="form-label" for="email">Email</label>
                <form:input path="email" id="email" type="email" cssClass="form-control" cssErrorClass="form-control is-invalid"
                            maxlength="150" autocomplete="username" placeholder="ban@email.com" autofocus="autofocus"/>
                <form:errors path="email" cssClass="invalid-feedback"/>
            </div>
            <div class="mb-2">
                <label class="form-label d-flex" for="password">
                    Mật khẩu
                    <a href="<c:url value='/auth/forgot-password'/>" class="ms-auto small">Quên mật khẩu?</a>
                </label>
                <form:password path="password" id="password" cssClass="form-control" cssErrorClass="form-control is-invalid"
                               maxlength="100" autocomplete="current-password"/>
                <form:errors path="password" cssClass="invalid-feedback"/>
            </div>
            <div class="form-footer">
                <button type="submit" class="btn btn-primary w-100">
                    <i class="ti ti-login me-1"></i> Đăng nhập
                </button>
            </div>
        </form:form>
    </div>
</div>
<div class="text-center text-secondary mt-3">
    Chưa có tài khoản? <a href="<c:url value='/auth/register'/>">Đăng ký</a>
</div>
</body>
</html>
