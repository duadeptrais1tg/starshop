<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Tổng quan</title>
</head>
<body>
<%@ include file="stats-cards.jsp" %>

<div class="row g-3">
    <div class="col-md-6">
        <div class="card h-100">
            <div class="card-body">
                <div class="text-secondary">Tiền COD đã thu tháng này</div>
                <div class="fw-bold fs-1 text-primary"><fmt:formatNumber value="${stats.codCollectedThisMonth}" pattern="#,##0"/>₫</div>
                <div class="small text-secondary">Nộp lại cho shop / chi nhánh theo quy định.</div>
            </div>
        </div>
    </div>
    <div class="col-md-6">
        <div class="card h-100">
            <div class="card-body d-flex flex-column gap-2">
                <a href="<c:url value='/shipper/orders?status=ASSIGNED'/>" class="btn btn-primary"><i class="ti ti-package me-1"></i>Đơn chờ giao (${stats.waiting})</a>
                <a href="<c:url value='/shipper/orders?status=DELIVERING'/>" class="btn"><i class="ti ti-truck-delivery me-1"></i>Đơn đang giao (${stats.delivering})</a>
                <a href="<c:url value='/shipper/stats'/>" class="btn btn-ghost-secondary"><i class="ti ti-chart-pie me-1"></i>Thống kê theo tháng</a>
            </div>
        </div>
    </div>
</div>
</body>
</html>
