<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Đặt hàng thành công</title>
</head>
<body>
<div class="row justify-content-center">
    <div class="col-lg-8">
        <div class="card">
            <div class="card-body text-center py-5">
                <div class="mb-3"><span class="avatar avatar-xl bg-success-lt text-success rounded-circle"><i class="ti ti-circle-check fs-1"></i></span></div>
                <h1 class="mb-2">Đặt hàng thành công!</h1>
                <p class="text-secondary mb-0">
                    Cảm ơn bạn đã mua hoa tại StarShop. Shop sẽ xác nhận đơn sớm nhất.<br>
                    Phương thức: <strong>${result.paymentMethodLabel}</strong> – vui lòng chuẩn bị
                    <strong class="text-primary"><fmt:formatNumber value="${result.total}" pattern="#,##0"/>₫</strong> khi nhận hàng.
                </p>
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
                            <td><span class="badge bg-warning-lt">${o.statusLabel}</span></td>
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
                <a href="<c:url value='/user/orders'/>" class="btn btn-primary"><i class="ti ti-package me-1"></i>Xem đơn hàng</a>
                <a href="<c:url value='/'/>" class="btn"><i class="ti ti-flower me-1"></i>Tiếp tục mua sắm</a>
            </div>
        </div>
    </div>
</div>
</body>
</html>
