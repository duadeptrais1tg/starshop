<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Đơn được giao</title>
</head>
<body>
<div class="card mb-3">
    <div class="card-header">
        <ul class="nav nav-tabs card-header-tabs flex-nowrap overflow-auto">
            <c:forEach var="s" items="${statuses}">
                <li class="nav-item">
                    <a href="<c:url value='/shipper/orders'><c:param name='status' value='${s}'/></c:url>"
                       class="nav-link text-nowrap ${s eq status ? 'active' : ''}">
                        ${s eq 'ASSIGNED' ? 'Chờ giao' : s.label}
                        <c:if test="${counts[s] > 0}"><span class="badge ${s eq 'ASSIGNED' or s eq 'DELIVERING' ? 'bg-red text-white' : 'bg-secondary-lt'} ms-1">${counts[s]}</span></c:if>
                    </a>
                </li>
            </c:forEach>
        </ul>
    </div>
    <c:if test="${page.totalElements == 0}">
        <div class="card-body">
            <div class="empty">
                <div class="empty-icon"><i class="ti ti-truck-off fs-1 text-secondary"></i></div>
                <p class="empty-title">Không có đơn nào</p>
            </div>
        </div>
    </c:if>
</div>

<c:forEach var="o" items="${page.content}">
    <a href="<c:url value='/shipper/orders/${o.id}'/>" class="card card-link mb-2 text-reset text-decoration-none">
        <div class="card-body">
            <div class="d-flex flex-wrap align-items-center gap-2 mb-1">
                <strong><c:out value="${o.orderCode}"/></strong>
                <span class="text-secondary small">· <c:out value="${o.shopName}"/> · ${o.assignedAt}</span>
                <span class="ms-auto">
                    <c:choose>
                        <c:when test="${o.codAmount > 0}"><span class="badge bg-orange-lt">Thu <fmt:formatNumber value="${o.codAmount}" pattern="#,##0"/>₫</span></c:when>
                        <c:otherwise><span class="badge bg-success-lt">Đã thanh toán</span></c:otherwise>
                    </c:choose>
                </span>
            </div>
            <div><i class="ti ti-user me-1 text-secondary"></i><c:out value="${o.receiverName}"/> – <c:out value="${o.receiverPhone}"/></div>
            <div class="text-secondary"><i class="ti ti-map-pin me-1"></i><c:out value="${o.shippingAddress}"/></div>
        </div>
    </a>
</c:forEach>

<c:if test="${page.totalPages > 1}">
    <div class="card"><%@ include file="/WEB-INF/views/common/pagination.jsp" %></div>
</c:if>
</body>
</html>
