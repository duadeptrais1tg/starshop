<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Chi tiết người dùng</title>
</head>
<body>
<div class="mb-3">
    <a href="<c:url value='/admin/users'/>" class="btn btn-sm"><i class="ti ti-arrow-left me-1"></i>Danh sách người dùng</a>
</div>

<c:if test="${not empty message}">
    <div class="alert alert-success" role="alert"><c:out value="${message}"/></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
</c:if>

<c:set var="isSelf" value="${currentUser.id == user.id}"/>
<div class="row row-cards">
    <%-- Thông tin + khóa/mở khóa --%>
    <div class="col-lg-5">
        <div class="card">
            <div class="card-body text-center">
                <span class="avatar avatar-xl bg-primary-lt text-primary mb-3"><i class="ti ti-user fs-1"></i></span>
                <h3 class="m-0"><c:out value="${user.fullName}"/></h3>
                <div class="text-secondary"><c:out value="${user.email}"/></div>
                <div class="mt-2">
                    <c:choose>
                        <c:when test="${user.locked}"><span class="badge bg-danger-lt">${user.statusLabel}</span></c:when>
                        <c:when test="${user.enabled}"><span class="badge bg-success-lt">${user.statusLabel}</span></c:when>
                        <c:otherwise><span class="badge bg-secondary-lt">${user.statusLabel}</span></c:otherwise>
                    </c:choose>
                </div>
            </div>
            <div class="card-body border-top">
                <dl class="row mb-0">
                    <dt class="col-5">Số điện thoại</dt><dd class="col-7"><c:out value="${user.phone}"/></dd>
                    <dt class="col-5">Vai trò</dt>
                    <dd class="col-7"><c:forEach var="r" items="${user.roles}"><span class="badge bg-blue-lt me-1">${r.label}</span></c:forEach></dd>
                    <dt class="col-5">Chi nhánh</dt><dd class="col-7"><c:out value="${empty user.storeName ? '—' : user.storeName}"/></dd>
                    <dt class="col-5">Nhà vận chuyển</dt><dd class="col-7"><c:out value="${empty user.carrierName ? '—' : user.carrierName}"/></dd>
                    <dt class="col-5">Ngày tạo</dt><dd class="col-7">${user.createdAt}</dd>
                </dl>
            </div>
            <div class="card-footer">
                <c:choose>
                    <c:when test="${isSelf}">
                        <div class="text-secondary small"><i class="ti ti-info-circle me-1"></i>Đây là tài khoản của bạn, không thể tự khóa.</div>
                    </c:when>
                    <c:when test="${user.locked}">
                        <form method="post" action="<c:url value='/admin/users/${user.id}/unlock'/>">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <button type="submit" class="btn btn-success w-100"><i class="ti ti-lock-open me-1"></i>Mở khóa tài khoản</button>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <form method="post" action="<c:url value='/admin/users/${user.id}/lock'/>"
                              onsubmit="return confirm('Khóa tài khoản này? Người dùng sẽ bị đăng xuất ngay ở lần thao tác tiếp theo.');">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <button type="submit" class="btn btn-outline-danger w-100"><i class="ti ti-lock me-1"></i>Khóa tài khoản</button>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>

    <%-- Gán vai trò --%>
    <div class="col-lg-7">
        <form class="card" method="post" action="<c:url value='/admin/users/${user.id}/roles'/>">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <div class="card-header"><h3 class="card-title">Phân quyền</h3></div>
            <div class="card-body">
                <div class="mb-3">
                    <label class="form-label">Vai trò</label>
                    <c:forEach var="r" items="${roles}">
                        <c:choose>
                            <c:when test="${r eq 'VENDOR'}">
                                <label class="form-check">
                                    <input class="form-check-input" type="checkbox" disabled ${user.hasRole(r) ? 'checked' : ''}>
                                    <span class="form-check-label">${r.label}
                                        <span class="text-secondary small">(tự động cấp khi shop được duyệt)</span></span>
                                </label>
                            </c:when>
                            <c:otherwise>
                                <label class="form-check">
                                    <input class="form-check-input" type="checkbox" name="roles" value="${r}"
                                           data-role="${r}" ${roleForm.roles.contains(r) ? 'checked' : ''}>
                                    <span class="form-check-label">${r.label}</span>
                                </label>
                            </c:otherwise>
                        </c:choose>
                    </c:forEach>
                </div>
                <div class="mb-3" id="store-group">
                    <label class="form-label required" for="storeId">Chi nhánh quản lý (cho Quản lý chi nhánh)</label>
                    <select id="storeId" name="storeId" class="form-select">
                        <option value="">-- Chọn chi nhánh --</option>
                        <c:forEach var="s" items="${stores}">
                            <option value="${s.id}" ${roleForm.storeId == s.id ? 'selected' : ''}><c:out value="${s.name}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <div class="mb-3" id="carrier-group">
                    <label class="form-label required" for="carrierId">Nhà vận chuyển (cho Người giao hàng)</label>
                    <select id="carrierId" name="carrierId" class="form-select">
                        <option value="">-- Chọn nhà vận chuyển --</option>
                        <c:forEach var="cr" items="${carriers}">
                            <option value="${cr.id}" ${roleForm.carrierId == cr.id ? 'selected' : ''}><c:out value="${cr.name}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <c:if test="${isSelf}">
                    <div class="text-secondary small"><i class="ti ti-info-circle me-1"></i>Bạn không thể tự bỏ quyền Quản trị viên của mình.</div>
                </c:if>
            </div>
            <div class="card-footer text-end">
                <button type="submit" class="btn btn-primary"><i class="ti ti-device-floppy me-1"></i>Lưu phân quyền</button>
            </div>
        </form>
    </div>
</div>

<script>
    // Chỉ hiện ô chọn chi nhánh / nhà vận chuyển khi tick vai trò tương ứng (server vẫn kiểm tra lại)
    (function () {
        function toggle(role, groupId) {
            var box = document.querySelector('input[data-role="' + role + '"]');
            var group = document.getElementById(groupId);
            group.style.display = box && box.checked ? '' : 'none';
        }
        function refresh() {
            toggle('MANAGER', 'store-group');
            toggle('SHIPPER', 'carrier-group');
        }
        document.querySelectorAll('input[data-role]').forEach(function (el) {
            el.addEventListener('change', refresh);
        });
        refresh();
    })();
</script>
</body>
</html>
