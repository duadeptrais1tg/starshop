<%@ page pageEncoding="UTF-8" %>
<%-- Phần <head> dùng chung: meta, CSS template, CSS riêng của StarShop --%>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta http-equiv="X-UA-Compatible" content="ie=edge">
<title><sitemesh:write property="title"/> | StarShop</title>
<link rel="stylesheet" href="<c:url value='/vendor-template/tabler/css/tabler.min.css'/>">
<link rel="stylesheet" href="<c:url value='/vendor-template/tabler-icons/tabler-icons.min.css'/>">
<link rel="stylesheet" href="<c:url value='/css/starshop.css'/>">
<%-- CSS/meta riêng của từng trang (nếu trang có khai báo trong <head>) --%>
<sitemesh:write property="head"/>
