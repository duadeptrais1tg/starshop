<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Sản phẩm đã xem</title>
</head>
<body>
<div class="d-flex align-items-center mb-3">
    <div>
        <h1 class="m-0"><i class="ti ti-history text-primary me-1"></i>Sản phẩm đã xem</h1>
        <div class="text-secondary">Xem gần nhất ở trên cùng</div>
    </div>
    <c:if test="${page.totalElements > 0}">
        <form method="post" action="<c:url value='/user/viewed/clear'/>" class="ms-auto"
              onsubmit="return confirm('Xóa toàn bộ lịch sử xem?');">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <button type="submit" class="btn btn-outline-danger btn-sm"><i class="ti ti-trash me-1"></i>Xóa lịch sử</button>
        </form>
    </c:if>
</div>

<c:choose>
    <c:when test="${page.totalElements == 0}">
        <div class="card">
            <div class="empty">
                <div class="empty-icon"><i class="ti ti-eye fs-1 text-secondary"></i></div>
                <p class="empty-title">Bạn chưa xem sản phẩm nào</p>
                <div class="empty-action"><a href="<c:url value='/products/search'/>" class="btn btn-primary">Khám phá sản phẩm</a></div>
            </div>
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
