<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Chiết khấu app</title>
</head>
<body>
<c:if test="${not empty message}">
    <div class="alert alert-success" role="alert"><c:out value="${message}"/></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
</c:if>

<div class="row row-cards mb-3">
    <%-- Mức mặc định toàn sàn --%>
    <div class="col-lg-5">
        <div class="card mb-3">
            <div class="card-body d-flex align-items-center">
                <span class="avatar avatar-lg bg-primary text-white me-3"><i class="ti ti-percentage fs-2"></i></span>
                <div>
                    <div class="text-secondary">Mức mặc định toàn sàn hôm nay</div>
                    <div class="h1 m-0"><fmt:formatNumber value="${defaultRate}" pattern="#,##0.##"/>%</div>
                </div>
            </div>
        </div>
        <div class="card">
            <div class="card-header"><h3 class="card-title">Đặt mức mặc định mới</h3></div>
            <c:set var="rateFormAction" value="/admin/commissions/default"/>
            <%@ include file="rate-form.jsp" %>
        </div>
    </div>
    <div class="col-lg-7">
        <div class="card h-100">
            <div class="card-header"><h3 class="card-title">Lịch sử mức mặc định</h3></div>
            <c:set var="historyShopId" value="${null}"/>
            <%@ include file="history-table.jsp" %>
        </div>
    </div>
</div>

<%-- Mức riêng từng shop --%>
<div class="card">
    <div class="card-header">
        <h3 class="card-title">Chiết khấu theo shop</h3>
    </div>
    <div class="card-body border-bottom">
        <form method="get" action="<c:url value='/admin/commissions'/>" class="d-flex gap-2">
            <input type="search" name="keyword" class="form-control" maxlength="100" placeholder="Tên shop hoặc email chủ shop"
                   value="<c:out value='${keyword}'/>">
            <button type="submit" class="btn btn-primary">Tìm</button>
        </form>
    </div>
    <c:choose>
        <c:when test="${page.totalElements == 0}">
            <div class="card-body text-secondary">Chưa có shop nào đang hoạt động.</div>
        </c:when>
        <c:otherwise>
            <div class="table-responsive">
                <table class="table table-vcenter card-table">
                    <thead>
                    <tr><th>Shop</th><th>Mức đang áp dụng</th><th class="w-1"></th></tr>
                    </thead>
                    <tbody>
                    <c:forEach var="row" items="${page.content}">
                        <tr>
                            <td>
                                <div class="fw-bold"><c:out value="${row.shopName}"/>
                                    <c:if test="${row.status eq 'SUSPENDED'}"><span class="badge bg-danger-lt ms-1">${row.status.label}</span></c:if>
                                </div>
                                <div class="text-secondary small"><c:out value="${row.ownerEmail}"/></div>
                            </td>
                            <td>
                                <span class="fw-bold"><fmt:formatNumber value="${row.effectiveRate}" pattern="#,##0.##"/>%</span>
                                <span class="badge ${row.custom ? 'bg-purple-lt' : 'bg-secondary-lt'} ms-1">${row.custom ? 'Mức riêng' : 'Mặc định'}</span>
                            </td>
                            <td><a href="<c:url value='/admin/commissions/shops/${row.shopId}'/>" class="btn btn-sm">Thiết lập</a></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
            <%@ include file="/WEB-INF/views/common/pagination.jsp" %>
        </c:otherwise>
    </c:choose>
</div>
</body>
</html>
