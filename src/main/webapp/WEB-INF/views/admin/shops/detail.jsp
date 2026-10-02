<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Chi tiết shop</title>
</head>
<body>
<div class="mb-3">
    <a href="<c:url value='/admin/shops'><c:param name='status' value='${shop.status}'/></c:url>" class="btn btn-sm">
        <i class="ti ti-arrow-left me-1"></i>Danh sách shop
    </a>
</div>

<c:if test="${not empty message}">
    <div class="alert alert-success" role="alert"><c:out value="${message}"/></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
</c:if>

<div class="row row-cards">
    <div class="col-lg-7">
        <div class="card">
            <div class="card-body d-flex align-items-center">
                <c:choose>
                    <c:when test="${not empty shop.logoUrl}">
                        <span class="avatar avatar-lg me-3" style="background-image: url('<c:out value="${shop.logoUrl}"/>')"></span>
                    </c:when>
                    <c:otherwise>
                        <span class="avatar avatar-lg bg-primary-lt text-primary me-3"><i class="ti ti-building-store fs-2"></i></span>
                    </c:otherwise>
                </c:choose>
                <div class="flex-fill">
                    <h3 class="m-0"><c:out value="${shop.name}"/></h3>
                    <div class="text-secondary small">/shop/<c:out value="${shop.slug}"/></div>
                </div>
                <c:set var="badgeStatus" value="${shop.status}"/><%@ include file="/WEB-INF/views/common/shop-status-badge.jsp" %>
            </div>
            <div class="card-body border-top">
                <dl class="row mb-0">
                    <dt class="col-sm-4">Chủ shop</dt>
                    <dd class="col-sm-8">
                        <a href="<c:url value='/admin/users/${shop.ownerId}'/>"><c:out value="${shop.ownerName}"/></a>
                        <span class="text-secondary">· <c:out value="${shop.ownerEmail}"/></span>
                    </dd>
                    <dt class="col-sm-4">Số điện thoại</dt><dd class="col-sm-8"><c:out value="${shop.phone}"/></dd>
                    <dt class="col-sm-4">Địa chỉ lấy hàng</dt><dd class="col-sm-8"><c:out value="${shop.pickupAddress}"/></dd>
                    <dt class="col-sm-4">Chi nhánh</dt><dd class="col-sm-8"><c:out value="${empty shop.storeName ? 'Chưa gán' : shop.storeName}"/></dd>
                    <dt class="col-sm-4">Ngày đăng ký</dt><dd class="col-sm-8">${shop.createdAt}</dd>
                    <dt class="col-sm-4">Mô tả</dt><dd class="col-sm-8 text-secondary"><c:out value="${empty shop.description ? '—' : shop.description}"/></dd>
                    <c:if test="${not empty shop.statusReason}">
                        <dt class="col-sm-4">Lý do</dt><dd class="col-sm-8 text-danger"><c:out value="${shop.statusReason}"/></dd>
                    </c:if>
                </dl>
            </div>
        </div>
    </div>

    <%-- Hành động theo trạng thái hiện tại --%>
    <div class="col-lg-5">
        <c:choose>
            <c:when test="${shop.status eq 'PENDING'}">
                <form class="card mb-3" method="post" action="<c:url value='/admin/shops/${shop.id}/approve'/>">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <div class="card-header"><h3 class="card-title text-success">Duyệt shop</h3></div>
                    <div class="card-body">
                        <label class="form-label required" for="storeId">Gán vào chi nhánh</label>
                        <select id="storeId" name="storeId" class="form-select" required>
                            <option value="">-- Chọn chi nhánh --</option>
                            <c:forEach var="s" items="${stores}">
                                <option value="${s.id}"><c:out value="${s.name}"/></option>
                            </c:forEach>
                        </select>
                        <small class="form-hint">Chủ shop sẽ được cấp quyền Người bán.</small>
                    </div>
                    <div class="card-footer text-end">
                        <button type="submit" class="btn btn-success"><i class="ti ti-check me-1"></i>Duyệt</button>
                    </div>
                </form>
                <form class="card" method="post" action="<c:url value='/admin/shops/${shop.id}/reject'/>"
                      onsubmit="return confirm('Từ chối shop này?');">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <div class="card-header"><h3 class="card-title text-danger">Từ chối</h3></div>
                    <div class="card-body">
                        <label class="form-label required" for="reject-reason">Lý do từ chối</label>
                        <textarea id="reject-reason" name="reason" class="form-control" rows="3" maxlength="255" required
                                  placeholder="Ví dụ: Thông tin địa chỉ lấy hàng chưa rõ ràng"></textarea>
                    </div>
                    <div class="card-footer text-end">
                        <button type="submit" class="btn btn-outline-danger"><i class="ti ti-x me-1"></i>Từ chối</button>
                    </div>
                </form>
            </c:when>
            <c:when test="${shop.status eq 'APPROVED'}">
                <form class="card" method="post" action="<c:url value='/admin/shops/${shop.id}/suspend'/>"
                      onsubmit="return confirm('Đình chỉ shop này?');">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <div class="card-header"><h3 class="card-title text-danger">Đình chỉ shop</h3></div>
                    <div class="card-body">
                        <label class="form-label required" for="suspend-reason">Lý do đình chỉ</label>
                        <textarea id="suspend-reason" name="reason" class="form-control" rows="3" maxlength="255" required
                                  placeholder="Ví dụ: Nhiều khiếu nại về chất lượng hoa"></textarea>
                    </div>
                    <div class="card-footer text-end">
                        <button type="submit" class="btn btn-danger"><i class="ti ti-ban me-1"></i>Đình chỉ</button>
                    </div>
                </form>
            </c:when>
            <c:when test="${shop.status eq 'SUSPENDED'}">
                <form class="card" method="post" action="<c:url value='/admin/shops/${shop.id}/reactivate'/>">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <div class="card-body">
                        <p class="mb-3">Shop đang bị đình chỉ. Mở lại để shop tiếp tục bán hàng.</p>
                        <button type="submit" class="btn btn-success w-100"><i class="ti ti-player-play me-1"></i>Mở lại shop</button>
                    </div>
                </form>
            </c:when>
            <c:otherwise>
                <div class="card"><div class="card-body text-secondary">
                    Shop đã bị từ chối. Chủ shop có thể sửa thông tin và gửi lại yêu cầu.
                </div></div>
            </c:otherwise>
        </c:choose>
    </div>
</div>
</body>
</html>
