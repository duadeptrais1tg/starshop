<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Danh mục</title>
</head>
<body>
<%-- Dùng chung cho Admin và Manager: mọi đường dẫn đi qua ${baseUrl} --%>
<c:if test="${not empty message}">
    <div class="alert alert-success" role="alert"><c:out value="${message}"/></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
</c:if>

<div class="card">
    <div class="card-body border-bottom">
        <form method="get" action="<c:url value='${baseUrl}'/>" class="row g-2 align-items-end">
            <div class="col-12 col-md-5">
                <label class="form-label" for="keyword">Tìm kiếm</label>
                <div class="input-icon">
                    <span class="input-icon-addon"><i class="ti ti-search"></i></span>
                    <input type="search" id="keyword" name="keyword" class="form-control" maxlength="100"
                           placeholder="Tên hoặc slug danh mục" value="<c:out value='${keyword}'/>">
                </div>
            </div>
            <div class="col-6 col-md-3">
                <label class="form-label" for="active">Hiển thị</label>
                <select id="active" name="active" class="form-select">
                    <option value="">Tất cả</option>
                    <option value="true" ${active eq 'true' ? 'selected' : ''}>Đang hiển thị</option>
                    <option value="false" ${active eq 'false' ? 'selected' : ''}>Đang ẩn</option>
                </select>
            </div>
            <div class="col-6 col-md-2">
                <button type="submit" class="btn btn-primary w-100"><i class="ti ti-filter me-1"></i>Lọc</button>
            </div>
            <div class="col-12 col-md-2">
                <a href="<c:url value='${baseUrl}/new'/>" class="btn btn-success w-100"><i class="ti ti-plus me-1"></i>Thêm</a>
            </div>
        </form>
    </div>

    <c:choose>
        <c:when test="${page.totalElements == 0}">
            <div class="card-body">
                <div class="empty">
                    <div class="empty-icon"><i class="ti ti-category fs-1 text-secondary"></i></div>
                    <p class="empty-title">Chưa có danh mục phù hợp</p>
                    <div class="empty-action">
                        <a href="<c:url value='${baseUrl}/new'/>" class="btn btn-primary"><i class="ti ti-plus me-1"></i>Thêm danh mục</a>
                    </div>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-responsive">
                <table class="table table-vcenter card-table">
                    <thead>
                    <tr>
                        <th>Danh mục</th>
                        <th class="d-none d-md-table-cell">Danh mục cha</th>
                        <th class="text-center">Sản phẩm</th>
                        <th>Hiển thị</th>
                        <th class="w-1"></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="cat" items="${page.content}">
                        <tr>
                            <td>
                                <div class="d-flex align-items-center">
                                    <c:choose>
                                        <c:when test="${not empty cat.imageUrl}">
                                            <span class="avatar me-2" style="background-image: url('<c:out value="${cat.imageUrl}"/>')"></span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="avatar bg-primary-lt text-primary me-2"><i class="ti ti-category"></i></span>
                                        </c:otherwise>
                                    </c:choose>
                                    <div>
                                        <div class="fw-bold"><c:out value="${cat.name}"/></div>
                                        <div class="text-secondary small"><c:out value="${cat.slug}"/></div>
                                    </div>
                                </div>
                            </td>
                            <td class="d-none d-md-table-cell text-secondary"><c:out value="${empty cat.parentName ? '—' : cat.parentName}"/></td>
                            <td class="text-center">${cat.productCount}</td>
                            <td>
                                <form method="post" action="<c:url value='${baseUrl}/${cat.id}/toggle'/>" class="m-0">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                    <button type="submit" class="btn btn-sm ${cat.active ? 'btn-success' : 'btn-outline-secondary'}"
                                            title="Bấm để ${cat.active ? 'ẩn' : 'hiện'} danh mục">
                                        <i class="ti ${cat.active ? 'ti-eye' : 'ti-eye-off'} me-1"></i>${cat.active ? 'Đang hiện' : 'Đang ẩn'}
                                    </button>
                                </form>
                            </td>
                            <td class="text-nowrap">
                                <a href="<c:url value='${baseUrl}/${cat.id}/edit'/>" class="btn btn-sm"><i class="ti ti-edit"></i></a>
                                <c:choose>
                                    <c:when test="${cat.deletable}">
                                        <form method="post" action="<c:url value='${baseUrl}/${cat.id}/delete'/>" class="d-inline"
                                              onsubmit="return confirm('Xóa danh mục này? Thao tác không thể hoàn tác.');">
                                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                            <button type="submit" class="btn btn-sm btn-outline-danger" title="Xóa"><i class="ti ti-trash"></i></button>
                                        </form>
                                    </c:when>
                                    <c:otherwise>
                                        <button type="button" class="btn btn-sm btn-outline-danger" disabled
                                                title="Đang có sản phẩm, chỉ có thể ẩn"><i class="ti ti-trash"></i></button>
                                    </c:otherwise>
                                </c:choose>
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
