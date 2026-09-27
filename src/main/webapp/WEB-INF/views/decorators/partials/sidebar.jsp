<%@ page pageEncoding="UTF-8" %>
<%-- Sidebar dọc của dashboard; menu thay đổi theo ssArea. Trên điện thoại thu gọn thành nút ☰ --%>
<aside class="navbar navbar-vertical navbar-expand-lg" data-bs-theme="dark">
    <div class="container-fluid">
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#sidebar-menu"
                aria-controls="sidebar-menu" aria-expanded="false" aria-label="Mở menu">
            <span class="navbar-toggler-icon"></span>
        </button>

        <a href="<c:url value='/${ssArea}'/>" class="navbar-brand navbar-brand-autodark ss-brand">
            <i class="ti ti-flower"></i> StarShop
        </a>

        <div class="collapse navbar-collapse" id="sidebar-menu">
            <ul class="navbar-nav pt-lg-3">
                <c:choose>
                    <c:when test="${ssArea == 'admin'}"><%@ include file="menu-admin.jsp" %></c:when>
                    <c:when test="${ssArea == 'manager'}"><%@ include file="menu-manager.jsp" %></c:when>
                    <c:when test="${ssArea == 'vendor'}"><%@ include file="menu-vendor.jsp" %></c:when>
                    <c:when test="${ssArea == 'shipper'}"><%@ include file="menu-shipper.jsp" %></c:when>
                </c:choose>

                <li class="nav-item mt-lg-3">
                    <a class="nav-link" href="<c:url value='/'/>">
                        <span class="nav-link-icon"><i class="ti ti-building-store"></i></span>
                        <span class="nav-link-title">Về trang cửa hàng</span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="<c:url value='/auth/logout'/>">
                        <span class="nav-link-icon"><i class="ti ti-logout"></i></span>
                        <span class="nav-link-title">Đăng xuất</span>
                    </a>
                </li>
            </ul>
        </div>
    </div>
</aside>
