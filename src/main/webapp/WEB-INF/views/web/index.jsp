<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>StarShop</title>
</head>
<body>
    <h1>StarShop is running</h1>
    <%-- Kiểm tra JSTL (jakarta.tags.core) và dữ liệu từ controller --%>
    <c:if test="${not empty serverTime}">
        <p>Thời gian máy chủ: <c:out value="${serverTime}"/></p>
    </c:if>
</body>
</html>
