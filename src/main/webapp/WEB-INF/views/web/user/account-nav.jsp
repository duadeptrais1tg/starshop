<%@ page pageEncoding="UTF-8" %>
<%-- Menu trái các trang tài khoản (include tĩnh). Mục đang mở được starshop.js tô màu qua data-ss-nav. --%>
<div class="card mb-3">
    <div class="list-group list-group-flush">
        <a href="<c:url value='/user/profile'/>" class="list-group-item list-group-item-action" data-ss-nav>
            <i class="ti ti-user me-2"></i>Hồ sơ của tôi
        </a>
        <a href="<c:url value='/user/addresses'/>" class="list-group-item list-group-item-action" data-ss-nav>
            <i class="ti ti-map-pin me-2"></i>Sổ địa chỉ
        </a>
        <a href="<c:url value='/user/orders'/>" class="list-group-item list-group-item-action" data-ss-nav>
            <i class="ti ti-package me-2"></i>Đơn hàng
        </a>
        <a href="<c:url value='/user/favorites'/>" class="list-group-item list-group-item-action" data-ss-nav>
            <i class="ti ti-heart me-2"></i>Yêu thích
        </a>
        <a href="<c:url value='/user/viewed'/>" class="list-group-item list-group-item-action" data-ss-nav>
            <i class="ti ti-eye me-2"></i>Đã xem
        </a>
    </div>
</div>
