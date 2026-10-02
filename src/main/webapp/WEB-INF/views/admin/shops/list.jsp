<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Duyệt shop</title>
</head>
<body>
<div class="card">
    <div class="card-header">
        <ul class="nav nav-tabs card-header-tabs flex-nowrap overflow-auto">
            <c:forEach var="st" items="${statuses}">
                <li class="nav-item">
                    <a class="nav-link text-nowrap ${selectedStatus eq st.name() ? 'active' : ''}"
                       href="<c:url value='/admin/shops'><c:param name='status' value='${st}'/></c:url>">
                        ${st.label}
                        <span class="badge ${st eq 'PENDING' and counts[st] > 0 ? 'bg-warning text-white' : 'bg-secondary-lt'} ms-1">${counts[st]}</span>
                    </a>
                </li>
            </c:forEach>
            <li class="nav-item">
                <a class="nav-link text-nowrap ${selectedStatus eq 'ALL' ? 'active' : ''}"
                   href="<c:url value='/admin/shops?status=ALL'/>">Tất cả</a>
            </li>
        </ul>
    </div>
    <div class="card-body border-bottom">
        <form method="get" action="<c:url value='/admin/shops'/>" class="d-flex gap-2">
            <input type="hidden" name="status" value="${selectedStatus}">
            <div class="input-icon flex-fill">
                <span class="input-icon-addon"><i class="ti ti-search"></i></span>
                <input type="search" name="keyword" class="form-control" maxlength="100"
                       placeholder="Tên shop, tên hoặc email chủ shop" value="<c:out value='${keyword}'/>">
            </div>
            <button type="submit" class="btn btn-primary">Tìm</button>
        </form>
    </div>

    <c:choose>
        <c:when test="${page.totalElements == 0}">
            <div class="card-body">
                <div class="empty">
                    <div class="empty-icon"><i class="ti ti-building-store fs-1 text-secondary"></i></div>
                    <p class="empty-title">Không có shop nào</p>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-responsive">
                <table class="table table-vcenter card-table">
                    <thead>
                    <tr>
                        <th>Shop</th>
                        <th class="d-none d-md-table-cell">Chủ shop</th>
                        <th class="d-none d-lg-table-cell">Chi nhánh</th>
                        <th class="d-none d-lg-table-cell">Ngày đăng ký</th>
                        <th>Trạng thái</th>
                        <th class="w-1"></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="s" items="${page.content}">
                        <tr>
                            <td>
                                <div class="fw-bold"><c:out value="${s.name}"/></div>
                                <div class="text-secondary small"><c:out value="${s.phone}"/></div>
                            </td>
                            <td class="d-none d-md-table-cell">
                                <div><c:out value="${s.ownerName}"/></div>
                                <div class="text-secondary small"><c:out value="${s.ownerEmail}"/></div>
                            </td>
                            <td class="d-none d-lg-table-cell text-secondary"><c:out value="${empty s.storeName ? '—' : s.storeName}"/></td>
                            <td class="d-none d-lg-table-cell text-secondary">${s.createdAt}</td>
                            <td><c:set var="badgeStatus" value="${s.status}"/><%@ include file="/WEB-INF/views/common/shop-status-badge.jsp" %></td>
                            <td><a href="<c:url value='/admin/shops/${s.id}'/>" class="btn btn-sm">Xem</a></td>
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
