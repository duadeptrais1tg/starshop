<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Sản phẩm yêu thích</title>
</head>
<body>
<div class="mb-3">
    <h1 class="m-0"><i class="ti ti-heart text-primary me-1"></i>Sản phẩm yêu thích</h1>
    <div class="text-secondary">
        <c:choose>
            <c:when test="${page.totalElements > 0}">${page.totalElements} sản phẩm · bấm vào tim để bỏ thích</c:when>
            <c:otherwise>Lưu lại những bó hoa bạn thích để mua sau</c:otherwise>
        </c:choose>
    </div>
</div>

<c:choose>
    <c:when test="${page.totalElements == 0}">
        <div class="card">
            <div class="empty">
                <div class="empty-icon"><i class="ti ti-heart fs-1 text-secondary"></i></div>
                <p class="empty-title">Bạn chưa thích sản phẩm nào</p>
                <p class="empty-subtitle text-secondary">Bấm biểu tượng tim trên sản phẩm để thêm vào đây.</p>
                <div class="empty-action"><a href="<c:url value='/products/search'/>" class="btn btn-primary">Khám phá sản phẩm</a></div>
            </div>
        </div>
    </c:when>
    <c:otherwise>
        <%-- data-fav-remove-card: bỏ thích ở trang này thì ẩn luôn card --%>
        <div class="row row-cards" data-fav-remove-card>
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
