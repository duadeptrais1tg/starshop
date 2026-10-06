<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Đơn ${order.code}</title>
</head>
<body>
<div class="d-flex flex-wrap align-items-center gap-2 mb-3">
    <a href="<c:url value='/vendor/orders'/>" class="btn btn-sm"><i class="ti ti-arrow-left me-1"></i>Danh sách đơn</a>
    <h2 class="m-0 ms-2"><c:out value="${order.code}"/></h2>
    <c:set var="badgeOrderStatus" value="${order.status}"/><%@ include file="/WEB-INF/views/common/order-status-badge.jsp" %>
    <span class="text-secondary small ms-auto">Đặt lúc ${order.createdAt}</span>
</div>
<c:if test="${not empty message}">
    <div class="alert alert-success" role="alert"><c:out value="${message}"/></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger" role="alert"><i class="ti ti-alert-circle me-1"></i><c:out value="${error}"/></div>
</c:if>

<div class="row g-3">
    <div class="col-lg-8">
        <%-- Sản phẩm + tiền --%>
        <div class="card mb-3">
            <div class="card-header"><h3 class="card-title">Sản phẩm</h3></div>
            <div class="list-group list-group-flush">
                <c:forEach var="i" items="${order.items}">
                    <div class="list-group-item">
                        <div class="row g-2 align-items-center">
                            <div class="col-auto">
                                <c:choose>
                                    <c:when test="${not empty i.imageUrl}"><span class="avatar" style="background-image: url('<c:out value="${i.imageUrl}"/>')"></span></c:when>
                                    <c:otherwise><span class="avatar bg-primary-lt text-primary"><i class="ti ti-flower"></i></span></c:otherwise>
                                </c:choose>
                            </div>
                            <div class="col">
                                <c:out value="${i.productName}"/>
                                <div class="small text-secondary"><fmt:formatNumber value="${i.unitPrice}" pattern="#,##0"/>₫ × ${i.quantity}</div>
                            </div>
                            <div class="col-auto fw-bold"><fmt:formatNumber value="${i.lineTotal}" pattern="#,##0"/>₫</div>
                        </div>
                    </div>
                </c:forEach>
            </div>
            <div class="card-footer small">
                <div class="d-flex justify-content-between"><span>Tiền hàng</span><span><fmt:formatNumber value="${order.subtotal}" pattern="#,##0"/>₫</span></div>
                <c:if test="${order.productDiscount > 0}">
                    <div class="d-flex justify-content-between text-success"><span>Giảm giá sản phẩm<c:if test="${not empty order.couponCode}"> (mã <c:out value="${order.couponCode}"/>)</c:if></span>
                        <span>-<fmt:formatNumber value="${order.productDiscount}" pattern="#,##0"/>₫</span></div>
                </c:if>
                <div class="d-flex justify-content-between"><span>Phí vận chuyển</span><span><fmt:formatNumber value="${order.shippingFee}" pattern="#,##0"/>₫</span></div>
                <c:if test="${order.shippingDiscount > 0}">
                    <div class="d-flex justify-content-between text-success"><span>Giảm phí vận chuyển</span><span>-<fmt:formatNumber value="${order.shippingDiscount}" pattern="#,##0"/>₫</span></div>
                </c:if>
                <div class="d-flex justify-content-between fw-bold mt-1"><span>Khách trả</span><span><fmt:formatNumber value="${order.total}" pattern="#,##0"/>₫</span></div>
                <div class="d-flex justify-content-between text-secondary mt-1">
                    <span>Shop nhận (sau chiết khấu <fmt:formatNumber value="${order.commissionRate}" maxFractionDigits="2"/>%)</span>
                    <span><fmt:formatNumber value="${order.shopReceives}" pattern="#,##0"/>₫</span>
                </div>
            </div>
        </div>

        <%-- Yêu cầu trả hàng --%>
        <c:if test="${not empty order.returnRequest}">
            <div class="card mb-3 ${order.canReviewReturn ? 'border-orange' : ''}">
                <div class="card-header">
                    <h3 class="card-title"><i class="ti ti-arrow-back-up me-1"></i>Yêu cầu trả hàng</h3>
                    <span class="badge bg-secondary-lt ms-auto">${order.returnRequest.statusLabel}</span>
                </div>
                <div class="card-body">
                    <div class="small text-secondary mb-1">Gửi lúc ${order.returnRequest.createdAt}</div>
                    <p class="ss-pre-line"><c:out value="${order.returnRequest.reason}"/></p>
                    <c:if test="${not empty order.returnRequest.imageUrls}">
                        <div class="d-flex flex-wrap gap-2 mb-2">
                            <c:forEach var="url" items="${order.returnRequest.imageUrls}">
                                <a href="<c:out value='${url}'/>" target="_blank" rel="noopener"><img src="<c:out value='${url}'/>" alt="Ảnh minh chứng" class="rounded border ss-vendor-preview"></a>
                            </c:forEach>
                        </div>
                    </c:if>
                    <c:if test="${not empty order.returnRequest.rejectReason}">
                        <div class="text-danger small">Lý do từ chối: <c:out value="${order.returnRequest.rejectReason}"/></div>
                    </c:if>
                </div>
                <c:if test="${order.canReviewReturn}">
                    <div class="card-footer">
                        <form method="post" action="<c:url value='/vendor/orders/${order.id}/return/approve'/>" class="d-inline"
                              onsubmit="return confirm('Chấp nhận trả hàng và hoàn tiền cho khách?');">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <button type="submit" class="btn btn-success"><i class="ti ti-check me-1"></i>Chấp nhận trả hàng</button>
                        </form>
                        <form method="post" action="<c:url value='/vendor/orders/${order.id}/return/reject'/>" class="mt-2 d-flex gap-2">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <input type="text" name="reason" class="form-control" maxlength="300" required placeholder="Lý do từ chối (bắt buộc)">
                            <button type="submit" class="btn btn-outline-danger text-nowrap">Từ chối</button>
                        </form>
                    </div>
                </c:if>
            </div>
        </c:if>

        <%-- Lịch sử trạng thái --%>
        <div class="card">
            <div class="card-header"><h3 class="card-title">Lịch sử đơn hàng</h3></div>
            <div class="card-body">
                <ul class="steps steps-vertical">
                    <c:forEach var="h" items="${order.history}">
                        <li class="step-item">
                            <div class="h4 m-0">${h.toLabel}</div>
                            <div class="text-secondary small">${h.time} · <c:out value="${h.changedBy}"/></div>
                            <c:if test="${not empty h.note}"><div class="small"><c:out value="${h.note}"/></div></c:if>
                        </li>
                    </c:forEach>
                </ul>
            </div>
        </div>
    </div>

    <div class="col-lg-4">
        <%-- Hành động theo trạng thái --%>
        <c:if test="${order.canConfirm or order.canAssign or order.canCancel}">
            <div class="card mb-3">
                <div class="card-header"><h3 class="card-title">Xử lý đơn</h3></div>
                <div class="card-body">
                    <c:if test="${order.canConfirm}">
                        <form method="post" action="<c:url value='/vendor/orders/${order.id}/confirm'/>" class="mb-3">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <button type="submit" class="btn btn-primary w-100"><i class="ti ti-check me-1"></i>Xác nhận đơn</button>
                        </form>
                    </c:if>
                    <c:if test="${order.canAssign}">
                        <form method="post" action="<c:url value='/vendor/orders/${order.id}/assign'/>" class="mb-3">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <label class="form-label" for="shipperId">Giao cho shipper (<c:out value="${order.carrierName}"/>)</label>
                            <c:choose>
                                <c:when test="${empty order.shippers}">
                                    <div class="text-secondary small">Nhà vận chuyển này chưa có shipper hoạt động.</div>
                                </c:when>
                                <c:otherwise>
                                    <select id="shipperId" name="shipperId" class="form-select mb-2" required>
                                        <option value="">-- Chọn shipper --</option>
                                        <c:forEach var="s" items="${order.shippers}">
                                            <option value="${s.id}"><c:out value="${s.name}"/> – <c:out value="${s.phone}"/> (đang giao ${s.activeOrders})</option>
                                        </c:forEach>
                                    </select>
                                    <button type="submit" class="btn btn-azure w-100"><i class="ti ti-truck-delivery me-1"></i>Giao cho shipper</button>
                                </c:otherwise>
                            </c:choose>
                        </form>
                    </c:if>
                    <c:if test="${order.canCancel}">
                        <form method="post" action="<c:url value='/vendor/orders/${order.id}/cancel'/>"
                              onsubmit="return confirm('Hủy đơn ${order.code}? Tồn kho sẽ được hoàn lại.');">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <label class="form-label" for="cancel-reason">Hủy đơn</label>
                            <input type="text" id="cancel-reason" name="reason" class="form-control mb-2" maxlength="300" required
                                   placeholder="Lý do hủy (bắt buộc)">
                            <button type="submit" class="btn btn-outline-danger w-100"><i class="ti ti-x me-1"></i>Hủy đơn</button>
                        </form>
                    </c:if>
                </div>
            </div>
        </c:if>

        <%-- Giao hàng --%>
        <div class="card mb-3">
            <div class="card-header"><h3 class="card-title">Giao hàng</h3></div>
            <div class="card-body small">
                <div class="mb-2">
                    <div class="text-secondary">Người nhận</div>
                    <strong><c:out value="${order.receiverName}"/></strong> – <c:out value="${order.receiverPhone}"/>
                    <div><c:out value="${order.shippingAddress}"/></div>
                </div>
                <div class="mb-2"><span class="text-secondary">Khách hàng:</span> <c:out value="${order.customerName}"/></div>
                <div class="mb-2"><span class="text-secondary">Nhà vận chuyển:</span> <c:out value="${order.carrierName}"/></div>
                <c:if test="${not empty order.assignment}">
                    <div class="mb-2">
                        <span class="text-secondary">Shipper:</span> <c:out value="${order.assignment.shipperName}"/> – <c:out value="${order.assignment.shipperPhone}"/>
                        <span class="badge bg-secondary-lt ms-1">${order.assignment.statusLabel}</span>
                        <c:if test="${not empty order.assignment.failReason}"><div class="text-danger">Giao thất bại: <c:out value="${order.assignment.failReason}"/></div></c:if>
                    </div>
                </c:if>
                <c:if test="${not empty order.note}">
                    <div class="alert alert-info mb-0 p-2"><i class="ti ti-note me-1"></i>Ghi chú của khách: <c:out value="${order.note}"/></div>
                </c:if>
                <c:if test="${not empty order.cancelReason}">
                    <div class="text-danger mt-2">Lý do hủy: <c:out value="${order.cancelReason}"/></div>
                </c:if>
            </div>
        </div>

        <%-- Thanh toán --%>
        <div class="card">
            <div class="card-header"><h3 class="card-title">Thanh toán</h3></div>
            <div class="card-body small">
                ${order.paymentMethodLabel}
                <c:choose>
                    <c:when test="${order.paid}"><span class="badge bg-success-lt ms-1">${order.paymentStatusLabel}</span></c:when>
                    <c:otherwise><div class="text-secondary">Shipper thu <fmt:formatNumber value="${order.total}" pattern="#,##0"/>₫ khi giao.</div></c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>
</body>
</html>
