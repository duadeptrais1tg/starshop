<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>${result.failed ? 'Thanh toán không thành công' : (result.awaitingPayment ? 'Chờ thanh toán' : 'Đặt hàng thành công')}</title>
</head>
<body>
<div class="row justify-content-center">
    <div class="col-lg-8">
        <div class="card">
            <div class="card-body text-center py-5">
                <c:choose>
                    <%-- Thanh toán online thất bại / bị hủy / quá hạn --%>
                    <c:when test="${result.failed}">
                        <div class="mb-3"><span class="avatar avatar-xl bg-danger-lt text-danger rounded-circle"><i class="ti ti-circle-x fs-1"></i></span></div>
                        <h1 class="mb-2">Thanh toán không thành công</h1>
                        <p class="text-secondary mb-0">
                            <c:out value="${result.cancelReason}"/>.<br>
                            Các đơn bên dưới đã được hủy, bạn không bị trừ tiền. Sản phẩm đã được trả lại giỏ hàng để bạn đặt lại.
                        </p>
                    </c:when>
                    <%-- Chưa có kết quả từ VNPAY (khách đóng trang thanh toán...) --%>
                    <c:when test="${result.awaitingPayment}">
                        <div class="mb-3"><span class="avatar avatar-xl bg-warning-lt text-warning rounded-circle"><i class="ti ti-clock fs-1"></i></span></div>
                        <h1 class="mb-2">Đơn hàng đang chờ thanh toán</h1>
                        <p class="text-secondary mb-0">
                            Vui lòng hoàn tất thanh toán <strong class="text-primary"><fmt:formatNumber value="${result.total}" pattern="#,##0"/>₫</strong>
                            qua ${result.paymentMethodLabel}. Quá hạn thanh toán, đơn sẽ tự hủy.
                        </p>
                    </c:when>
                    <c:otherwise>
                        <div class="mb-3"><span class="avatar avatar-xl bg-success-lt text-success rounded-circle"><i class="ti ti-circle-check fs-1"></i></span></div>
                        <h1 class="mb-2">Đặt hàng thành công!</h1>
                        <p class="text-secondary mb-0">
                            Cảm ơn bạn đã mua hoa tại StarShop. Shop sẽ xác nhận đơn sớm nhất.<br>
                            <c:choose>
                                <c:when test="${result.paid}">
                                    Đã thanh toán <strong class="text-primary"><fmt:formatNumber value="${result.total}" pattern="#,##0"/>₫</strong>
                                    qua <strong>${result.paymentMethodLabel}</strong>.
                                </c:when>
                                <c:otherwise>
                                    Phương thức: <strong>${result.paymentMethodLabel}</strong> – vui lòng chuẩn bị
                                    <strong class="text-primary"><fmt:formatNumber value="${result.total}" pattern="#,##0"/>₫</strong> khi nhận hàng.
                                </c:otherwise>
                            </c:choose>
                        </p>
                    </c:otherwise>
                </c:choose>
            </div>
            <div class="table-responsive">
                <table class="table table-vcenter card-table">
                    <thead>
                    <tr><th>Mã đơn</th><th>Shop</th><th class="text-center">Số lượng</th><th class="text-end">Tổng tiền</th><th>Trạng thái</th></tr>
                    </thead>
                    <tbody>
                    <c:forEach var="o" items="${result.orders}">
                        <tr>
                            <td class="fw-bold"><c:out value="${o.code}"/></td>
                            <td><c:out value="${o.shopName}"/></td>
                            <td class="text-center">${o.itemCount}</td>
                            <td class="text-end"><fmt:formatNumber value="${o.total}" pattern="#,##0"/>₫</td>
                            <td><span class="badge ${result.failed ? 'bg-danger-lt' : 'bg-warning-lt'}">${o.statusLabel}</span></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
            <div class="card-body border-top">
                <div class="text-secondary small mb-1">Giao đến</div>
                <div><strong><c:out value="${result.receiverName}"/></strong> – <c:out value="${result.receiverPhone}"/></div>
                <div class="text-secondary"><c:out value="${result.shippingAddress}"/></div>
            </div>
            <div class="card-footer d-flex flex-wrap justify-content-center gap-2">
                <c:choose>
                    <c:when test="${result.failed}">
                        <a href="<c:url value='/cart'/>" class="btn btn-primary"><i class="ti ti-shopping-cart me-1"></i>Về giỏ hàng để đặt lại</a>
                    </c:when>
                    <c:when test="${result.awaitingPayment}">
                        <a href="<c:url value='/payment/vnpay/pay'><c:param name='ref' value='${result.txnRef}'/></c:url>" class="btn btn-primary">
                            <i class="ti ti-credit-card me-1"></i>Tiếp tục thanh toán
                        </a>
                    </c:when>
                    <c:otherwise>
                        <a href="<c:url value='/user/orders'/>" class="btn btn-primary"><i class="ti ti-package me-1"></i>Xem đơn hàng</a>
                    </c:otherwise>
                </c:choose>
                <a href="<c:url value='/'/>" class="btn"><i class="ti ti-flower me-1"></i>Tiếp tục mua sắm</a>
            </div>
        </div>
    </div>
</div>
</body>
</html>
