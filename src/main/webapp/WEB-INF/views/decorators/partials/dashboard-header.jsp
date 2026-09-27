<%@ page pageEncoding="UTF-8" %>
<%-- Thanh trên cùng của dashboard (màn hình lớn): thông báo + tài khoản --%>
<header class="navbar navbar-expand-md d-none d-lg-flex d-print-none">
    <div class="container-xl">
        <div class="navbar-nav flex-row order-md-last">
            <div class="nav-item me-2">
                <%-- Chuông thông báo realtime sẽ làm ở chức năng WebSocket (B12) --%>
                <a href="#" class="nav-link px-0" title="Thông báo">
                    <i class="ti ti-bell fs-2"></i>
                </a>
            </div>
            <div class="nav-item dropdown">
                <a href="#" class="nav-link d-flex lh-1 text-reset p-0" data-bs-toggle="dropdown" aria-label="Tài khoản">
                    <span class="avatar avatar-sm"><i class="ti ti-user"></i></span>
                    <div class="d-none d-xl-block ps-2">
                        <%-- Tên người đăng nhập sẽ hiển thị khi có chức năng đăng nhập (A3) --%>
                        <div>Tài khoản</div>
                        <div class="mt-1 small text-secondary"><c:out value="${ssAreaName}"/></div>
                    </div>
                </a>
                <div class="dropdown-menu dropdown-menu-end dropdown-menu-arrow">
                    <a href="<c:url value='/user/profile'/>" class="dropdown-item">Hồ sơ</a>
                    <div class="dropdown-divider"></div>
                    <a href="<c:url value='/auth/logout'/>" class="dropdown-item">Đăng xuất</a>
                </div>
            </div>
        </div>
    </div>
</header>
