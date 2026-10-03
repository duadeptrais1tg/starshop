<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Hồ sơ của tôi</title>
</head>
<body>
<div class="row g-3">
    <div class="col-lg-3"><%@ include file="account-nav.jsp" %></div>
    <div class="col-lg-9">
        <c:if test="${not empty message}">
            <div class="alert alert-success" role="alert"><c:out value="${message}"/></div>
        </c:if>

        <%-- Ảnh đại diện --%>
        <div class="card mb-3">
            <div class="card-body d-flex flex-wrap align-items-center gap-3">
                <c:choose>
                    <c:when test="${not empty profile.avatarUrl}">
                        <span class="avatar avatar-xl" style="background-image: url('<c:out value="${profile.avatarUrl}"/>')"></span>
                    </c:when>
                    <c:otherwise>
                        <span class="avatar avatar-xl bg-primary-lt text-primary fs-1">${currentUser.initial}</span>
                    </c:otherwise>
                </c:choose>
                <div class="flex-fill">
                    <h2 class="m-0"><c:out value="${profile.fullName}"/></h2>
                    <div class="text-secondary"><c:out value="${profile.email}"/></div>
                    <div class="mt-1">
                        <c:forEach var="label" items="${profile.roleLabels}"><span class="badge bg-blue-lt me-1">${label}</span></c:forEach>
                        <span class="text-secondary small ms-1">Tham gia ${profile.createdAt}</span>
                    </div>
                </div>
                <div>
                    <form method="post" action="<c:url value='/user/profile/avatar'/>" enctype="multipart/form-data" class="d-flex gap-2">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <input type="file" name="avatar" class="form-control form-control-sm" accept="image/jpeg,image/png,image/webp" required>
                        <button type="submit" class="btn btn-sm btn-primary text-nowrap"><i class="ti ti-upload me-1"></i>Đổi ảnh</button>
                    </form>
                    <c:if test="${not empty profile.avatarUrl}">
                        <form method="post" action="<c:url value='/user/profile/avatar/remove'/>" class="mt-1 text-end"
                              onsubmit="return confirm('Xóa ảnh đại diện?');">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <button type="submit" class="btn btn-link btn-sm text-danger p-0">Xóa ảnh</button>
                        </form>
                    </c:if>
                    <c:if test="${not empty avatarError}"><div class="text-danger small mt-1"><c:out value="${avatarError}"/></div></c:if>
                    <div class="form-hint">JPG, PNG, WEBP, tối đa 5MB.</div>
                </div>
            </div>
        </div>

        <div class="row g-3">
            <%-- Thông tin cá nhân --%>
            <div class="col-md-6">
                <form:form modelAttribute="profileForm" method="post" action="${pageContext.request.contextPath}/user/profile"
                           cssClass="card h-100" novalidate="novalidate">
                    <div class="card-header"><h3 class="card-title">Thông tin cá nhân</h3></div>
                    <div class="card-body">
                        <c:if test="${not empty infoError}"><div class="alert alert-danger"><c:out value="${infoError}"/></div></c:if>
                        <div class="mb-3">
                            <label class="form-label">Email (tên đăng nhập)</label>
                            <input type="email" class="form-control" value="<c:out value='${profile.email}'/>" disabled>
                        </div>
                        <div class="mb-3">
                            <label class="form-label required" for="fullName">Họ và tên</label>
                            <form:input path="fullName" id="fullName" cssClass="form-control" cssErrorClass="form-control is-invalid" maxlength="100"/>
                            <form:errors path="fullName" cssClass="invalid-feedback"/>
                        </div>
                        <div class="mb-3">
                            <label class="form-label required" for="phone">Số điện thoại</label>
                            <form:input path="phone" id="phone" type="tel" cssClass="form-control" cssErrorClass="form-control is-invalid" maxlength="12"/>
                            <form:errors path="phone" cssClass="invalid-feedback"/>
                        </div>
                    </div>
                    <div class="card-footer text-end">
                        <button type="submit" class="btn btn-primary"><i class="ti ti-device-floppy me-1"></i>Lưu thông tin</button>
                    </div>
                </form:form>
            </div>

            <%-- Đổi mật khẩu --%>
            <div class="col-md-6">
                <form:form modelAttribute="passwordForm" method="post" action="${pageContext.request.contextPath}/user/profile/password"
                           cssClass="card h-100" novalidate="novalidate" autocomplete="off">
                    <div class="card-header"><h3 class="card-title">Đổi mật khẩu</h3></div>
                    <div class="card-body">
                        <c:if test="${not empty passwordError}"><div class="alert alert-danger"><c:out value="${passwordError}"/></div></c:if>
                        <div class="mb-3">
                            <label class="form-label required" for="currentPassword">Mật khẩu hiện tại</label>
                            <form:password path="currentPassword" id="currentPassword" cssClass="form-control"
                                           cssErrorClass="form-control is-invalid" autocomplete="current-password"/>
                            <form:errors path="currentPassword" cssClass="invalid-feedback"/>
                        </div>
                        <div class="mb-3">
                            <label class="form-label required" for="newPassword">Mật khẩu mới</label>
                            <form:password path="password" id="newPassword" cssClass="form-control"
                                           cssErrorClass="form-control is-invalid" autocomplete="new-password"/>
                            <form:errors path="password" cssClass="invalid-feedback"/>
                            <small class="form-hint">Tối thiểu 8 ký tự, gồm cả chữ và số.</small>
                        </div>
                        <div class="mb-3">
                            <label class="form-label required" for="confirmPassword">Nhập lại mật khẩu mới</label>
                            <form:password path="confirmPassword" id="confirmPassword" cssClass="form-control"
                                           cssErrorClass="form-control is-invalid" autocomplete="new-password"/>
                            <form:errors path="confirmPassword" cssClass="invalid-feedback"/>
                        </div>
                    </div>
                    <div class="card-footer text-end">
                        <button type="submit" class="btn btn-primary"><i class="ti ti-key me-1"></i>Đổi mật khẩu</button>
                    </div>
                </form:form>
            </div>
        </div>
    </div>
</div>
</body>
</html>
