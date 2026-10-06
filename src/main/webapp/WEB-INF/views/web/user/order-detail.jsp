<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Đơn ${order.code}</title>
</head>
<body>
<div class="row g-3">
    <div class="col-lg-3"><%@ include file="account-nav.jsp" %></div>
    <div class="col-lg-9">
        <div class="d-flex flex-wrap align-items-center gap-2 mb-3">
            <a href="<c:url value='/user/orders'/>" class="btn btn-sm"><i class="ti ti-arrow-left me-1"></i>Đơn hàng của tôi</a>
            <h2 class="m-0 ms-2"><c:out value="${order.code}"/></h2>
            <c:choose>
                <c:when test="${order.awaitingPayment}"><span class="badge bg-warning-lt">Chờ thanh toán</span></c:when>
                <c:otherwise><c:set var="badgeOrderStatus" value="${order.status}"/><%@ include file="/WEB-INF/views/common/order-status-badge.jsp" %></c:otherwise>
            </c:choose>
            <span class="text-secondary small ms-auto">Đặt lúc ${order.createdAt}</span>
        </div>
        <c:if test="${not empty message}">
            <div class="alert alert-success" role="alert"><c:out value="${message}"/></div>
        </c:if>
        <%-- Lỗi hủy / trả hàng hiện ở đầu trang (form có thể đã bị ẩn khi đơn không còn hủy / trả được) --%>
        <c:if test="${not empty cancelError or not empty returnError}">
            <div class="alert alert-danger" role="alert"><i class="ti ti-alert-circle me-1"></i><c:out value="${not empty cancelError ? cancelError : returnError}"/></div>
        </c:if>
        <c:if test="${order.awaitingPayment}">
            <div class="alert alert-warning d-flex flex-wrap align-items-center gap-2">
                <span><i class="ti ti-clock me-1"></i>Đơn đang chờ thanh toán ${order.paymentMethodLabel}. Quá hạn đơn sẽ tự hủy.</span>
                <a href="<c:url value='/payment/vnpay/pay'><c:param name='ref' value='${order.paymentTxnRef}'/></c:url>" class="btn btn-sm btn-primary ms-auto">Tiếp tục thanh toán</a>
            </div>
        </c:if>

        <div class="row g-3">
            <div class="col-md-7">
                <%-- Sản phẩm --%>
                <div class="card mb-3">
                    <div class="card-header">
                        <h3 class="card-title"><i class="ti ti-building-store me-1"></i>
                            <a href="<c:url value='/shop/${order.shopSlug}'/>" class="text-reset"><c:out value="${order.shopName}"/></a></h3>
                    </div>
                    <div class="list-group list-group-flush">
                        <c:forEach var="i" items="${order.items}">
                            <a href="<c:url value='/products/${i.productSlug}'/>" class="list-group-item list-group-item-action">
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
                            </a>
                        </c:forEach>
                    </div>
                    <div class="card-footer small">
                        <div class="d-flex justify-content-between"><span>Tiền hàng</span><span><fmt:formatNumber value="${order.subtotal}" pattern="#,##0"/>₫</span></div>
                        <c:if test="${order.productDiscount > 0}">
                            <div class="d-flex justify-content-between text-success"><span>Giảm giá<c:if test="${not empty order.couponCode}"> (mã <c:out value="${order.couponCode}"/>)</c:if></span>
                                <span>-<fmt:formatNumber value="${order.productDiscount}" pattern="#,##0"/>₫</span></div>
                        </c:if>
                        <div class="d-flex justify-content-between"><span>Phí vận chuyển</span><span><fmt:formatNumber value="${order.shippingFee}" pattern="#,##0"/>₫</span></div>
                        <c:if test="${order.shippingDiscount > 0}">
                            <div class="d-flex justify-content-between text-success"><span>Giảm phí vận chuyển</span><span>-<fmt:formatNumber value="${order.shippingDiscount}" pattern="#,##0"/>₫</span></div>
                        </c:if>
                        <div class="d-flex justify-content-between fw-bold fs-3 mt-1"><span>Tổng tiền</span>
                            <span class="text-primary"><fmt:formatNumber value="${order.total}" pattern="#,##0"/>₫</span></div>
                    </div>
                </div>

                <%-- Yêu cầu trả hàng đã gửi --%>
                <c:if test="${not empty order.returnRequest}">
                    <div class="card mb-3">
                        <div class="card-header">
                            <h3 class="card-title"><i class="ti ti-arrow-back-up me-1"></i>Yêu cầu trả hàng</h3>
                            <span class="badge bg-secondary-lt ms-auto">${order.returnRequest.statusLabel}</span>
                        </div>
                        <div class="card-body">
                            <div class="small text-secondary mb-1">Gửi lúc ${order.returnRequest.createdAt}</div>
                            <p class="ss-pre-line"><c:out value="${order.returnRequest.reason}"/></p>
                            <div class="d-flex flex-wrap gap-2">
                                <c:forEach var="url" items="${order.returnRequest.imageUrls}">
                                    <a href="<c:out value='${url}'/>" target="_blank" rel="noopener"><img src="<c:out value='${url}'/>" alt="Ảnh minh chứng" class="rounded border ss-vendor-preview"></a>
                                </c:forEach>
                            </div>
                            <c:if test="${not empty order.returnRequest.rejectReason}">
                                <div class="text-danger mt-2">Shop từ chối: <c:out value="${order.returnRequest.rejectReason}"/></div>
                            </c:if>
                        </div>
                    </div>
                </c:if>

                <%-- Gửi yêu cầu trả hàng --%>
                <c:if test="${order.canRequestReturn}">
                    <form method="post" action="<c:url value='/user/orders/${order.id}/return'/>" enctype="multipart/form-data" class="card mb-3">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <div class="card-header">
                            <h3 class="card-title"><i class="ti ti-arrow-back-up me-1"></i>Yêu cầu trả hàng / hoàn tiền</h3>
                        </div>
                        <div class="card-body">
                            <div class="text-secondary small mb-2">Bạn có thể yêu cầu trong ${order.returnDays} ngày sau khi nhận hàng (hạn chót ${order.returnDeadline}).</div>
                            <label class="form-label required" for="return-reason">Lý do</label>
                            <textarea id="return-reason" name="reason" rows="3" class="form-control mb-2" minlength="10" maxlength="1000" required
                                      placeholder="Ví dụ: hoa bị dập, héo khi nhận; giao sai mẫu..."><c:out value="${returnReason}"/></textarea>
                            <label class="form-label required" for="return-images">Ảnh minh chứng (1–${maxReturnImages} ảnh)</label>
                            <input type="file" id="return-images" name="images" class="form-control" multiple required accept="image/jpeg,image/png,image/webp">
                            <div class="form-hint">JPG/PNG/WEBP, mỗi ảnh tối đa 5MB.</div>
                        </div>
                        <div class="card-footer text-end">
                            <button type="submit" class="btn btn-warning" onclick="return confirm('Gửi yêu cầu trả hàng? Mỗi đơn chỉ được gửi một lần.');">Gửi yêu cầu</button>
                        </div>
                    </form>
                </c:if>
            </div>

            <div class="col-md-5">
                <%-- Hủy đơn --%>
                <c:if test="${order.canCancel}">
                    <form method="post" action="<c:url value='/user/orders/${order.id}/cancel'/>" class="card mb-3"
                          onsubmit="return confirm('Bạn chắc chắn muốn hủy đơn ${order.code}?');">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <div class="card-header"><h3 class="card-title">Hủy đơn hàng</h3></div>
                        <div class="card-body">
                            <label class="form-label required" for="cancel-reason">Lý do hủy</label>
                            <select id="cancel-reason" name="reason" class="form-select mb-2" required>
                                <option value="">-- Chọn lý do --</option>
                                <c:forEach var="r" items="${cancelReasons}">
                                    <option value="${r}" ${selectedReason eq r.name() ? 'selected' : ''}>${r.label}</option>
                                </c:forEach>
                            </select>
                            <input type="text" id="other-reason" name="otherReason" class="form-control ${selectedReason eq 'OTHER' ? '' : 'd-none'}"
                                   maxlength="300" placeholder="Nhập lý do" value="<c:out value='${otherReason}'/>">
                        </div>
                        <div class="card-footer">
                            <button type="submit" class="btn btn-outline-danger w-100"><i class="ti ti-x me-1"></i>Hủy đơn</button>
                        </div>
                    </form>
                </c:if>

                <div class="card mb-3">
                    <div class="card-header"><h3 class="card-title">Giao hàng</h3></div>
                    <div class="card-body small">
                        <strong><c:out value="${order.receiverName}"/></strong> – <c:out value="${order.receiverPhone}"/>
                        <div class="mb-2"><c:out value="${order.shippingAddress}"/></div>
                        <div><span class="text-secondary">Nhà vận chuyển:</span> <c:out value="${order.carrierName}"/></div>
                        <c:if test="${not empty order.shipperName}">
                            <div><span class="text-secondary">Shipper:</span> <c:out value="${order.shipperName}"/> – <c:out value="${order.shipperPhone}"/></div>
                        </c:if>
                        <c:if test="${not empty order.deliveredAt}"><div><span class="text-secondary">Đã giao lúc:</span> ${order.deliveredAt}</div></c:if>
                        <c:if test="${not empty order.note}"><div class="mt-2"><span class="text-secondary">Ghi chú:</span> <c:out value="${order.note}"/></div></c:if>
                        <c:if test="${not empty order.cancelReason}"><div class="text-danger mt-2">Lý do hủy: <c:out value="${order.cancelReason}"/></div></c:if>
                    </div>
                </div>

                <div class="card mb-3">
                    <div class="card-header"><h3 class="card-title">Thanh toán</h3></div>
                    <div class="card-body small">
                        ${order.paymentMethodLabel}
                        <c:if test="${order.paid}"><span class="badge bg-success-lt ms-1">Đã thanh toán</span></c:if>
                    </div>
                </div>

                <div class="card">
                    <div class="card-header"><h3 class="card-title">Lịch sử đơn hàng</h3></div>
                    <div class="card-body">
                        <ul class="steps steps-vertical">
                            <c:forEach var="h" items="${order.history}">
                                <li class="step-item">
                                    <div class="h4 m-0">${h.toLabel}</div>
                                    <div class="text-secondary small">${h.time}</div>
                                    <c:if test="${not empty h.note}"><div class="small"><c:out value="${h.note}"/></div></c:if>
                                </li>
                            </c:forEach>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    // "Lý do khác" -> hiện ô nhập
    (function () {
        var select = document.getElementById('cancel-reason');
        var other = document.getElementById('other-reason');
        if (!select) {
            return;
        }
        select.addEventListener('change', function () {
            other.classList.toggle('d-none', select.value !== 'OTHER');
            other.required = select.value === 'OTHER';
        });
    })();
</script>
</body>
</html>
