<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Thông tin shop</title>
</head>
<body>
<c:if test="${not empty message}">
    <div class="alert alert-success" role="alert"><c:out value="${message}"/></div>
</c:if>
<c:if test="${shop.status eq 'SUSPENDED'}">
    <div class="alert alert-danger" role="alert">
        <i class="ti ti-ban me-1"></i>Shop đang bị đình chỉ, khách không xem được trang shop và sản phẩm:
        <strong><c:out value="${shop.statusReason}"/></strong>
    </div>
</c:if>

<div class="card mb-3">
    <div class="card-body d-flex flex-wrap align-items-center gap-3">
        <div class="flex-fill">
            <h2 class="m-0"><c:out value="${shop.name}"/></h2>
            <div class="text-secondary small mt-1">
                <c:set var="badgeStatus" value="${shop.status}"/>
                <%@ include file="/WEB-INF/views/common/shop-status-badge.jsp" %>
                <c:if test="${not empty shop.storeName}">
                    <span class="ms-2"><i class="ti ti-building me-1"></i>Chi nhánh quản lý: <c:out value="${shop.storeName}"/></span>
                </c:if>
            </div>
        </div>
        <c:if test="${shop.status eq 'APPROVED'}">
            <a href="<c:url value='/shop/${shop.slug}'/>" class="btn" target="_blank" rel="noopener">
                <i class="ti ti-external-link me-1"></i>Xem trang shop
            </a>
        </c:if>
    </div>
</div>

<form:form modelAttribute="shopForm" method="post" action="${pageContext.request.contextPath}/vendor/shop"
           enctype="multipart/form-data" cssClass="card" novalidate="novalidate">
    <div class="card-header"><h3 class="card-title">Thông tin trang chủ shop</h3></div>
    <div class="card-body">
        <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
        <c:set var="currentLogo" value="${shop.logoUrl}"/>
        <c:set var="currentBanner" value="${shop.bannerUrl}"/>
        <%@ include file="/WEB-INF/views/common/shop-form-fields.jsp" %>
        <div class="form-hint mt-3">Đổi tên shop không làm thay đổi đường dẫn trang shop (/shop/<c:out value="${shop.slug}"/>).</div>
    </div>
    <div class="card-footer text-end">
        <button type="submit" class="btn btn-primary"><i class="ti ti-device-floppy me-1"></i>Lưu thay đổi</button>
    </div>
</form:form>
</body>
</html>
