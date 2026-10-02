<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Tổng quan</title>
</head>
<body>
<div class="row row-deck row-cards mb-3">
    <div class="col-sm-6 col-lg-3">
        <div class="card card-sm">
            <div class="card-body d-flex align-items-center">
                <span class="avatar bg-primary text-white me-3"><i class="ti ti-users"></i></span>
                <div>
                    <div class="h2 m-0"><fmt:formatNumber value="${stats.totalUsers}" pattern="#,##0"/></div>
                    <div class="text-secondary">Người dùng</div>
                </div>
            </div>
        </div>
    </div>
    <div class="col-sm-6 col-lg-3">
        <div class="card card-sm">
            <div class="card-body d-flex align-items-center">
                <span class="avatar bg-green text-white me-3"><i class="ti ti-building-store"></i></span>
                <div>
                    <div class="h2 m-0"><fmt:formatNumber value="${stats.approvedShops}" pattern="#,##0"/></div>
                    <div class="text-secondary">
                        Shop hoạt động
                        <c:if test="${stats.pendingShops > 0}">
                            · <a href="<c:url value='/admin/shops?status=PENDING'/>" class="text-warning">${stats.pendingShops} chờ duyệt</a>
                        </c:if>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="col-sm-6 col-lg-3">
        <div class="card card-sm">
            <div class="card-body d-flex align-items-center">
                <span class="avatar bg-azure text-white me-3"><i class="ti ti-package"></i></span>
                <div>
                    <div class="h2 m-0"><fmt:formatNumber value="${stats.totalOrders}" pattern="#,##0"/></div>
                    <div class="text-secondary">Đơn hàng</div>
                </div>
            </div>
        </div>
    </div>
    <div class="col-sm-6 col-lg-3">
        <div class="card card-sm">
            <div class="card-body d-flex align-items-center">
                <span class="avatar bg-orange text-white me-3"><i class="ti ti-currency-dong"></i></span>
                <div>
                    <div class="h2 m-0"><fmt:formatNumber value="${stats.revenue}" pattern="#,##0"/> ₫</div>
                    <div class="text-secondary">Doanh thu (đơn đã giao)</div>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="card">
    <div class="card-header">
        <h3 class="card-title">Shop chờ duyệt lâu nhất</h3>
        <div class="card-actions">
            <a href="<c:url value='/admin/shops?status=PENDING'/>" class="btn btn-sm">Xem tất cả</a>
        </div>
    </div>
    <c:choose>
        <c:when test="${empty pendingShops}">
            <div class="card-body">
                <div class="empty py-3">
                    <div class="empty-icon"><i class="ti ti-circle-check fs-1 text-success"></i></div>
                    <p class="empty-title">Không có shop nào chờ duyệt</p>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div class="list-group list-group-flush">
                <c:forEach var="s" items="${pendingShops}">
                    <a href="<c:url value='/admin/shops/${s.id}'/>" class="list-group-item list-group-item-action d-flex align-items-center">
                        <div class="flex-fill">
                            <div class="fw-bold"><c:out value="${s.name}"/></div>
                            <div class="text-secondary small"><c:out value="${s.ownerName}"/> · <c:out value="${s.ownerEmail}"/></div>
                        </div>
                        <div class="text-secondary small text-nowrap">${s.createdAt}</div>
                    </a>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</div>
</body>
</html>
