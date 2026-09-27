<%@ page pageEncoding="UTF-8" %>
<%-- Decorator cho trang khách (Guest/User) và trang lỗi: menu ngang --%>
<%@ include file="partials/taglibs.jsp" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="partials/head.jsp" %>
</head>
<body>
<div class="page">
    <%@ include file="partials/web-header.jsp" %>

    <div class="page-wrapper">
        <div class="page-body">
            <div class="container-xl">
                <sitemesh:write property="body"/>
            </div>
        </div>
        <%@ include file="partials/footer.jsp" %>
    </div>
</div>
<%@ include file="partials/scripts.jsp" %>
</body>
</html>
