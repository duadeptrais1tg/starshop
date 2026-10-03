<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Nhà vận chuyển</title>
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
        <form method="get" action="<c:url value='/admin/carriers'/>" class="row g-2 align-items-end">
            <div class="col-12 col-md-5">
                <label class="form-label" for="keyword">Tìm kiếm</label>
                <div class="input-icon">
                    <span class="input-icon-addon"><i class="ti ti-search"></i></span>
                    <input type="search" id="keyword" name="keyword" class="form-control" maxlength="100"
                           placeholder="Tên nhà vận chuyển" value="<c:out value='${keyword}'/>">
                </div>
            </div>
            <div class="col-6 col-md-3">
                <label class="form-label" for="active">Trạng thái</label>
                <select id="active" name="active" class="form-select">
                    <option value="">Tất cả</option>
                    <option value="true" ${active eq 'true' ? 'selected' : ''}>Đang hoạt động</option>
                    <option value="false" ${active eq 'false' ? 'selected' : ''}>Ngừng hoạt động</option>
                </select>
            </div>
            <div class="col-6 col-md-2">
                <button type="submit" class="btn btn-primary w-100"><i class="ti ti-filter me-1"></i>Lọc</button>
            </div>
            <div class="col-12 col-md-2">
                <a href="<c:url value='/admin/carriers/new'/>" class="btn btn-success w-100"><i class="ti ti-plus me-1"></i>Thêm</a>
            </div>
        </form>
    </div>

    <c:choose>
        <c:when test="${page.totalElements == 0}">
            <div class="card-body">
                <div class="empty">
                    <div class="empty-icon"><i class="ti ti-truck fs-1 text-secondary"></i></div>
                    <p class="empty-title">Chưa có nhà vận chuyển phù hợp</p>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-responsive">
                <table class="table table-vcenter card-table">
                    <thead>
                    <tr>
                        <th>Nhà vận chuyển</th>
                        <th class="text-end">Phí vận chuyển</th>
                        <th class="text-center d-none d-md-table-cell">Shipper</th>
                        <th>Trạng thái</th>
                        <th class="w-1"></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="cr" items="${page.content}">
                        <tr>
                            <td class="fw-bold"><c:out value="${cr.name}"/></td>
                            <td class="text-end"><fmt:formatNumber value="${cr.shippingFee}" pattern="#,##0"/>₫</td>
                            <td class="text-center d-none d-md-table-cell">${cr.shipperCount}</td>
                            <td>
                                <form method="post" action="<c:url value='/admin/carriers/${cr.id}/toggle'/>" class="m-0">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                    <button type="submit" class="btn btn-sm ${cr.active ? 'btn-success' : 'btn-outline-secondary'}"
                                            title="Bấm để ${cr.active ? 'tắt' : 'bật'}">
                                        ${cr.active ? 'Đang hoạt động' : 'Ngừng hoạt động'}
                                    </button>
                                </form>
                            </td>
                            <td class="text-nowrap">
                                <a href="<c:url value='/admin/carriers/${cr.id}/edit'/>" class="btn btn-sm" title="Sửa"><i class="ti ti-edit"></i></a>
                                <form method="post" action="<c:url value='/admin/carriers/${cr.id}/delete'/>" class="d-inline"
                                      onsubmit="return confirm('Xóa nhà vận chuyển này?');">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                    <button type="submit" class="btn btn-sm btn-outline-danger" title="Xóa"><i class="ti ti-trash"></i></button>
                                </form>
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
<p class="text-secondary small mt-2"><i class="ti ti-info-circle me-1"></i>Đổi phí vận chuyển chỉ áp dụng cho đơn mới; đơn đã đặt giữ nguyên phí lúc đặt.</p>
</body>
</html>
