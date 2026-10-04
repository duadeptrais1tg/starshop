<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Giỏ hàng</title>
</head>
<body>
<h1 class="mb-3"><i class="ti ti-shopping-cart text-primary me-1"></i>Giỏ hàng</h1>

<c:if test="${not empty message}">
    <div class="alert alert-success" role="alert"><c:out value="${message}"/></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
</c:if>

<c:choose>
    <c:when test="${cart.lineCount == 0}">
        <div class="card">
            <div class="empty">
                <div class="empty-icon"><i class="ti ti-shopping-cart-off fs-1 text-secondary"></i></div>
                <p class="empty-title">Giỏ hàng đang trống</p>
                <p class="empty-subtitle text-secondary">Chọn vài bó hoa thật đẹp nhé!</p>
                <div class="empty-action"><a href="<c:url value='/products/search'/>" class="btn btn-primary">Mua sắm ngay</a></div>
            </div>
        </div>
    </c:when>
    <c:otherwise>
        <%-- Một form chung: ô chọn (itemIds) dùng cho "Xóa đã chọn"; nút xóa từng dòng dùng formaction riêng --%>
        <form id="cart-form" method="post" action="<c:url value='/cart/items/delete'/>">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">

            <c:forEach var="group" items="${cart.groups}">
                <div class="card mb-3" data-shop-group>
                    <div class="card-header">
                        <label class="form-check m-0">
                            <input class="form-check-input" type="checkbox" data-select-shop ${group.hasAvailable ? '' : 'disabled'}>
                            <span class="form-check-label fw-bold">
                                <i class="ti ti-building-store me-1"></i>
                                <a href="<c:url value='/shop/${group.shopSlug}'/>" class="text-reset"><c:out value="${group.shopName}"/></a>
                            </span>
                        </label>
                    </div>
                    <div class="list-group list-group-flush">
                        <c:forEach var="line" items="${group.lines}">
                            <div class="list-group-item ${line.available ? '' : 'bg-light'}" data-line="${line.itemId}"
                                 data-price="${line.unitPrice}" data-qty="${line.quantity}" ${line.available ? '' : 'data-unavailable'}>
                                <div class="row g-2 align-items-center">
                                    <div class="col-auto">
                                        <input class="form-check-input" type="checkbox" name="itemIds" value="${line.itemId}"
                                               data-select-line ${line.available ? '' : 'disabled'} aria-label="Chọn sản phẩm">
                                    </div>
                                    <div class="col-auto">
                                        <a href="<c:url value='/products/${line.productSlug}'/>">
                                            <c:choose>
                                                <c:when test="${not empty line.imageUrl}">
                                                    <span class="avatar avatar-lg" style="background-image: url('<c:out value="${line.imageUrl}"/>')"></span>
                                                </c:when>
                                                <c:otherwise><span class="avatar avatar-lg bg-primary-lt text-primary"><i class="ti ti-flower"></i></span></c:otherwise>
                                            </c:choose>
                                        </a>
                                    </div>
                                    <div class="col">
                                        <a href="<c:url value='/products/${line.productSlug}'/>" class="text-reset fw-semibold"><c:out value="${line.productName}"/></a>
                                        <div>
                                            <span class="text-primary fw-bold"><fmt:formatNumber value="${line.unitPrice}" pattern="#,##0"/>₫</span>
                                            <c:if test="${not empty line.compareAtPrice}">
                                                <del class="text-secondary small ms-1"><fmt:formatNumber value="${line.compareAtPrice}" pattern="#,##0"/>₫</del>
                                            </c:if>
                                        </div>
                                        <c:if test="${not line.available}">
                                            <span class="badge bg-warning-lt mt-1"><i class="ti ti-alert-triangle me-1"></i><c:out value="${line.unavailableReason}"/></span>
                                        </c:if>
                                    </div>
                                    <div class="col-12 col-md-auto d-flex align-items-center gap-3">
                                        <div class="input-group input-group-sm ss-qty" style="width:120px">
                                            <button type="button" class="btn" data-step="-1" aria-label="Giảm"><i class="ti ti-minus"></i></button>
                                            <input type="number" class="form-control text-center" value="${line.quantity}" min="1"
                                                   max="${line.stock > 99 ? 99 : line.stock}" data-qty-input aria-label="Số lượng"
                                                   ${line.stock <= 0 ? 'disabled' : ''}>
                                            <button type="button" class="btn" data-step="1" aria-label="Tăng"><i class="ti ti-plus"></i></button>
                                        </div>
                                        <div class="fw-bold text-end" style="min-width:110px" data-line-total>
                                            <fmt:formatNumber value="${line.lineTotal}" pattern="#,##0"/>₫
                                        </div>
                                        <button type="submit" class="btn btn-sm btn-ghost-danger" title="Xóa"
                                                formaction="<c:url value='/cart/items/${line.itemId}/delete'/>"
                                                onclick="return confirm('Xóa sản phẩm này khỏi giỏ?');"><i class="ti ti-trash"></i></button>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </c:forEach>

            <%-- Thanh tổng kết dính dưới màn hình --%>
            <div class="card position-sticky bottom-0 shadow" style="z-index: 10">
                <div class="card-body d-flex flex-wrap align-items-center gap-3">
                    <label class="form-check m-0">
                        <input class="form-check-input" type="checkbox" id="select-all">
                        <span class="form-check-label">Chọn tất cả</span>
                    </label>
                    <button type="submit" class="btn btn-sm btn-outline-danger" id="delete-selected" disabled
                            onclick="return confirm('Xóa các sản phẩm đã chọn?');">
                        <i class="ti ti-trash me-1"></i>Xóa đã chọn
                    </button>
                    <div class="ms-auto text-end">
                        <div class="text-secondary small">Tạm tính (<span id="selected-count">0</span> sản phẩm)</div>
                        <div class="text-primary fw-bold fs-2" id="subtotal">0₫</div>
                    </div>
                    <a href="#" class="btn btn-primary btn-lg disabled" id="checkout-btn" aria-disabled="true">
                        <i class="ti ti-credit-card me-1"></i>Mua hàng
                    </a>
                </div>
            </div>
        </form>
        <p class="text-secondary small mt-2">Sản phẩm của mỗi shop sẽ được tách thành một đơn hàng riêng khi thanh toán.</p>
    </c:otherwise>
</c:choose>

<script>
    (function () {
        var form = document.getElementById('cart-form');
        if (!form) {
            return;
        }
        var money = new Intl.NumberFormat('vi-VN');
        var lineBoxes = function () { return form.querySelectorAll('[data-select-line]:not(:disabled)'); };

        // Tính tạm tính từ các dòng đang chọn (server tính lại khi thanh toán, đây chỉ để hiển thị)
        function refresh() {
            var total = 0, count = 0, ids = [];
            form.querySelectorAll('[data-select-line]:checked').forEach(function (box) {
                var row = box.closest('[data-line]');
                total += parseFloat(row.getAttribute('data-price')) * parseInt(row.getAttribute('data-qty'), 10);
                count += parseInt(row.getAttribute('data-qty'), 10);
                ids.push(box.value);
            });
            document.getElementById('subtotal').textContent = money.format(total) + '₫';
            document.getElementById('selected-count').textContent = count;
            document.getElementById('delete-selected').disabled = ids.length === 0;
            var checkout = document.getElementById('checkout-btn');
            checkout.classList.toggle('disabled', ids.length === 0);
            checkout.setAttribute('aria-disabled', ids.length === 0);
            checkout.href = ids.length ? '<c:url value="/checkout"/>?items=' + ids.join(',') : '#';
            var all = lineBoxes();
            document.getElementById('select-all').checked = all.length > 0 && Array.prototype.every.call(all, function (b) { return b.checked; });
            form.querySelectorAll('[data-shop-group]').forEach(function (group) {
                var boxes = group.querySelectorAll('[data-select-line]:not(:disabled)');
                group.querySelector('[data-select-shop]').checked =
                    boxes.length > 0 && Array.prototype.every.call(boxes, function (b) { return b.checked; });
            });
        }

        form.querySelectorAll('[data-select-line]').forEach(function (box) { box.addEventListener('change', refresh); });
        form.querySelectorAll('[data-select-shop]').forEach(function (shopBox) {
            shopBox.addEventListener('change', function () {
                shopBox.closest('[data-shop-group]').querySelectorAll('[data-select-line]:not(:disabled)')
                    .forEach(function (b) { b.checked = shopBox.checked; });
                refresh();
            });
        });
        document.getElementById('select-all').addEventListener('change', function (e) {
            lineBoxes().forEach(function (b) { b.checked = e.target.checked; });
            refresh();
        });

        // Đổi số lượng -> lưu ngay bằng AJAX
        function saveQuantity(row, input, value) {
            var old = row.getAttribute('data-qty');
            StarShop.postForm('<c:url value="/api/cart/items/"/>' + row.getAttribute('data-line') + '/quantity', {quantity: value})
                .then(function (data) {
                    if (row.hasAttribute('data-unavailable')) {
                        window.location.reload();   // có thể đã hết cảnh báo "vượt tồn kho"
                        return;
                    }
                    row.setAttribute('data-qty', data.quantity);
                    input.value = data.quantity;
                    row.querySelector('[data-line-total]').textContent = money.format(data.lineTotal) + '₫';
                    refresh();
                })
                .catch(function (message) {
                    input.value = old;
                    if (message) { StarShop.toast(message, 'danger'); }
                });
        }
        form.querySelectorAll('[data-line]').forEach(function (row) {
            var input = row.querySelector('[data-qty-input]');
            input.addEventListener('change', function () { saveQuantity(row, input, input.value); });
            row.querySelectorAll('[data-step]').forEach(function (btn) {
                btn.addEventListener('click', function () {
                    var value = (parseInt(input.value, 10) || 1) + parseInt(btn.getAttribute('data-step'), 10);
                    if (value < 1 || value > parseInt(input.max, 10)) { return; }
                    input.value = value;
                    saveQuantity(row, input, value);
                });
            });
        });
        refresh();
    })();
</script>
</body>
</html>
