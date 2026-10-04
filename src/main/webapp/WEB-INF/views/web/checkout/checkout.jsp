<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Thanh toán</title>
</head>
<body>
<div class="d-flex align-items-center mb-3">
    <a href="<c:url value='/cart'/>" class="btn btn-sm me-3"><i class="ti ti-arrow-left me-1"></i>Giỏ hàng</a>
    <h1 class="m-0">Thanh toán</h1>
</div>

<c:if test="${not empty placeError}">
    <div class="alert alert-danger" role="alert"><i class="ti ti-alert-circle me-1"></i><c:out value="${placeError}"/></div>
</c:if>

<%-- Một form duy nhất: POST = đặt hàng; đổi lựa chọn thì script tải lại trang bằng GET (server tính lại) --%>
<form id="checkout-form" method="post" action="<c:url value='/checkout'/>">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
    <input type="hidden" name="items" value="${checkout.itemIdsParam}">

    <div class="row g-3">
        <div class="col-lg-8">
            <%-- Địa chỉ nhận hàng --%>
            <div class="card mb-3">
                <div class="card-header">
                    <h3 class="card-title"><i class="ti ti-map-pin text-primary me-1"></i>Địa chỉ nhận hàng</h3>
                    <div class="card-actions">
                        <c:url var="checkoutBack" value="/checkout"><c:param name="items" value="${checkout.itemIdsParam}"/></c:url>
                        <c:url var="newAddressUrl" value="/user/addresses/new"><c:param name="redirect" value="${checkoutBack}"/></c:url>
                        <a href="${newAddressUrl}" class="btn btn-sm"><i class="ti ti-plus me-1"></i>Thêm địa chỉ</a>
                    </div>
                </div>
                <c:choose>
                    <c:when test="${empty checkout.addresses}">
                        <div class="card-body text-secondary">Bạn chưa có địa chỉ nhận hàng. Bấm "Thêm địa chỉ" để tiếp tục.</div>
                    </c:when>
                    <c:otherwise>
                        <div class="list-group list-group-flush">
                            <c:forEach var="a" items="${checkout.addresses}">
                                <label class="list-group-item d-flex gap-3 align-items-start">
                                    <input class="form-check-input mt-1" type="radio" name="addressId" value="${a.id}" data-refresh
                                        ${a.id == checkout.addressId ? 'checked' : ''}>
                                    <span>
                                        <strong><c:out value="${a.receiverName}"/></strong>
                                        <span class="text-secondary ms-2"><c:out value="${a.phone}"/></span>
                                        <c:if test="${a.defaultAddress}"><span class="badge bg-primary-lt ms-2">Mặc định</span></c:if>
                                        <br><span class="text-secondary"><c:out value="${a.fullAddress}"/></span>
                                    </span>
                                </label>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <%-- Đơn theo từng shop --%>
            <c:forEach var="g" items="${checkout.groups}">
                <div class="card mb-3">
                    <div class="card-header">
                        <h3 class="card-title">
                            <i class="ti ti-building-store me-1"></i>
                            <a href="<c:url value='/shop/${g.shopSlug}'/>" class="text-reset"><c:out value="${g.shopName}"/></a>
                        </h3>
                    </div>
                    <div class="list-group list-group-flush">
                        <c:forEach var="line" items="${g.lines}">
                            <div class="list-group-item">
                                <div class="row g-2 align-items-center">
                                    <div class="col-auto">
                                        <c:choose>
                                            <c:when test="${not empty line.imageUrl}">
                                                <span class="avatar avatar-md" style="background-image: url('<c:out value="${line.imageUrl}"/>')"></span>
                                            </c:when>
                                            <c:otherwise><span class="avatar avatar-md bg-primary-lt text-primary"><i class="ti ti-flower"></i></span></c:otherwise>
                                        </c:choose>
                                    </div>
                                    <div class="col">
                                        <div class="fw-semibold"><c:out value="${line.productName}"/></div>
                                        <div class="small text-secondary">
                                            <fmt:formatNumber value="${line.unitPrice}" pattern="#,##0"/>₫
                                            <c:if test="${not empty line.compareAtPrice}"><del class="ms-1"><fmt:formatNumber value="${line.compareAtPrice}" pattern="#,##0"/>₫</del></c:if>
                                            × ${line.quantity}
                                        </div>
                                        <c:if test="${not empty line.unavailableReason}">
                                            <span class="badge bg-danger-lt mt-1"><i class="ti ti-alert-triangle me-1"></i><c:out value="${line.unavailableReason}"/></span>
                                        </c:if>
                                    </div>
                                    <div class="col-auto fw-bold"><fmt:formatNumber value="${line.lineTotal}" pattern="#,##0"/>₫</div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                    <div class="card-body border-top">
                        <%-- Mã giảm giá của đơn --%>
                        <label class="form-label" for="coupon_${g.shopId}"><i class="ti ti-ticket me-1"></i>Mã giảm giá</label>
                        <div class="input-group mb-2" style="max-width: 420px">
                            <input type="text" class="form-control text-uppercase ${not empty g.couponError ? 'is-invalid' : (g.couponApplied ? 'is-valid' : '')}"
                                   id="coupon_${g.shopId}" name="coupon_${g.shopId}" value="<c:out value='${g.couponCode}'/>"
                                   maxlength="50" placeholder="Nhập mã của shop hoặc toàn sàn" autocomplete="off">
                            <button type="button" class="btn" data-refresh>Áp dụng</button>
                            <c:if test="${not empty g.couponCode}">
                                <button type="button" class="btn btn-ghost-danger" data-clear-coupon="coupon_${g.shopId}" title="Bỏ mã"><i class="ti ti-x"></i></button>
                            </c:if>
                        </div>
                        <c:if test="${not empty g.couponError}">
                            <div class="text-danger small mb-2"><c:out value="${g.couponError}"/></div>
                        </c:if>
                        <c:if test="${g.couponApplied}">
                            <div class="text-success small mb-2"><i class="ti ti-check me-1"></i>Đã áp dụng "<c:out value="${g.couponName}"/>"</div>
                        </c:if>
                        <c:if test="${not empty g.couponOptions}">
                            <div class="small text-secondary mb-1">Mã dùng được cho đơn này:</div>
                            <div class="d-flex flex-wrap gap-2 mb-2">
                                <c:forEach var="opt" items="${g.couponOptions}">
                                    <button type="button" class="btn btn-sm ${opt.code eq g.couponCode ? 'btn-primary' : 'btn-outline-primary'} text-start"
                                            data-use-coupon="coupon_${g.shopId}" data-code="<c:out value='${opt.code}'/>"
                                            title="<c:out value='${opt.description}'/> – HSD <c:out value='${opt.endAt}'/>">
                                        <span>
                                            <strong><c:out value="${opt.code}"/></strong>
                                            <span class="ms-1">-<fmt:formatNumber value="${opt.discount}" pattern="#,##0"/>₫</span>
                                            <br><span class="small opacity-75"><c:out value="${opt.description}"/></span>
                                        </span>
                                    </button>
                                </c:forEach>
                            </div>
                        </c:if>
                        <c:if test="${not empty g.autoPromotionNames}">
                            <div class="small text-success mb-2"><i class="ti ti-discount me-1"></i>Tự động áp dụng:
                                <c:forEach var="n" items="${g.autoPromotionNames}" varStatus="st"><c:out value="${n}"/>${st.last ? '' : ', '}</c:forEach>
                            </div>
                        </c:if>

                        <label class="form-label mt-2" for="note_${g.shopId}">Ghi chú cho shop</label>
                        <input type="text" class="form-control" id="note_${g.shopId}" name="note_${g.shopId}" maxlength="500"
                               value="<c:out value='${g.note}'/>" placeholder="Ví dụ: ghi thiệp &quot;Chúc mừng sinh nhật&quot;, giao giờ hành chính...">
                    </div>
                    <div class="card-footer small">
                        <div class="d-flex justify-content-between"><span>Tiền hàng</span><span><fmt:formatNumber value="${g.subtotal}" pattern="#,##0"/>₫</span></div>
                        <c:if test="${g.productDiscount > 0}">
                            <div class="d-flex justify-content-between text-success"><span>Giảm giá sản phẩm</span><span>-<fmt:formatNumber value="${g.productDiscount}" pattern="#,##0"/>₫</span></div>
                        </c:if>
                        <div class="d-flex justify-content-between"><span>Phí vận chuyển</span><span><fmt:formatNumber value="${g.shippingFee}" pattern="#,##0"/>₫</span></div>
                        <c:if test="${g.shippingDiscount > 0}">
                            <div class="d-flex justify-content-between text-success"><span>Giảm phí vận chuyển</span><span>-<fmt:formatNumber value="${g.shippingDiscount}" pattern="#,##0"/>₫</span></div>
                        </c:if>
                        <div class="d-flex justify-content-between fw-bold mt-1"><span>Tổng đơn</span><span><fmt:formatNumber value="${g.total}" pattern="#,##0"/>₫</span></div>
                    </div>
                </div>
            </c:forEach>
        </div>

        <div class="col-lg-4">
            <div class="position-sticky" style="top: 1rem">
                <%-- Nhà vận chuyển --%>
                <div class="card mb-3">
                    <div class="card-header"><h3 class="card-title"><i class="ti ti-truck-delivery text-primary me-1"></i>Nhà vận chuyển</h3></div>
                    <div class="list-group list-group-flush">
                        <c:forEach var="carrier" items="${checkout.carriers}">
                            <label class="list-group-item d-flex align-items-center gap-2">
                                <input class="form-check-input m-0" type="radio" name="carrierId" value="${carrier.id}" data-refresh
                                    ${carrier.id == checkout.carrierId ? 'checked' : ''}>
                                <span class="flex-fill"><c:out value="${carrier.name}"/></span>
                                <span class="text-secondary"><fmt:formatNumber value="${carrier.shippingFee}" pattern="#,##0"/>₫/đơn</span>
                            </label>
                        </c:forEach>
                        <c:if test="${empty checkout.carriers}"><div class="list-group-item text-secondary">Chưa có nhà vận chuyển.</div></c:if>
                    </div>
                </div>

                <%-- Phương thức thanh toán --%>
                <div class="card mb-3">
                    <div class="card-header"><h3 class="card-title"><i class="ti ti-credit-card text-primary me-1"></i>Thanh toán</h3></div>
                    <div class="list-group list-group-flush">
                        <c:forEach var="m" items="${checkout.paymentMethods}">
                            <label class="list-group-item d-flex align-items-center gap-2 ${m eq 'COD' ? '' : 'text-secondary'}">
                                <input class="form-check-input m-0" type="radio" name="payment" value="${m}" data-refresh
                                    ${m eq checkout.paymentMethod ? 'checked' : ''} ${m eq 'COD' ? '' : 'disabled'}>
                                <span class="flex-fill">${m.label}</span>
                                <c:if test="${m ne 'COD'}"><span class="badge bg-secondary-lt">Sắp có</span></c:if>
                            </label>
                        </c:forEach>
                    </div>
                </div>

                <%-- Tổng thanh toán --%>
                <div class="card">
                    <div class="card-body">
                        <div class="d-flex justify-content-between mb-1"><span>Tiền hàng (${checkout.itemCount} sản phẩm)</span>
                            <span><fmt:formatNumber value="${checkout.subtotal}" pattern="#,##0"/>₫</span></div>
                        <c:if test="${checkout.productDiscount > 0}">
                            <div class="d-flex justify-content-between mb-1 text-success"><span>Giảm giá sản phẩm</span>
                                <span>-<fmt:formatNumber value="${checkout.productDiscount}" pattern="#,##0"/>₫</span></div>
                        </c:if>
                        <div class="d-flex justify-content-between mb-1"><span>Phí vận chuyển (${checkout.groups.size()} đơn)</span>
                            <span><fmt:formatNumber value="${checkout.shippingFee}" pattern="#,##0"/>₫</span></div>
                        <c:if test="${checkout.shippingDiscount > 0}">
                            <div class="d-flex justify-content-between mb-1 text-success"><span>Giảm phí vận chuyển</span>
                                <span>-<fmt:formatNumber value="${checkout.shippingDiscount}" pattern="#,##0"/>₫</span></div>
                        </c:if>
                        <hr class="my-2">
                        <div class="d-flex justify-content-between align-items-baseline">
                            <span class="fw-bold">Tổng thanh toán</span>
                            <span class="text-primary fw-bold fs-2"><fmt:formatNumber value="${checkout.total}" pattern="#,##0"/>₫</span>
                        </div>

                        <c:if test="${not checkout.canPlace}">
                            <div class="alert alert-warning mt-3 mb-0 small">
                                <c:forEach var="e" items="${checkout.errors}"><div><i class="ti ti-alert-triangle me-1"></i><c:out value="${e}"/></div></c:forEach>
                            </div>
                        </c:if>
                        <button type="submit" class="btn btn-primary btn-lg w-100 mt-3" id="place-order" ${checkout.canPlace ? '' : 'disabled'}>
                            <i class="ti ti-check me-1"></i>Đặt hàng
                        </button>
                        <div class="small text-secondary mt-2 text-center">Mỗi shop là một đơn riêng, giao và theo dõi riêng.</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</form>

<script>
    (function () {
        var form = document.getElementById('checkout-form');

        // Tải lại trang xem trước với lựa chọn mới (GET, không gửi token CSRF lên URL)
        function refresh() {
            var params = new URLSearchParams();
            new FormData(form).forEach(function (value, key) {
                if (key !== '${_csrf.parameterName}' && String(value).trim() !== '') {
                    params.append(key, value);
                }
            });
            window.location.href = '<c:url value="/checkout"/>?' + params.toString();
        }

        form.querySelectorAll('input[type=radio][data-refresh]').forEach(function (el) {
            el.addEventListener('change', refresh);
        });
        form.querySelectorAll('button[data-refresh]').forEach(function (el) {
            el.addEventListener('click', refresh);
        });
        form.querySelectorAll('[data-use-coupon]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                document.getElementById(btn.getAttribute('data-use-coupon')).value = btn.getAttribute('data-code');
                refresh();
            });
        });
        form.querySelectorAll('[data-clear-coupon]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                document.getElementById(btn.getAttribute('data-clear-coupon')).value = '';
                refresh();
            });
        });
        // Enter trong ô mã giảm giá = Áp dụng (không đặt hàng)
        form.querySelectorAll('input[name^="coupon_"]').forEach(function (input) {
            input.addEventListener('keydown', function (e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    refresh();
                }
            });
        });
        // Chống bấm "Đặt hàng" 2 lần
        form.addEventListener('submit', function () {
            document.getElementById('place-order').disabled = true;
        });
    })();
</script>
</body>
</html>
