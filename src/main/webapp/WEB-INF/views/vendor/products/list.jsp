<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Sản phẩm</title>
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
        <form method="get" action="<c:url value='/vendor/products'/>" class="row g-2 align-items-end">
            <div class="col-12 col-md-4">
                <label class="form-label" for="keyword">Tìm kiếm</label>
                <div class="input-icon">
                    <span class="input-icon-addon"><i class="ti ti-search"></i></span>
                    <input type="search" id="keyword" name="keyword" class="form-control" maxlength="100"
                           placeholder="Tên sản phẩm" value="<c:out value='${keyword}'/>">
                </div>
            </div>
            <div class="col-6 col-md-3">
                <label class="form-label" for="categoryId">Danh mục</label>
                <select id="categoryId" name="categoryId" class="form-select">
                    <option value="">Tất cả</option>
                    <c:forEach var="cat" items="${categories}">
                        <option value="${cat.id}" ${cat.id == categoryId ? 'selected' : ''}><c:out value="${cat.name}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-6 col-md-2">
                <label class="form-label" for="status">Trạng thái</label>
                <select id="status" name="status" class="form-select">
                    <option value="">Tất cả</option>
                    <c:forEach var="s" items="${statuses}">
                        <option value="${s}" ${selectedStatus eq s ? 'selected' : ''}>${s.label}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-6 col-md-1">
                <button type="submit" class="btn btn-primary w-100" title="Lọc"><i class="ti ti-filter"></i></button>
            </div>
            <div class="col-6 col-md-2">
                <a href="<c:url value='/vendor/products/new'/>" class="btn btn-success w-100"><i class="ti ti-plus me-1"></i>Thêm</a>
            </div>
        </form>
    </div>

    <c:choose>
        <c:when test="${page.totalElements == 0}">
            <div class="card-body">
                <div class="empty">
                    <div class="empty-icon"><i class="ti ti-flower fs-1 text-secondary"></i></div>
                    <c:choose>
                        <c:when test="${totalProducts == 0}">
                            <p class="empty-title">Shop chưa có sản phẩm nào</p>
                            <div class="empty-action"><a href="<c:url value='/vendor/products/new'/>" class="btn btn-primary"><i class="ti ti-plus me-1"></i>Thêm sản phẩm đầu tiên</a></div>
                        </c:when>
                        <c:otherwise>
                            <p class="empty-title">Không có sản phẩm phù hợp</p>
                            <div class="empty-action"><a href="<c:url value='/vendor/products'/>" class="btn">Xóa bộ lọc</a></div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-responsive">
                <table class="table table-vcenter card-table">
                    <thead>
                    <tr>
                        <th>Sản phẩm</th>
                        <th class="text-end">Giá</th>
                        <th class="text-end">Tồn kho</th>
                        <th class="text-end d-none d-md-table-cell">Đã bán</th>
                        <th>Trạng thái</th>
                        <th class="w-1"></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="p" items="${page.content}">
                        <tr>
                            <td>
                                <div class="d-flex align-items-center gap-2">
                                    <c:choose>
                                        <c:when test="${not empty p.imageUrl}">
                                            <span class="avatar avatar-md" style="background-image: url('<c:out value="${p.imageUrl}"/>')"></span>
                                        </c:when>
                                        <c:otherwise><span class="avatar avatar-md bg-primary-lt text-primary"><i class="ti ti-flower"></i></span></c:otherwise>
                                    </c:choose>
                                    <div class="text-truncate" style="max-width: 280px">
                                        <a href="<c:url value='/vendor/products/${p.id}/edit'/>" class="fw-bold text-reset"><c:out value="${p.name}"/></a>
                                        <div class="text-secondary small"><c:out value="${p.categoryName}"/>
                                            <c:if test="${p.reviewCount > 0}"> · ★ <fmt:formatNumber value="${p.ratingAvg}" maxFractionDigits="1"/> (${p.reviewCount})</c:if>
                                        </div>
                                    </div>
                                </div>
                            </td>
                            <td class="text-end text-nowrap">
                                <fmt:formatNumber value="${p.price}" pattern="#,##0"/>₫
                                <c:if test="${not empty p.originalPrice}"><div class="small text-secondary"><del><fmt:formatNumber value="${p.originalPrice}" pattern="#,##0"/>₫</del></div></c:if>
                            </td>
                            <td class="text-end ${p.outOfStock ? 'text-danger fw-bold' : ''}"><fmt:formatNumber value="${p.stock}"/></td>
                            <td class="text-end d-none d-md-table-cell"><fmt:formatNumber value="${p.soldCount}"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${not p.active}"><span class="badge bg-secondary-lt">Ngừng bán</span></c:when>
                                    <c:when test="${p.outOfStock}"><span class="badge bg-danger-lt">Hết hàng</span></c:when>
                                    <c:otherwise><span class="badge bg-success-lt">Đang bán</span></c:otherwise>
                                </c:choose>
                                <c:if test="${p.categoryHidden}"><i class="ti ti-eye-off text-warning ms-1" title="Danh mục đang bị ẩn, khách không thấy sản phẩm"></i></c:if>
                            </td>
                            <td class="text-nowrap">
                                <a href="<c:url value='/vendor/products/${p.id}/edit'/>" class="btn btn-sm" title="Sửa"><i class="ti ti-edit"></i></a>
                                <form method="post" action="<c:url value='/vendor/products/${p.id}/toggle'/>" class="d-inline">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                    <button type="submit" class="btn btn-sm" title="${p.active ? 'Ngừng bán' : 'Bán lại'}"><i class="ti ${p.active ? 'ti-eye-off' : 'ti-eye'}"></i></button>
                                </form>
                                <form method="post" action="<c:url value='/vendor/products/${p.id}/delete'/>" class="d-inline"
                                      onsubmit="return confirm('Xóa sản phẩm này? Sản phẩm đã có đơn hàng sẽ chỉ được ẩn.');">
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
</body>
</html>
