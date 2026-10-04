<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title><c:out value="${shop.name}"/></title>
    <meta name="description" content="<c:out value='${shop.name}'/> – shop hoa trên StarShop">
</head>
<body>
<%-- Banner + thông tin shop --%>
<div class="card mb-3 overflow-hidden">
    <div class="ss-shop-banner" <c:if test="${not empty shop.bannerUrl}">style="background-image: url('<c:out value="${shop.bannerUrl}"/>')"</c:if>></div>
    <div class="card-body pt-0">
        <div class="d-flex flex-wrap align-items-end gap-3">
            <c:choose>
                <c:when test="${not empty shop.logoUrl}">
                    <span class="avatar avatar-xl ss-shop-logo" style="background-image: url('<c:out value="${shop.logoUrl}"/>')"></span>
                </c:when>
                <c:otherwise>
                    <span class="avatar avatar-xl ss-shop-logo bg-primary-lt text-primary fs-1"><c:out value="${shop.initial}"/></span>
                </c:otherwise>
            </c:choose>
            <div class="flex-fill pt-2">
                <h1 class="m-0"><c:out value="${shop.name}"/></h1>
                <div class="d-flex flex-wrap align-items-center gap-3 text-secondary mt-1">
                    <span class="ss-stars" title="Đánh giá trung bình">
                        <c:forEach var="i" begin="1" end="5">
                            <span class="ss-star ${i <= shop.ratingRounded ? 'on' : (i - 0.5 == shop.ratingRounded ? 'half' : '')}">★</span>
                        </c:forEach>
                        <c:choose>
                            <c:when test="${shop.reviewCount > 0}">
                                <strong class="text-body ms-1"><fmt:formatNumber value="${shop.ratingAvg}" minFractionDigits="1" maxFractionDigits="1"/></strong>
                                <span>(<fmt:formatNumber value="${shop.reviewCount}"/> đánh giá)</span>
                            </c:when>
                            <c:otherwise><span class="ms-1">Chưa có đánh giá</span></c:otherwise>
                        </c:choose>
                    </span>
                    <span><i class="ti ti-flower me-1"></i><fmt:formatNumber value="${shop.productCount}"/> sản phẩm</span>
                    <span><i class="ti ti-shopping-bag me-1"></i>Đã bán <fmt:formatNumber value="${shop.soldCount}"/></span>
                    <c:if test="${not empty shop.joinedAt}">
                        <span><i class="ti ti-calendar me-1"></i>Tham gia ${shop.joinedAt}</span>
                    </c:if>
                </div>
            </div>
        </div>
        <c:if test="${not empty shop.description}">
            <p class="mt-3 mb-0 ss-pre-line"><c:out value="${shop.description}"/></p>
        </c:if>
    </div>
</div>

<%-- Tìm trong shop + sắp xếp --%>
<c:url var="shopUrl" value="/shop/${shop.slug}"/>
<form method="get" action="${shopUrl}" class="d-flex flex-wrap align-items-center gap-2 mb-3" role="search">
    <div class="input-icon flex-grow-1" style="max-width: 420px">
        <span class="input-icon-addon"><i class="ti ti-search"></i></span>
        <input type="search" name="keyword" class="form-control" placeholder="Tìm trong shop này..."
               value="<c:out value='${criteria.keyword}'/>" maxlength="100" aria-label="Tìm trong shop">
    </div>
    <button type="submit" class="btn">Tìm</button>
    <div class="ms-auto d-flex align-items-center gap-2">
        <label for="shop-sort" class="text-secondary text-nowrap mb-0">Sắp xếp</label>
        <select id="shop-sort" name="sort" class="form-select form-select-sm" onchange="this.form.submit()">
            <c:forEach var="s" items="${sorts}">
                <option value="${s}" ${criteria.sort eq s.name() ? 'selected' : ''}>${s.label}</option>
            </c:forEach>
        </select>
    </div>
</form>

<c:choose>
    <c:when test="${page.totalElements == 0}">
        <div class="card">
            <div class="empty">
                <div class="empty-icon"><i class="ti ti-flower-off fs-1 text-secondary"></i></div>
                <c:choose>
                    <c:when test="${not empty criteria.keyword}">
                        <p class="empty-title">Không tìm thấy sản phẩm phù hợp trong shop</p>
                        <div class="empty-action"><a href="${shopUrl}" class="btn btn-primary">Xem tất cả sản phẩm của shop</a></div>
                    </c:when>
                    <c:otherwise><p class="empty-title">Shop chưa có sản phẩm nào</p></c:otherwise>
                </c:choose>
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
