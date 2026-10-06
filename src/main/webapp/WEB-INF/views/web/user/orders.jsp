<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Đơn hàng của tôi</title>
</head>
<body>
<div class="row g-3">
    <div class="col-lg-3"><%@ include file="account-nav.jsp" %></div>
    <div class="col-lg-9">
        <div class="card mb-3">
            <div class="card-header">
                <ul class="nav nav-tabs card-header-tabs flex-nowrap overflow-auto">
                    <c:forEach var="t" items="${tabs}">
                        <li class="nav-item">
                            <a href="<c:url value='/user/orders'><c:param name='tab' value='${t}'/></c:url>"
                               class="nav-link text-nowrap ${t eq tab ? 'active' : ''}">
                                ${t.label}
                                <c:if test="${counts[t] > 0}"><span class="badge bg-secondary-lt ms-1">${counts[t]}</span></c:if>
                            </a>
                        </li>
                    </c:forEach>
                </ul>
            </div>
            <c:if test="${page.totalElements == 0}">
                <div class="card-body">
                    <div class="empty">
                        <div class="empty-icon"><i class="ti ti-package-off fs-1 text-secondary"></i></div>
                        <p class="empty-title">Chưa có đơn nào ở mục "${tab.label}"</p>
                        <div class="empty-action"><a href="<c:url value='/products/search'/>" class="btn btn-primary">Mua sắm ngay</a></div>
                    </div>
                </div>
            </c:if>
        </div>

        <c:forEach var="o" items="${page.content}">
            <div class="card mb-3">
                <div class="card-header">
                    <div class="d-flex flex-wrap align-items-center gap-2 w-100">
                        <i class="ti ti-building-store text-secondary"></i>
                        <a href="<c:url value='/shop/${o.shopSlug}'/>" class="fw-bold text-reset"><c:out value="${o.shopName}"/></a>
                        <span class="text-secondary small ms-2"><c:out value="${o.code}"/> · ${o.createdAt}</span>
                        <span class="ms-auto">
                            <c:choose>
                                <c:when test="${o.awaitingPayment}"><span class="badge bg-warning-lt">Chờ thanh toán</span></c:when>
                                <c:otherwise><c:set var="badgeOrderStatus" value="${o.status}"/><%@ include file="/WEB-INF/views/common/order-status-badge.jsp" %></c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                </div>
                <a href="<c:url value='/user/orders/${o.id}'/>" class="card-body d-flex align-items-center gap-3 text-reset text-decoration-none">
                    <c:choose>
                        <c:when test="${not empty o.firstProductImage}">
                            <span class="avatar avatar-lg" style="background-image: url('<c:out value="${o.firstProductImage}"/>')"></span>
                        </c:when>
                        <c:otherwise><span class="avatar avatar-lg bg-primary-lt text-primary"><i class="ti ti-flower"></i></span></c:otherwise>
                    </c:choose>
                    <div class="flex-fill">
                        <div class="fw-semibold"><c:out value="${o.firstProductName}"/> <span class="text-secondary">× ${o.firstProductQuantity}</span></div>
                        <c:if test="${o.moreProducts > 0}"><div class="small text-secondary">và ${o.moreProducts} sản phẩm khác</div></c:if>
                    </div>
                    <div class="text-end">
                        <div class="small text-secondary">Tổng tiền</div>
                        <div class="fw-bold text-primary"><fmt:formatNumber value="${o.total}" pattern="#,##0"/>₫</div>
                    </div>
                </a>
                <div class="card-footer d-flex justify-content-end gap-2">
                    <c:if test="${o.awaitingPayment}">
                        <a href="<c:url value='/payment/vnpay/pay'><c:param name='ref' value='${o.paymentTxnRef}'/></c:url>" class="btn btn-sm btn-primary">
                            <i class="ti ti-credit-card me-1"></i>Tiếp tục thanh toán
                        </a>
                    </c:if>
                    <a href="<c:url value='/user/orders/${o.id}'/>" class="btn btn-sm">Xem chi tiết</a>
                </div>
            </div>
        </c:forEach>

        <c:if test="${page.totalPages > 1}">
            <div class="card"><%@ include file="/WEB-INF/views/common/pagination.jsp" %></div>
        </c:if>
    </div>
</div>
</body>
</html>
