<%@ page pageEncoding="UTF-8" %>
<%--
  Layout dashboard dùng chung cho admin / vendor / manager / shipper (sidebar dọc).
  Decorator gọi file này phải đặt trước 2 biến: ssArea (admin|vendor|manager|shipper) và ssAreaName.
--%>
<!DOCTYPE html>
<%-- Tabler 1.6: phải có data-bs-navbar-position="vertical" thì sidebar dọc mới hiển thị --%>
<html lang="vi" data-bs-navbar-position="vertical">
<head>
    <%@ include file="head.jsp" %>
</head>
<body>
<div class="page">
    <%@ include file="sidebar.jsp" %>

    <div class="page-wrapper">
        <%-- Thanh trên cùng nằm trong page-wrapper (Tabler ẩn navbar ngang đặt trực tiếp trong .page) --%>
        <%@ include file="dashboard-header.jsp" %>
        <div class="page-header d-print-none">
            <div class="container-xl">
                <div class="page-pretitle"><c:out value="${ssAreaName}"/></div>
                <h2 class="page-title"><sitemesh:write property="title"/></h2>
            </div>
        </div>
        <div class="page-body">
            <div class="container-xl">
                <sitemesh:write property="body"/>
            </div>
        </div>
        <%@ include file="footer.jsp" %>
    </div>
</div>
<%@ include file="scripts.jsp" %>
</body>
</html>
