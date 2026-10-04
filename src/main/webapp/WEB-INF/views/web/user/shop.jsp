<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>${empty shop ? 'Mở shop' : 'Shop của tôi'}</title>
</head>
<body>
<div class="row g-3">
    <div class="col-lg-3"><%@ include file="account-nav.jsp" %></div>
    <div class="col-lg-9">
        <c:if test="${not empty message}">
            <div class="alert alert-success" role="alert"><c:out value="${message}"/></div>
        </c:if>
        <c:if test="${not empty error}">
            <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
        </c:if>

        <%-- Trạng thái shop đã đăng ký --%>
        <c:if test="${not empty shop}">
            <div class="card mb-3">
                <div class="card-body d-flex flex-wrap align-items-center gap-3">
                    <c:choose>
                        <c:when test="${not empty shop.logoUrl}">
                            <span class="avatar avatar-lg" style="background-image: url('<c:out value="${shop.logoUrl}"/>')"></span>
                        </c:when>
                        <c:otherwise><span class="avatar avatar-lg bg-primary-lt text-primary"><i class="ti ti-building-store"></i></span></c:otherwise>
                    </c:choose>
                    <div class="flex-fill">
                        <h2 class="m-0"><c:out value="${shop.name}"/></h2>
                        <div class="mt-1">
                            <c:set var="badgeStatus" value="${shop.status}"/>
                            <%@ include file="/WEB-INF/views/common/shop-status-badge.jsp" %>
                            <span class="text-secondary small ms-1">Gửi lúc ${shop.createdAt}</span>
                        </div>
                    </div>
                    <c:if test="${shop.approved}">
                        <div class="d-flex gap-2">
                            <a href="<c:url value='/shop/${shop.slug}'/>" class="btn"><i class="ti ti-eye me-1"></i>Xem trang shop</a>
                            <a href="<c:url value='/vendor'/>" class="btn btn-primary"><i class="ti ti-building-store me-1"></i>Kênh người bán</a>
                        </div>
                    </c:if>
                </div>
                <c:choose>
                    <c:when test="${shop.pending}">
                        <div class="card-footer text-secondary">
                            <i class="ti ti-clock me-1"></i>Yêu cầu đang chờ quản trị viên duyệt. Khi được duyệt, tài khoản của bạn
                            có thêm quyền <strong>Người bán</strong> để đăng sản phẩm và nhận đơn.
                        </div>
                    </c:when>
                    <c:when test="${shop.rejected}">
                        <div class="card-footer">
                            <div class="text-danger"><i class="ti ti-alert-circle me-1"></i>Yêu cầu bị từ chối:
                                <strong><c:out value="${shop.statusReason}"/></strong></div>
                            <div class="text-secondary small">Bạn có thể sửa thông tin bên dưới và gửi lại.</div>
                        </div>
                    </c:when>
                    <c:when test="${shop.status eq 'SUSPENDED'}">
                        <div class="card-footer text-danger">
                            <i class="ti ti-ban me-1"></i>Shop đang bị đình chỉ: <c:out value="${shop.statusReason}"/>
                        </div>
                    </c:when>
                </c:choose>
            </div>
        </c:if>

        <c:if test="${canRegister}">
            <form:form modelAttribute="shopForm" method="post" action="${pageContext.request.contextPath}/user/shop"
                       enctype="multipart/form-data" cssClass="card" novalidate="novalidate">
                <div class="card-header">
                    <div>
                        <h3 class="card-title">${empty shop ? 'Đăng ký mở shop' : 'Gửi lại yêu cầu mở shop'}</h3>
                        <div class="card-subtitle">Bán hoa trên StarShop: điền thông tin shop, quản trị viên sẽ duyệt trong thời gian sớm nhất.</div>
                    </div>
                </div>
                <div class="card-body">
                    <c:set var="currentLogo" value="${shop.logoUrl}"/>
                    <c:set var="currentBanner" value="${shop.bannerUrl}"/>
                    <%@ include file="/WEB-INF/views/common/shop-form-fields.jsp" %>
                </div>
                <div class="card-footer text-end">
                    <button type="submit" class="btn btn-primary"><i class="ti ti-send me-1"></i>Gửi yêu cầu</button>
                </div>
            </form:form>
        </c:if>
    </div>
</div>
</body>
</html>
