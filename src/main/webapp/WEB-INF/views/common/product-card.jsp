<%@ page pageEncoding="UTF-8" %>
<%--
  Card sản phẩm dùng chung. Trang gọi cần khai báo taglib c, fmt và đặt biến "product" (ProductCardDto)
  trước khi include tĩnh file này.
--%>
<c:url var="productUrl" value="/products/${product.slug}"/>
<div class="card card-sm h-100 ss-product-card">
    <a href="${productUrl}" class="ss-product-thumb">
        <c:choose>
            <c:when test="${not empty product.imageUrl}">
                <img src="<c:out value='${product.imageUrl}'/>" alt="<c:out value='${product.name}'/>" loading="lazy">
            </c:when>
            <c:otherwise>
                <span class="ss-product-noimg"><i class="ti ti-flower"></i></span>
            </c:otherwise>
        </c:choose>
        <c:if test="${product.discountPercent > 0}">
            <span class="badge bg-red text-white ss-product-badge">-${product.discountPercent}%</span>
        </c:if>
        <c:if test="${not product.inStock}">
            <span class="ss-product-soldout">Hết hàng</span>
        </c:if>
    </a>
    <div class="card-body d-flex flex-column">
        <div class="small text-secondary text-truncate"><i class="ti ti-building-store me-1"></i><c:out value="${product.shopName}"/></div>
        <a href="${productUrl}" class="text-reset fw-semibold ss-line-2 mb-2" title="<c:out value='${product.name}'/>"><c:out value="${product.name}"/></a>
        <div class="mt-auto">
            <div class="d-flex flex-wrap align-items-baseline gap-1">
                <span class="text-primary fw-bold fs-3"><fmt:formatNumber value="${product.finalPrice}" pattern="#,##0"/>₫</span>
                <c:if test="${not empty product.compareAtPrice}">
                    <del class="text-secondary small"><fmt:formatNumber value="${product.compareAtPrice}" pattern="#,##0"/>₫</del>
                </c:if>
            </div>
            <div class="d-flex justify-content-between align-items-center small text-secondary mt-1">
                <span class="ss-stars" title="${product.reviewCount > 0 ? product.ratingAvg : 'Chưa có đánh giá'}">
                    <c:forEach var="i" begin="1" end="5">
                        <c:choose>
                            <c:when test="${i <= product.ratingRounded}"><span class="ss-star on">★</span></c:when>
                            <c:when test="${i - 0.5 == product.ratingRounded}"><span class="ss-star half">★</span></c:when>
                            <c:otherwise><span class="ss-star">★</span></c:otherwise>
                        </c:choose>
                    </c:forEach>
                    <c:if test="${product.reviewCount > 0}"><span class="ms-1">(${product.reviewCount})</span></c:if>
                </span>
                <span>Đã bán <fmt:formatNumber value="${product.soldCount}" pattern="#,##0"/></span>
            </div>
        </div>
    </div>
</div>
