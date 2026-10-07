<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Khuyến mãi của shop</title>
</head>
<body>
<c:if test="${not empty message}">
    <div class="alert alert-success" role="alert"><c:out value="${message}"/></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
</c:if>

<div class="card">
    <div class="card-body border-bottom">
        <form method="get" action="<c:url value='/vendor/promotions'/>" class="row g-2 align-items-end">
            <div class="col-12 col-md-5">
                <label class="form-label" for="keyword">Tìm kiếm</label>
                <div class="input-icon">
                    <span class="input-icon-addon"><i class="ti ti-search"></i></span>
                    <input type="search" id="keyword" name="keyword" class="form-control" maxlength="100"
                           placeholder="Tên chương trình" value="<c:out value='${keyword}'/>">
                </div>
            </div>
            <div class="col-6 col-md-3">
                <label class="form-label" for="status">Trạng thái</label>
                <select id="status" name="status" class="form-select">
                    <option value="">Tất cả</option>
                    <c:forEach var="s" items="${statuses}">
                        <option value="${s}" ${selectedStatus eq s ? 'selected' : ''}>${s.label}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-6 col-md-2">
                <button type="submit" class="btn btn-primary w-100"><i class="ti ti-filter me-1"></i>Lọc</button>
            </div>
            <div class="col-12 col-md-2">
                <a href="<c:url value='/vendor/promotions/new'/>" class="btn btn-success w-100"><i class="ti ti-plus me-1"></i>Tạo mới</a>
            </div>
        </form>
    </div>

    <c:choose>
        <c:when test="${page.totalElements == 0}">
            <div class="card-body">
                <div class="empty">
                    <div class="empty-icon"><i class="ti ti-discount fs-1 text-secondary"></i></div>
                    <p class="empty-title">Shop chưa có chương trình khuyến mãi</p>
                    <p class="empty-subtitle text-secondary">Tạo mã giảm giá hoặc giảm giá tự động cho sản phẩm của shop.</p>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-responsive">
                <table class="table table-vcenter card-table">
                    <thead>
                    <tr>
                        <th>Chương trình</th>
                        <th>Ưu đãi</th>
                        <th class="d-none d-lg-table-cell">Thời gian</th>
                        <th class="d-none d-md-table-cell">Mã / lượt dùng</th>
                        <th>Trạng thái</th>
                        <th class="w-1"></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="p" items="${page.content}">
                        <tr>
                            <td>
                                <div class="fw-bold"><c:out value="${p.name}"/></div>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${p.percent}">
                                        Giảm <fmt:formatNumber value="${p.discountValue}" pattern="#,##0.##"/>%
                                        <c:if test="${not empty p.maxDiscount}"><div class="text-secondary small">tối đa <fmt:formatNumber value="${p.maxDiscount}" pattern="#,##0"/>₫</div></c:if>
                                    </c:when>
                                    <c:otherwise>Giảm <fmt:formatNumber value="${p.discountValue}" pattern="#,##0"/>₫ phí ship</c:otherwise>
                                </c:choose>
                                <c:if test="${p.minOrderValue > 0}"><div class="text-secondary small">đơn từ <fmt:formatNumber value="${p.minOrderValue}" pattern="#,##0"/>₫</div></c:if>
                            </td>
                            <td class="d-none d-lg-table-cell small">${p.startAt}<br>→ ${p.endAt}</td>
                            <td class="d-none d-md-table-cell">
                                <c:choose>
                                    <c:when test="${not empty p.couponCode}">
                                        <span class="badge bg-primary-lt"><c:out value="${p.couponCode}"/></span>
                                        <div class="text-secondary small">${p.usedCount} / ${empty p.usageLimit ? '∞' : p.usageLimit} lượt</div>
                                    </c:when>
                                    <c:otherwise><span class="text-secondary small">Tự áp dụng</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${p.status eq 'RUNNING'}"><span class="badge bg-success-lt">${p.status.label}</span></c:when>
                                    <c:when test="${p.status eq 'UPCOMING'}"><span class="badge bg-azure-lt">${p.status.label}</span></c:when>
                                    <c:when test="${p.status eq 'INACTIVE'}"><span class="badge bg-danger-lt">${p.status.label}</span></c:when>
                                    <c:otherwise><span class="badge bg-secondary-lt">${p.status.label}</span></c:otherwise>
                                </c:choose>
                                <c:if test="${p.locked}"><i class="ti ti-lock text-secondary ms-1" title="Đã có người dùng, không sửa giá trị"></i></c:if>
                            </td>
                            <td class="text-nowrap">
                                <a href="<c:url value='/vendor/promotions/${p.id}/edit'/>" class="btn btn-sm" title="Sửa"><i class="ti ti-edit"></i></a>
                                <form method="post" action="<c:url value='/vendor/promotions/${p.id}/toggle'/>" class="d-inline">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                    <button type="submit" class="btn btn-sm" title="${p.active ? 'Tắt' : 'Bật'}"><i class="ti ${p.active ? 'ti-player-pause' : 'ti-player-play'}"></i></button>
                                </form>
                                <c:if test="${not p.locked}">
                                    <form method="post" action="<c:url value='/vendor/promotions/${p.id}/delete'/>" class="d-inline"
                                          onsubmit="return confirm('Xóa chương trình này?');">
                                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                        <button type="submit" class="btn btn-sm btn-outline-danger" title="Xóa"><i class="ti ti-trash"></i></button>
                                    </form>
                                </c:if>
                            </td>
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
