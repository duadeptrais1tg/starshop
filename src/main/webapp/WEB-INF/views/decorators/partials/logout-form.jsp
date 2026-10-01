<%@ page pageEncoding="UTF-8" %>
<%-- Nút đăng xuất dạng form POST (kèm CSRF), dùng trong dropdown tài khoản --%>
<form method="post" action="<c:url value='/auth/logout'/>" class="m-0">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
    <button type="submit" class="dropdown-item text-danger"><i class="ti ti-logout me-2"></i>Đăng xuất</button>
</form>
