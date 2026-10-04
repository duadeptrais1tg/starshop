<%@ page pageEncoding="UTF-8" %>
<%-- Header trang khách (Guest/User): logo, tìm kiếm, giỏ hàng, tài khoản, menu chính --%>
<header class="navbar navbar-expand-md d-print-none">
    <div class="container-xl">
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#web-menu"
                aria-controls="web-menu" aria-expanded="false" aria-label="Mở menu">
            <span class="navbar-toggler-icon"></span>
        </button>

        <a href="<c:url value='/'/>" class="navbar-brand ss-brand me-md-3">
            <i class="ti ti-flower"></i> StarShop
        </a>

        <div class="navbar-nav flex-row order-md-last ss-header-actions">
            <div class="nav-item">
                <a href="<c:url value='/cart'/>" class="nav-link px-2" title="Giỏ hàng">
                    <i class="ti ti-shopping-cart fs-2"></i>
                    <span class="badge bg-primary text-white badge-count ${cartCount > 0 ? '' : 'd-none'}" id="cart-count">${cartCount > 0 ? cartCount : 0}</span>
                </a>
            </div>
            <%-- Khu vực tài khoản: currentUser do CurrentUserAdvice đưa vào (null = khách) --%>
            <c:choose>
                <c:when test="${not empty currentUser}">
                    <div class="nav-item dropdown ms-1">
                        <a href="#" class="nav-link d-flex lh-1 text-reset p-0 ps-1" data-bs-toggle="dropdown"
                           aria-label="Tài khoản" aria-expanded="false">
                            <c:choose>
                                <c:when test="${not empty currentUser.avatarUrl}">
                                    <span class="avatar avatar-sm" style="background-image: url('<c:out value="${currentUser.avatarUrl}"/>')"></span>
                                </c:when>
                                <c:otherwise>
                                    <span class="avatar avatar-sm bg-primary-lt text-primary fw-bold"><c:out value="${currentUser.initial}"/></span>
                                </c:otherwise>
                            </c:choose>
                            <span class="d-none d-lg-block ps-2 text-truncate" style="max-width:140px">
                                <c:out value="${currentUser.fullName}"/>
                            </span>
                        </a>
                        <div class="dropdown-menu dropdown-menu-end dropdown-menu-arrow">
                            <div class="dropdown-header">
                                <div class="fw-bold text-body"><c:out value="${currentUser.fullName}"/></div>
                                <div class="small text-secondary"><c:out value="${currentUser.email}"/></div>
                            </div>
                            <a href="<c:url value='/user/profile'/>" class="dropdown-item"><i class="ti ti-user me-2"></i>Hồ sơ của tôi</a>
                            <a href="<c:url value='/user/addresses'/>" class="dropdown-item"><i class="ti ti-map-pin me-2"></i>Sổ địa chỉ</a>
                            <a href="<c:url value='/user/orders'/>" class="dropdown-item"><i class="ti ti-package me-2"></i>Đơn hàng</a>
                            <a href="<c:url value='/user/favorites'/>" class="dropdown-item"><i class="ti ti-heart me-2"></i>Yêu thích</a>
                            <a href="<c:url value='/user/viewed'/>" class="dropdown-item"><i class="ti ti-eye me-2"></i>Đã xem</a>
                            <c:if test="${currentUser.admin or currentUser.manager or currentUser.vendor or currentUser.shipper}">
                                <div class="dropdown-divider"></div>
                                <c:if test="${currentUser.admin}">
                                    <a href="<c:url value='/admin'/>" class="dropdown-item"><i class="ti ti-shield-lock me-2"></i>Trang quản trị</a>
                                </c:if>
                                <c:if test="${currentUser.manager}">
                                    <a href="<c:url value='/manager'/>" class="dropdown-item"><i class="ti ti-building me-2"></i>Quản lý chi nhánh</a>
                                </c:if>
                                <c:if test="${currentUser.vendor}">
                                    <a href="<c:url value='/vendor'/>" class="dropdown-item"><i class="ti ti-building-store me-2"></i>Kênh người bán</a>
                                </c:if>
                                <c:if test="${currentUser.shipper}">
                                    <a href="<c:url value='/shipper'/>" class="dropdown-item"><i class="ti ti-truck-delivery me-2"></i>Kênh giao hàng</a>
                                </c:if>
                            </c:if>
                            <div class="dropdown-divider"></div>
                            <%@ include file="logout-form.jsp" %>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="nav-item d-none d-md-flex flex-row align-items-center flex-nowrap ms-2">
                        <a href="<c:url value='/auth/login'/>" class="btn btn-outline-primary btn-sm text-nowrap me-2">Đăng nhập</a>
                        <a href="<c:url value='/auth/register'/>" class="btn btn-primary btn-sm text-nowrap">Đăng ký</a>
                    </div>
                    <div class="nav-item d-md-none">
                        <a href="<c:url value='/auth/login'/>" class="nav-link px-2" title="Đăng nhập">
                            <i class="ti ti-user fs-2"></i>
                        </a>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

        <div class="collapse navbar-collapse" id="web-menu">
            <form action="<c:url value='/products/search'/>" method="get" role="search"
                  class="ss-search flex-grow-1 my-2 my-md-0 me-md-3">
                <div class="input-icon">
                    <span class="input-icon-addon"><i class="ti ti-search"></i></span>
                    <input type="search" name="keyword" class="form-control" placeholder="Tìm hoa, shop..."
                           aria-label="Tìm kiếm" value="<c:out value='${param.keyword}'/>">
                </div>
            </form>
            <ul class="navbar-nav">
                <li class="nav-item">
                    <a class="nav-link" href="<c:url value='/'/>" data-ss-nav>
                        <span class="nav-link-icon"><i class="ti ti-home"></i></span>
                        <span class="nav-link-title">Trang chủ</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="<c:url value='/products/search'/>" data-ss-nav>
                        <span class="nav-link-icon"><i class="ti ti-flower"></i></span>
                        <span class="nav-link-title">Sản phẩm</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="<c:url value='/user/favorites'/>" data-ss-nav>
                        <span class="nav-link-icon"><i class="ti ti-heart"></i></span>
                        <span class="nav-link-title">Yêu thích</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="<c:url value='/user/orders'/>" data-ss-nav>
                        <span class="nav-link-icon"><i class="ti ti-package"></i></span>
                        <span class="nav-link-title">Đơn hàng</span>
                    </a>
                </li>
                <c:if test="${empty currentUser}">
                    <li class="nav-item d-md-none">
                        <a class="nav-link" href="<c:url value='/auth/register'/>">
                            <span class="nav-link-icon"><i class="ti ti-user-plus"></i></span>
                            <span class="nav-link-title">Đăng ký</span>
                        </a>
                    </li>
                </c:if>
            </ul>
        </div>
    </div>
</header>
