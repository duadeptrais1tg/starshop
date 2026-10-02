<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Người dùng</title>
</head>
<body>
<div class="card">
    <div class="card-body border-bottom">
        <form method="get" action="<c:url value='/admin/users'/>" class="row g-2 align-items-end">
            <div class="col-12 col-md-5">
                <label class="form-label" for="keyword">Tìm kiếm</label>
                <div class="input-icon">
                    <span class="input-icon-addon"><i class="ti ti-search"></i></span>
                    <input type="search" id="keyword" name="keyword" class="form-control" maxlength="100"
                           placeholder="Tên, email hoặc số điện thoại" value="<c:out value='${keyword}'/>">
                </div>
            </div>
            <div class="col-6 col-md-2">
                <label class="form-label" for="role">Vai trò</label>
                <select id="role" name="role" class="form-select">
                    <option value="">Tất cả</option>
                    <c:forEach var="r" items="${roles}">
                        <option value="${r}" ${selectedRole eq r ? 'selected' : ''}>${r.label}</option>
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
            <div class="col-12 col-md-3 d-flex gap-2">
                <button type="submit" class="btn btn-primary flex-fill"><i class="ti ti-filter me-1"></i>Lọc</button>
                <a href="<c:url value='/admin/users'/>" class="btn">Xóa lọc</a>
            </div>
        </form>
    </div>

    <c:choose>
        <c:when test="${page.totalElements == 0}">
            <div class="card-body">
                <div class="empty">
                    <div class="empty-icon"><i class="ti ti-user-search fs-1 text-secondary"></i></div>
                    <p class="empty-title">Không tìm thấy người dùng phù hợp</p>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-responsive">
                <table class="table table-vcenter card-table">
                    <thead>
                    <tr>
                        <th>Người dùng</th>
                        <th class="d-none d-md-table-cell">SĐT</th>
                        <th>Vai trò</th>
                        <th class="d-none d-lg-table-cell">Chi nhánh / NVC</th>
                        <th>Trạng thái</th>
                        <th class="d-none d-lg-table-cell">Ngày tạo</th>
                        <th class="w-1"></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="u" items="${page.content}">
                        <tr>
                            <td>
                                <div class="fw-bold"><c:out value="${u.fullName}"/></div>
                                <div class="text-secondary small"><c:out value="${u.email}"/></div>
                            </td>
                            <td class="d-none d-md-table-cell"><c:out value="${u.phone}"/></td>
                            <td>
                                <c:forEach var="r" items="${u.roles}">
                                    <span class="badge bg-blue-lt mb-1">${r.label}</span>
                                </c:forEach>
                            </td>
                            <td class="d-none d-lg-table-cell text-secondary">
                                <c:out value="${not empty u.storeName ? u.storeName : u.carrierName}"/>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${u.locked}"><span class="badge bg-danger-lt">${u.statusLabel}</span></c:when>
                                    <c:when test="${u.enabled}"><span class="badge bg-success-lt">${u.statusLabel}</span></c:when>
                                    <c:otherwise><span class="badge bg-secondary-lt">${u.statusLabel}</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td class="d-none d-lg-table-cell text-secondary">${u.createdAt}</td>
                            <td><a href="<c:url value='/admin/users/${u.id}'/>" class="btn btn-sm">Chi tiết</a></td>
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
