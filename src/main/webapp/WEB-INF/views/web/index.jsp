<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Trang chủ</title>
</head>
<body>
<section class="ss-hero mb-4">
    <h1 class="display-6 fw-bold mb-2">Hoa tươi cho mọi khoảnh khắc</h1>
    <p class="fs-3 mb-4 opacity-75">Chuỗi cửa hàng hoa StarShop – giao nhanh trong 2 giờ.</p>
    <a href="<c:url value='/products/search'/>" class="btn btn-light btn-lg">
        <i class="ti ti-flower me-1"></i> Xem sản phẩm
    </a>
</section>

<div class="d-flex align-items-center mb-3">
    <h2 class="m-0"><i class="ti ti-flame text-orange me-1"></i>Sản phẩm bán chạy</h2>
    <a href="<c:url value='/products/search?sort=BEST_SELLING'/>" class="ms-auto">Xem tất cả <i class="ti ti-chevron-right"></i></a>
</div>

<c:choose>
    <c:when test="${page.totalElements == 0}">
        <div class="empty">
            <div class="empty-icon"><i class="ti ti-mood-empty fs-1 text-secondary"></i></div>
            <p class="empty-title">Chưa có sản phẩm bán chạy</p>
            <div class="empty-action"><a href="<c:url value='/products/search'/>" class="btn btn-primary">Xem tất cả sản phẩm</a></div>
        </div>
    </c:when>
    <c:otherwise>
        <div class="row row-cards">
            <c:forEach var="product" items="${page.content}">
                <div class="col-6 col-md-4 col-lg-3">
                    <%@ include file="/WEB-INF/views/common/product-card.jsp" %>
                </div>
            </c:forEach>
        </div>
        <c:if test="${page.totalPages > 1}">
            <div class="card mt-3">
                <%@ include file="/WEB-INF/views/common/pagination.jsp" %>
            </div>
        </c:if>
    </c:otherwise>
</c:choose>
</body>
</html>
