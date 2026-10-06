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
    <h1 class="h1 fw-bold mb-1">Xin chào, <c:out value="${currentUser.fullName}"/>!</h1>
    <p class="fs-3 mb-3 opacity-75">Hôm nay bạn muốn tặng hoa cho ai?</p>
    <a href="<c:url value='/products/search'/>" class="btn btn-light"><i class="ti ti-search me-1"></i>Tìm hoa</a>
</section>

<%-- 4 khối: mỗi khối hiện 10 sản phẩm, "Xem thêm" tải tiếp qua /api/products (tối đa 20) --%>
<c:forEach var="entry" items="${blocks}">
    <c:set var="block" value="${entry.key}"/>
    <c:set var="blockPage" value="${entry.value}"/>
    <section class="mb-5" id="block-${block.key}">
        <div class="d-flex align-items-center mb-3">
            <h2 class="m-0"><i class="ti ${block.icon} text-primary me-1"></i>${block.label}</h2>
        </div>
        <c:choose>
            <c:when test="${empty blockPage.content}">
                <div class="card"><div class="card-body text-secondary text-center py-4">${block.emptyText}</div></div>
            </c:when>
            <c:otherwise>
                <div class="row row-cards ss-grid-5" data-block-items>
                    <c:forEach var="product" items="${blockPage.content}">
                        <div class="col-6 col-md-4 col-lg-3 col-xl">
                            <%@ include file="/WEB-INF/views/common/product-card.jsp" %>
                        </div>
                    </c:forEach>
                </div>
                <c:if test="${blockPage.hasNext() and maxPages > 1}">
                    <div class="text-center mt-3">
                        <button type="button" class="btn btn-outline-primary" data-load-more="${block.key}" data-next-page="1">
                            <i class="ti ti-chevron-down me-1"></i>Xem thêm
                        </button>
                    </div>
                </c:if>
            </c:otherwise>
        </c:choose>
    </section>
</c:forEach>

<script>
    // Nút "Xem thêm": gọi /api/products?type=...&page=..., thêm card vào khối, ẩn nút khi hết
    document.querySelectorAll('[data-load-more]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var type = btn.getAttribute('data-load-more');
            var page = btn.getAttribute('data-next-page');
            var grid = document.querySelector('#block-' + type + ' [data-block-items]');
            btn.disabled = true;
            fetch('<c:url value="/api/products"/>?type=' + encodeURIComponent(type) + '&page=' + page,
                {headers: {'Accept': 'application/json'}})
                .then(function (res) {
                    if (!res.ok) { throw new Error(res.status); }
                    return res.json();
                })
                .then(function (data) {
                    data.items.forEach(function (p) {
                        var col = document.createElement('div');
                        col.className = 'col-6 col-md-4 col-lg-3 col-xl';
                        col.innerHTML = StarShop.renderProductCard(p);
                        grid.appendChild(col);
                    });
                    StarShop.syncFavorites(grid);
                    if (data.hasMore) {
                        btn.setAttribute('data-next-page', data.page + 1);
                        btn.disabled = false;
                    } else {
                        btn.parentElement.remove();
                    }
                })
                .catch(function () {
                    btn.disabled = false;
                    btn.textContent = 'Lỗi tải dữ liệu, bấm để thử lại';
                });
        });
    });
</script>
</body>
</html>
