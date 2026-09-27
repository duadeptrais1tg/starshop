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
                    <span class="badge bg-primary text-white badge-count d-none" id="cart-count">0</span>
                </a>
            </div>
            <%-- Khu vực tài khoản: hiện đang cho Guest; phần đăng nhập (A3) sẽ hiển thị tên user + menu theo role --%>
            <div class="nav-item d-none d-md-flex flex-row align-items-center flex-nowrap ms-2">
                <a href="<c:url value='/auth/login'/>" class="btn btn-outline-primary btn-sm text-nowrap me-2">Đăng nhập</a>
                <a href="<c:url value='/auth/register'/>" class="btn btn-primary btn-sm text-nowrap">Đăng ký</a>
            </div>
            <div class="nav-item d-md-none">
                <a href="<c:url value='/auth/login'/>" class="nav-link px-2" title="Đăng nhập">
                    <i class="ti ti-user fs-2"></i>
                </a>
            </div>
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
                <li class="nav-item d-md-none">
                    <a class="nav-link" href="<c:url value='/auth/register'/>">
                        <span class="nav-link-icon"><i class="ti ti-user-plus"></i></span>
                        <span class="nav-link-title">Đăng ký</span>
                    </a>
                </li>
            </ul>
        </div>
    </div>
</header>
