<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title><c:out value="${product.name}"/></title>
</head>
<body>
<c:set var="pricing" value="${product.pricing}"/>
<ol class="breadcrumb mb-3" aria-label="breadcrumbs">
    <li class="breadcrumb-item"><a href="<c:url value='/'/>">Trang chủ</a></li>
    <li class="breadcrumb-item"><a href="<c:url value='/categories/${product.categorySlug}'/>"><c:out value="${product.categoryName}"/></a></li>
    <li class="breadcrumb-item active text-truncate" aria-current="page"><c:out value="${product.name}"/></li>
</ol>

<div class="card mb-4">
    <div class="card-body">
        <div class="row g-4">
            <%-- Gallery ảnh: bấm ảnh nhỏ để đổi ảnh lớn --%>
            <div class="col-lg-6">
                <div class="ss-gallery-main mb-2">
                    <c:choose>
                        <c:when test="${not empty product.imageUrls}">
                            <img id="gallery-main" src="<c:out value='${product.imageUrls[0]}'/>" alt="<c:out value='${product.name}'/>">
                        </c:when>
                        <c:otherwise><span class="ss-product-noimg"><i class="ti ti-flower"></i></span></c:otherwise>
                    </c:choose>
                </div>
                <c:if test="${product.imageUrls.size() > 1}">
                    <div class="d-flex flex-wrap gap-2">
                        <c:forEach var="url" items="${product.imageUrls}" varStatus="st">
                            <button type="button" class="ss-gallery-thumb ${st.first ? 'active' : ''}" data-src="<c:out value='${url}'/>"
                                    aria-label="Ảnh ${st.count}">
                                <img src="<c:out value='${url}'/>" alt="">
                            </button>
                        </c:forEach>
                    </div>
                </c:if>
            </div>

            <%-- Thông tin + mua hàng --%>
            <div class="col-lg-6">
                <h1 class="mb-2"><c:out value="${product.name}"/></h1>
                <div class="d-flex flex-wrap align-items-center gap-3 text-secondary mb-3">
                    <span class="ss-stars">
                        <c:forEach var="i" begin="1" end="5">
                            <span class="ss-star ${i <= pricing.ratingRounded ? 'on' : (i - 0.5 == pricing.ratingRounded ? 'half' : '')}">★</span>
                        </c:forEach>
                        <c:choose>
                            <c:when test="${ratingSummary.total > 0}"><span class="ms-1">${ratingSummary.average} (${ratingSummary.total} đánh giá)</span></c:when>
                            <c:otherwise><span class="ms-1">Chưa có đánh giá</span></c:otherwise>
                        </c:choose>
                    </span>
                    <span>Đã bán <fmt:formatNumber value="${pricing.soldCount}" pattern="#,##0"/></span>
                    <span><i class="ti ti-heart"></i> ${product.favoriteCount}</span>
                </div>

                <div class="p-3 rounded mb-3" style="background:#fff0f6">
                    <span class="text-primary fw-bold" style="font-size:2rem"><fmt:formatNumber value="${pricing.finalPrice}" pattern="#,##0"/>₫</span>
                    <c:if test="${not empty pricing.compareAtPrice}">
                        <del class="text-secondary ms-2"><fmt:formatNumber value="${pricing.compareAtPrice}" pattern="#,##0"/>₫</del>
                        <span class="badge bg-red text-white ms-2">-${pricing.discountPercent}%</span>
                    </c:if>
                </div>

                <c:if test="${not empty product.promotions}">
                    <div class="mb-3">
                        <div class="fw-bold mb-2"><i class="ti ti-discount-2 text-primary me-1"></i>Khuyến mãi đang áp dụng</div>
                        <c:forEach var="promo" items="${product.promotions}">
                            <div class="border rounded p-2 mb-2 small">
                                <div class="fw-semibold"><c:out value="${promo.name}"/></div>
                                <div class="text-secondary">
                                    <c:choose>
                                        <c:when test="${promo.percent}">
                                            Giảm <fmt:formatNumber value="${promo.discountValue}" pattern="#,##0.##"/>%
                                            <c:if test="${not empty promo.maxDiscount}"> (tối đa <fmt:formatNumber value="${promo.maxDiscount}" pattern="#,##0"/>₫)</c:if>
                                        </c:when>
                                        <c:otherwise>Giảm <fmt:formatNumber value="${promo.discountValue}" pattern="#,##0"/>₫ phí vận chuyển</c:otherwise>
                                    </c:choose>
                                    <c:if test="${promo.minOrderValue > 0}"> · đơn từ <fmt:formatNumber value="${promo.minOrderValue}" pattern="#,##0"/>₫</c:if>
                                    · đến ${promo.endAt}
                                </div>
                                <c:forEach var="code" items="${promo.couponCodes}">
                                    <span class="badge bg-primary-lt mt-1"><i class="ti ti-ticket me-1"></i>Mã: <c:out value="${code}"/></span>
                                </c:forEach>
                            </div>
                        </c:forEach>
                    </div>
                </c:if>

                <div class="d-flex align-items-center mb-3">
                    <span class="avatar bg-primary-lt text-primary me-2"><i class="ti ti-building-store"></i></span>
                    <div>
                        <div class="text-secondary small">Shop bán</div>
                        <a href="<c:url value='/shop/${product.shopSlug}'/>" class="fw-semibold"><c:out value="${product.shopName}"/></a>
                    </div>
                </div>

                <c:choose>
                    <c:when test="${product.inStock}">
                        <%-- Thêm vào giỏ bằng AJAX (xem script cuối trang); form POST /cart/items là dự phòng khi tắt JavaScript --%>
                        <form method="post" action="<c:url value='/cart/items'/>" id="add-to-cart-form" class="d-flex flex-wrap align-items-center gap-2 mb-2">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <input type="hidden" name="productId" value="${product.id}">
                            <div class="input-group ss-qty" style="width:150px">
                                <button type="button" class="btn" data-qty-step="-1" aria-label="Giảm"><i class="ti ti-minus"></i></button>
                                <input type="number" name="quantity" id="qty" class="form-control text-center" value="1" min="1"
                                       max="${product.stock}" aria-label="Số lượng">
                                <button type="button" class="btn" data-qty-step="1" aria-label="Tăng"><i class="ti ti-plus"></i></button>
                            </div>
                            <span class="text-secondary small">Còn ${product.stock} sản phẩm</span>
                            <button type="submit" class="btn btn-primary ms-auto"><i class="ti ti-shopping-cart-plus me-1"></i>Thêm vào giỏ</button>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <div class="alert alert-warning mb-2">Sản phẩm tạm hết hàng.</div>
                    </c:otherwise>
                </c:choose>
                <%-- Yêu thích làm ở chức năng yêu thích: POST /user/favorites/{id} (yêu cầu đăng nhập) --%>
                <form method="post" action="<c:url value='/user/favorites/${product.id}'/>">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <button type="submit" class="btn btn-outline-danger"><i class="ti ti-heart me-1"></i>Yêu thích</button>
                </form>
            </div>
        </div>
    </div>
</div>

<div class="card mb-4">
    <div class="card-header"><h3 class="card-title">Mô tả sản phẩm</h3></div>
    <div class="card-body" style="white-space: pre-line"><c:out value="${empty product.description ? 'Chưa có mô tả.' : product.description}"/></div>
</div>

<%-- Đánh giá --%>
<div class="card mb-4" id="reviews">
    <div class="card-header"><h3 class="card-title">Đánh giá sản phẩm</h3></div>
    <div class="card-body border-bottom">
        <div class="row align-items-center g-3">
            <div class="col-md-3 text-center">
                <div class="display-6 fw-bold text-warning">${ratingSummary.average}</div>
                <div class="ss-stars">
                    <c:forEach var="i" begin="1" end="5"><span class="ss-star ${i <= pricing.ratingRounded ? 'on' : ''}">★</span></c:forEach>
                </div>
                <div class="text-secondary small">${ratingSummary.total} đánh giá</div>
            </div>
            <div class="col-md-9">
                <c:forEach var="star" begin="1" end="5">
                    <c:set var="s" value="${6 - star}"/>
                    <div class="d-flex align-items-center gap-2 small">
                        <span class="text-nowrap" style="width:40px">${s} ★</span>
                        <div class="progress flex-fill" style="height:6px">
                            <div class="progress-bar bg-warning" style="width:${ratingSummary.percent(s)}%"></div>
                        </div>
                        <span class="text-secondary" style="width:30px">${ratingSummary.count(s)}</span>
                    </div>
                </c:forEach>
            </div>
        </div>
    </div>
    <c:choose>
        <c:when test="${page.totalElements == 0}">
            <div class="card-body text-secondary text-center py-4">
                <i class="ti ti-message-circle fs-1 d-block mb-1"></i>Chưa có đánh giá nào cho sản phẩm này.
            </div>
        </c:when>
        <c:otherwise>
            <div class="list-group list-group-flush">
                <c:forEach var="review" items="${page.content}">
                    <div class="list-group-item">
                        <div class="d-flex align-items-center mb-1">
                            <strong><c:out value="${review.reviewerName}"/></strong>
                            <span class="ss-stars ms-2">
                                <c:forEach var="i" begin="1" end="5"><span class="ss-star ${i <= review.rating ? 'on' : ''}">★</span></c:forEach>
                            </span>
                            <span class="ms-auto text-secondary small">${review.createdAt}</span>
                        </div>
                        <div style="white-space: pre-line"><c:out value="${review.content}"/></div>
                        <c:if test="${not empty review.media}">
                            <div class="d-flex flex-wrap gap-2 mt-2 ss-review-media">
                                <c:forEach var="m" items="${review.media}">
                                    <c:choose>
                                        <c:when test="${m.video}"><video src="<c:out value='${m.url}'/>" controls preload="metadata"></video></c:when>
                                        <c:otherwise><a href="<c:out value='${m.url}'/>" target="_blank" rel="noopener"><img src="<c:out value='${m.url}'/>" alt="Ảnh đánh giá" loading="lazy"></a></c:otherwise>
                                    </c:choose>
                                </c:forEach>
                            </div>
                        </c:if>
                    </div>
                </c:forEach>
            </div>
            <%@ include file="/WEB-INF/views/common/pagination.jsp" %>
        </c:otherwise>
    </c:choose>
</div>

<c:if test="${not empty related}">
    <h2 class="mb-3">Sản phẩm liên quan</h2>
    <div class="row row-cards">
        <c:forEach var="product" items="${related}">
            <div class="col-6 col-md-4 col-lg-3">
                <%@ include file="/WEB-INF/views/common/product-card.jsp" %>
            </div>
        </c:forEach>
    </div>
</c:if>

<script>
    (function () {
        // Gallery: bấm ảnh nhỏ -> đổi ảnh lớn
        var main = document.getElementById('gallery-main');
        document.querySelectorAll('.ss-gallery-thumb').forEach(function (thumb) {
            thumb.addEventListener('click', function () {
                main.src = thumb.getAttribute('data-src');
                document.querySelectorAll('.ss-gallery-thumb').forEach(function (t) { t.classList.remove('active'); });
                thumb.classList.add('active');
            });
        });
        // Thêm vào giỏ bằng AJAX: cập nhật badge header, không tải lại trang
        var cartForm = document.getElementById('add-to-cart-form');
        if (cartForm) {
            cartForm.addEventListener('submit', function (e) {
                e.preventDefault();
                var btn = cartForm.querySelector('button[type=submit]');
                btn.disabled = true;
                StarShop.addToCart(cartForm.productId.value, cartForm.quantity.value)
                    .catch(function () { })
                    .then(function () { btn.disabled = false; });
            });
        }
        // Nút +/- số lượng (server vẫn kiểm tra lại tồn kho khi thêm giỏ)
        var qty = document.getElementById('qty');
        document.querySelectorAll('[data-qty-step]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var value = (parseInt(qty.value, 10) || 1) + parseInt(btn.getAttribute('data-qty-step'), 10);
                qty.value = Math.min(Math.max(value, 1), parseInt(qty.max, 10));
            });
        });
    })();
</script>
</body>
</html>
