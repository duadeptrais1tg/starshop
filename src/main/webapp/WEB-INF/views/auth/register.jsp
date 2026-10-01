<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Đăng ký</title>
</head>
<body>
<div class="card card-md">
    <div class="card-body">
        <h2 class="h2 text-center mb-2">Tạo tài khoản</h2>
        <p class="text-secondary text-center mb-4">Mã kích hoạt sẽ được gửi tới email của bạn.</p>

        <c:if test="${not empty error}">
            <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
        </c:if>

        <%-- form:form tự thêm input ẩn _csrf --%>
        <form:form modelAttribute="form" method="post" action="${pageContext.request.contextPath}/auth/register"
                   novalidate="novalidate" autocomplete="on">
            <div class="mb-3">
                <label class="form-label required" for="fullName">Họ và tên</label>
                <form:input path="fullName" id="fullName" cssClass="form-control" cssErrorClass="form-control is-invalid"
                            maxlength="100" autocomplete="name" placeholder="Nguyễn Văn A" required="required"/>
                <form:errors path="fullName" cssClass="invalid-feedback"/>
            </div>
            <div class="mb-3">
                <label class="form-label required" for="email">Email</label>
                <form:input path="email" id="email" type="email" cssClass="form-control" cssErrorClass="form-control is-invalid"
                            maxlength="150" autocomplete="email" placeholder="ban@email.com" required="required"/>
                <form:errors path="email" cssClass="invalid-feedback"/>
            </div>
            <div class="mb-3">
                <label class="form-label required" for="phone">Số điện thoại</label>
                <form:input path="phone" id="phone" type="tel" cssClass="form-control" cssErrorClass="form-control is-invalid"
                            maxlength="12" autocomplete="tel" placeholder="0912345678" required="required"/>
                <form:errors path="phone" cssClass="invalid-feedback"/>
            </div>
            <div class="mb-3">
                <label class="form-label required" for="password">Mật khẩu</label>
                <form:password path="password" id="password" cssClass="form-control" cssErrorClass="form-control is-invalid"
                               maxlength="100" autocomplete="new-password" required="required"/>
                <form:errors path="password" cssClass="invalid-feedback"/>
                <small class="form-hint">Tối thiểu 8 ký tự, gồm cả chữ và số.</small>
            </div>
            <div class="mb-3">
                <label class="form-label required" for="confirmPassword">Nhập lại mật khẩu</label>
                <form:password path="confirmPassword" id="confirmPassword" cssClass="form-control"
                               cssErrorClass="form-control is-invalid" maxlength="100" autocomplete="new-password"
                               required="required"/>
                <form:errors path="confirmPassword" cssClass="invalid-feedback"/>
            </div>
            <div class="form-footer">
                <button type="submit" class="btn btn-primary w-100">
                    <i class="ti ti-user-plus me-1"></i> Đăng ký
                </button>
            </div>
        </form:form>
    </div>
</div>
<div class="text-center text-secondary mt-3">
    Đã có tài khoản? <a href="<c:url value='/auth/login'/>">Đăng nhập</a>
</div>
</body>
</html>
