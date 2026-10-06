<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Đơn hàng</title>
</head>
<body>
<div class="card">
    <%-- Tab trạng thái (giữ bộ lọc mã / ngày khi đổi tab) --%>
    <div class="card-header">
        <ul class="nav nav-tabs card-header-tabs flex-nowrap overflow-auto">
            <c:forEach var="t" items="${tabs}">
                <c:url var="tabUrl" value="/vendor/orders">
                    <c:param name="tab" value="${t}"/>
                    <c:if test="${not empty code}"><c:param name="code" value="${code}"/></c:if>
                    <c:if test="${not empty from}"><c:param name="from" value="${from}"/></c:if>
                    <c:if test="${not empty to}"><c:param name="to" value="${to}"/></c:if>
                </c:url>
                <li class="nav-item">
                    <a href="${tabUrl}" class="nav-link text-nowrap ${t eq tab ? 'active' : ''}">
                        ${t.label}
                        <c:if test="${counts[t] > 0}"><span class="badge ${t eq 'NEW' or t eq 'RETURNS' ? 'bg-red text-white' : 'bg-secondary-lt'} ms-1">${counts[t]}</span></c:if>
                    </a>
                </li>
            </c:forEach>
        </ul>
    </div>

    <div class="card-body border-bottom">
        <form method="get" action="<c:url value='/vendor/orders'/>" class="row g-2 align-items-end">
            <input type="hidden" name="tab" value="${tab}">
            <div class="col-12 col-md-4">
                <label class="form-label" for="code">Mã đơn</label>
                <div class="input-icon">
                    <span class="input-icon-addon"><i class="ti ti-search"></i></span>
                    <input type="search" id="code" name="code" class="form-control" maxlength="30"
                           placeholder="Ví dụ SS261004" value="<c:out value='${code}'/>">
                </div>
            </div>
            <div class="col-6 col-md-3">
                <label class="form-label" for="from">Từ ngày</label>
                <input type="date" id="from" name="from" class="form-control" value="${from}">
            </div>
            <div class="col-6 col-md-3">
                <label class="form-label" for="to">Đến ngày</label>
                <input type="date" id="to" name="to" class="form-control" value="${to}">
            </div>
            <div class="col-12 col-md-2 d-flex gap-2">
                <button type="submit" class="btn btn-primary flex-fill"><i class="ti ti-filter me-1"></i>Lọc</button>
                <c:if test="${not empty code or not empty from or not empty to}">
                    <a href="<c:url value='/vendor/orders'><c:param name='tab' value='${tab}'/></c:url>" class="btn" title="Xóa bộ lọc"><i class="ti ti-x"></i></a>
                </c:if>
            </div>
        </form>
    </div>

    <c:choose>
        <c:when test="${page.totalElements == 0}">
            <div class="card-body">
                <div class="empty">
                    <div class="empty-icon"><i class="ti ti-package-off fs-1 text-secondary"></i></div>
                    <p class="empty-title">Không có đơn nào ở mục "${tab.label}"</p>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-responsive">
                <table class="table table-vcenter card-table">
                    <thead>
                    <tr>
                        <th>Mã đơn</th>
                        <th>Người nhận</th>
                        <th class="text-center d-none d-md-table-cell">SL</th>
                        <th class="text-end">Tổng tiền</th>
                        <th class="d-none d-lg-table-cell">Thanh toán</th>
                        <th>Trạng thái</th>
                        <th class="w-1"></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="o" items="${page.content}">
                        <tr>
                            <td>
                                <a href="<c:url value='/vendor/orders/${o.id}'/>" class="fw-bold"><c:out value="${o.code}"/></a>
                                <div class="text-secondary small">${o.createdAt}</div>
                            </td>
                            <td>
                                <c:out value="${o.receiverName}"/>
                                <div class="text-secondary small"><c:out value="${o.receiverPhone}"/></div>
                            </td>
                            <td class="text-center d-none d-md-table-cell">${o.itemCount}</td>
                            <td class="text-end text-nowrap fw-bold"><fmt:formatNumber value="${o.total}" pattern="#,##0"/>₫</td>
                            <td class="d-none d-lg-table-cell small">
                                ${o.paymentMethodLabel}
                                <c:if test="${o.paid}"><span class="badge bg-success-lt ms-1">Đã thanh toán</span></c:if>
                                <div class="text-secondary"><c:out value="${o.carrierName}"/></div>
                            </td>
                            <td><c:set var="badgeOrderStatus" value="${o.status}"/><%@ include file="/WEB-INF/views/common/order-status-badge.jsp" %></td>
                            <td><a href="<c:url value='/vendor/orders/${o.id}'/>" class="btn btn-sm">Xem</a></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
            <%@ include file="/WEB-INF/views/common/pagination.jsp" %>
        </c:otherwise>
    </c:choose>
</div>
</body>
</html>
