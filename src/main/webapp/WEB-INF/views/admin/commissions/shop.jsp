<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Chiết khấu của shop</title>
</head>
<body>
<div class="mb-3">
    <a href="<c:url value='/admin/commissions'/>" class="btn btn-sm"><i class="ti ti-arrow-left me-1"></i>Chiết khấu app</a>
</div>
<c:if test="${not empty message}">
    <div class="alert alert-success" role="alert"><c:out value="${message}"/></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
</c:if>

<div class="row row-cards">
    <div class="col-lg-5">
        <div class="card mb-3">
            <div class="card-body">
                <h3 class="m-0"><c:out value="${shop.shopName}"/></h3>
                <div class="text-secondary small mb-3"><c:out value="${shop.ownerEmail}"/></div>
                <div class="text-secondary">Mức đang áp dụng hôm nay</div>
                <div class="h1 m-0">
                    <fmt:formatNumber value="${shop.effectiveRate}" pattern="#,##0.##"/>%
                    <span class="badge ${shop.custom ? 'bg-purple-lt' : 'bg-secondary-lt'} fs-5 align-middle">${shop.custom ? 'Mức riêng' : 'Theo mặc định'}</span>
                </div>
                <c:if test="${not shop.custom}">
                    <div class="text-secondary small mt-1">Mặc định toàn sàn hiện là <fmt:formatNumber value="${defaultRate}" pattern="#,##0.##"/>%.</div>
                </c:if>
            </div>
        </div>
        <div class="card">
            <div class="card-header"><h3 class="card-title">Đặt mức riêng cho shop</h3></div>
            <c:set var="rateFormAction" value="/admin/commissions/shops/${shop.shopId}"/>
            <%@ include file="rate-form.jsp" %>
        </div>
    </div>
    <div class="col-lg-7">
        <div class="card h-100">
            <div class="card-header"><h3 class="card-title">Lịch sử mức riêng</h3></div>
            <c:set var="historyShopId" value="${shop.shopId}"/>
            <%@ include file="history-table.jsp" %>
            <div class="card-footer text-secondary small">Ngoài các giai đoạn có mức riêng, shop áp dụng mức mặc định toàn sàn.</div>
        </div>
    </div>
</div>
</body>
</html>
