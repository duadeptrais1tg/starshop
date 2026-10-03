<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title><c:out value="${category.name}"/></title>
</head>
<body>
<ol class="breadcrumb mb-3" aria-label="breadcrumbs">
    <li class="breadcrumb-item"><a href="<c:url value='/'/>">Trang chủ</a></li>
    <c:if test="${not empty category.parent}">
        <li class="breadcrumb-item"><a href="<c:url value='/categories/${category.parent.slug}'/>"><c:out value="${category.parent.name}"/></a></li>
    </c:if>
    <li class="breadcrumb-item active" aria-current="page"><c:out value="${category.name}"/></li>
</ol>

<div class="d-flex flex-wrap align-items-center gap-2 mb-3">
    <div>
        <h1 class="m-0"><c:out value="${category.name}"/></h1>
        <div class="text-secondary">${page.totalElements} sản phẩm</div>
    </div>
    <form method="get" action="<c:url value='/categories/${category.slug}'/>" class="ms-auto d-flex align-items-center gap-2">
        <label for="sort" class="text-secondary text-nowrap mb-0">Sắp xếp</label>
        <select id="sort" name="sort" class="form-select form-select-sm" onchange="this.form.submit()">
            <c:forEach var="s" items="${sorts}">
                <option value="${s}" ${sort eq s.name() ? 'selected' : ''}>${s.label}</option>
            </c:forEach>
        </select>
        <noscript><button type="submit" class="btn btn-sm">Áp dụng</button></noscript>
    </form>
</div>

<c:if test="${not empty category.children}">
    <div class="d-flex flex-wrap gap-2 mb-3">
        <c:forEach var="child" items="${category.children}">
            <a href="<c:url value='/categories/${child.slug}'/>" class="btn btn-sm btn-outline-primary rounded-pill"><c:out value="${child.name}"/></a>
        </c:forEach>
    </div>
</c:if>

<c:choose>
    <c:when test="${page.totalElements == 0}">
        <div class="card">
            <div class="empty">
                <div class="empty-icon"><i class="ti ti-flower fs-1 text-secondary"></i></div>
                <p class="empty-title">Danh mục chưa có sản phẩm</p>
                <div class="empty-action"><a href="<c:url value='/products/search'/>" class="btn btn-primary">Xem sản phẩm khác</a></div>
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
