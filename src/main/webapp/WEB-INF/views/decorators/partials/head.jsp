<%@ page pageEncoding="UTF-8" %>
<%-- Phần <head> dùng chung: meta, CSS template, CSS riêng của StarShop --%>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta http-equiv="X-UA-Compatible" content="ie=edge">
<title><sitemesh:write property="title"/> | StarShop</title>
<%-- CSRF token cho AJAX (starshop.js đọc 2 thẻ này để gắn header khi gọi fetch POST/PUT/DELETE) --%>
<c:if test="${not empty _csrf}">
<meta name="_csrf" content="${_csrf.token}">
<meta name="_csrf_header" content="${_csrf.headerName}">
</c:if>
<link rel="stylesheet" href="<c:url value='/vendor-template/tabler/css/tabler.min.css'/>">
<link rel="stylesheet" href="<c:url value='/vendor-template/tabler-icons/tabler-icons.min.css'/>">
<link rel="stylesheet" href="<c:url value='/css/starshop.css'/>">
<%-- CSS/meta riêng của từng trang (nếu trang có khai báo trong <head>) --%>
<sitemesh:write property="head"/>
