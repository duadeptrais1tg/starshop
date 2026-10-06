<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Đơn ${order.orderCode}</title>
</head>
<body>
<div class="d-flex flex-wrap align-items-center gap-2 mb-3">
    <a href="<c:url value='/shipper/orders'><c:param name='status' value='${order.status}'/></c:url>" class="btn btn-sm"><i class="ti ti-arrow-left me-1"></i>Danh sách</a>
    <h2 class="m-0 ms-2"><c:out value="${order.orderCode}"/></h2>
    <span class="badge ${order.status eq 'DELIVERED' ? 'bg-success-lt' : (order.status eq 'FAILED' ? 'bg-danger-lt' : 'bg-blue-lt')}">${order.status.label}</span>
</div>
<c:if test="${not empty message}">
    <div class="alert alert-success" role="alert"><c:out value="${message}"/></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger" role="alert"><i class="ti ti-alert-circle me-1"></i><c:out value="${error}"/></div>
</c:if>
<c:if test="${not order.current}">
    <div class="alert alert-secondary">Đơn này đã được giao cho shipper khác, bạn chỉ xem lại được.</div>
</c:if>

<div class="row g-3">
    <div class="col-md-7">
        <%-- Tiền thu hộ: thông tin quan trọng nhất, đặt đầu tiên --%>
        <div class="card mb-3 ${order.cod ? 'border-orange' : ''}">
            <div class="card-body d-flex align-items-center gap-3">
                <span class="avatar avatar-lg ${order.cod ? 'bg-orange-lt text-orange' : 'bg-success-lt text-success'}"><i class="ti ti-cash"></i></span>
                <div>
                    <c:choose>
                        <c:when test="${order.cod}">
                            <div class="text-secondary">Thu hộ (COD)</div>
                            <div class="fw-bold fs-1"><fmt:formatNumber value="${order.codAmount}" pattern="#,##0"/>₫</div>
                            <c:if test="${order.codCollected}"><div class="text-success small"><i class="ti ti-check me-1"></i>Đã thu lúc ${order.collectedAt}</div></c:if>
                        </c:when>
                        <c:otherwise>
                            <div class="fw-bold fs-3">Không thu tiền</div>
                            <div class="text-secondary small">Khách đã thanh toán qua ${order.paymentMethodLabel}</div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>

        <div class="card mb-3">
            <div class="card-header"><h3 class="card-title"><i class="ti ti-map-pin me-1"></i>Giao đến</h3></div>
            <div class="card-body">
                <div class="fw-bold"><c:out value="${order.receiverName}"/></div>
                <div class="mb-2"><c:out value="${order.shippingAddress}"/></div>
                <c:if test="${not empty order.note}"><div class="alert alert-info p-2 mb-2"><i class="ti ti-note me-1"></i><c:out value="${order.note}"/></div></c:if>
                <div class="d-flex flex-wrap gap-2">
                    <a href="tel:<c:out value='${order.receiverPhone}'/>" class="btn btn-success"><i class="ti ti-phone me-1"></i><c:out value="${order.receiverPhone}"/></a>
                    <c:url var="mapUrl" value="https://www.google.com/maps/search/">
                        <c:param name="api" value="1"/><c:param name="query" value="${order.shippingAddress}"/>
                    </c:url>
                    <a href="${mapUrl}" target="_blank" rel="noopener" class="btn">
                        <i class="ti ti-map-2 me-1"></i>Mở bản đồ</a>
                </div>
            </div>
        </div>

        <div class="card mb-3">
            <div class="card-header"><h3 class="card-title"><i class="ti ti-building-store me-1"></i>Lấy hàng tại</h3></div>
            <div class="card-body">
                <div class="fw-bold"><c:out value="${order.shopName}"/></div>
                <div class="mb-2"><c:out value="${order.pickupAddress}"/></div>
                <a href="tel:<c:out value='${order.shopPhone}'/>" class="btn btn-sm"><i class="ti ti-phone me-1"></i><c:out value="${order.shopPhone}"/></a>
            </div>
            <ul class="list-group list-group-flush">
                <c:forEach var="i" items="${order.items}">
                    <li class="list-group-item d-flex"><span class="flex-fill"><c:out value="${i.productName}"/></span><strong>× ${i.quantity}</strong></li>
                </c:forEach>
            </ul>
        </div>
    </div>

    <div class="col-md-5">
        <div class="card">
            <div class="card-header"><h3 class="card-title">Cập nhật giao hàng</h3></div>
            <div class="card-body">
                <div class="small text-secondary mb-3">Phân công lúc ${order.assignedAt}<c:if test="${not empty order.deliveredAt}"> · Giao lúc ${order.deliveredAt}</c:if></div>
                <c:if test="${not empty order.failReason}">
                    <div class="alert alert-danger p-2 small"><i class="ti ti-alert-triangle me-1"></i>Giao thất bại: <c:out value="${order.failReason}"/></div>
                </c:if>

                <c:if test="${order.canStart}">
                    <form method="post" action="<c:url value='/shipper/orders/${order.id}/start'/>">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <button type="submit" class="btn btn-primary btn-lg w-100"><i class="ti ti-truck-delivery me-1"></i>Bắt đầu giao</button>
                    </form>
                </c:if>

                <c:if test="${order.canFinish}">
                    <form method="post" action="<c:url value='/shipper/orders/${order.id}/delivered'/>" class="mb-3"
                          onsubmit="return confirm('${order.cod ? 'Xác nhận đã giao và ĐÃ THU tiền của khách?' : 'Xác nhận đã giao cho khách?'}');">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <button type="submit" class="btn btn-success btn-lg w-100">
                            <i class="ti ti-circle-check me-1"></i>${order.cod ? 'Đã giao & đã thu tiền' : 'Đã giao'}
                        </button>
                    </form>
                    <form method="post" action="<c:url value='/shipper/orders/${order.id}/failed'/>">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <label class="form-label" for="fail-reason">Giao thất bại</label>
                        <select class="form-select mb-2" id="fail-preset" aria-label="Lý do thường gặp">
                            <option value="">-- Lý do thường gặp --</option>
                            <option>Không liên lạc được người nhận</option>
                            <option>Người nhận hẹn giao lại</option>
                            <option>Người nhận từ chối nhận hàng</option>
                            <option>Sai địa chỉ / không tìm thấy địa chỉ</option>
                        </select>
                        <input type="text" id="fail-reason" name="reason" class="form-control mb-2" maxlength="300" required placeholder="Lý do (bắt buộc)">
                        <button type="submit" class="btn btn-outline-danger w-100"><i class="ti ti-x me-1"></i>Báo giao thất bại</button>
                    </form>
                </c:if>

                <c:if test="${order.canRetry}">
                    <form method="post" action="<c:url value='/shipper/orders/${order.id}/retry'/>">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <button type="submit" class="btn btn-primary btn-lg w-100"><i class="ti ti-refresh me-1"></i>Giao lại</button>
                    </form>
                    <div class="small text-secondary mt-2">Shop cũng có thể giao đơn này cho shipper khác.</div>
                </c:if>

                <c:if test="${order.status eq 'DELIVERED'}">
                    <div class="text-success"><i class="ti ti-circle-check me-1"></i>Đơn đã giao thành công.</div>
                </c:if>
            </div>
        </div>
    </div>
</div>

<script>
    // Chọn lý do thường gặp -> điền vào ô lý do
    (function () {
        var preset = document.getElementById('fail-preset');
        if (preset) {
            preset.addEventListener('change', function () {
                if (preset.value) {
                    document.getElementById('fail-reason').value = preset.value;
                }
            });
        }
    })();
</script>
</body>
</html>
