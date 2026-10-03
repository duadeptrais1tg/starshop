<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Sổ địa chỉ</title>
</head>
<body>
<div class="row g-3">
    <div class="col-lg-3"><%@ include file="account-nav.jsp" %></div>
    <div class="col-lg-9">
        <c:if test="${not empty message}">
            <div class="alert alert-success" role="alert"><c:out value="${message}"/></div>
        </c:if>
        <div class="d-flex align-items-center mb-3">
            <div>
                <h2 class="m-0">Sổ địa chỉ</h2>
                <div class="text-secondary small">${addresses.size()} / ${maxAddresses} địa chỉ</div>
            </div>
            <c:if test="${addresses.size() < maxAddresses}">
                <a href="<c:url value='/user/addresses/new'/>" class="btn btn-primary ms-auto"><i class="ti ti-plus me-1"></i>Thêm địa chỉ</a>
            </c:if>
        </div>

        <c:choose>
            <c:when test="${empty addresses}">
                <div class="card">
                    <div class="empty">
                        <div class="empty-icon"><i class="ti ti-map-pin fs-1 text-secondary"></i></div>
                        <p class="empty-title">Bạn chưa có địa chỉ nhận hàng</p>
                        <p class="empty-subtitle text-secondary">Thêm địa chỉ để đặt hoa nhanh hơn.</p>
                        <div class="empty-action"><a href="<c:url value='/user/addresses/new'/>" class="btn btn-primary">Thêm địa chỉ</a></div>
                    </div>
                </div>
            </c:when>
            <c:otherwise>
                <div class="card">
                    <div class="list-group list-group-flush">
                        <c:forEach var="a" items="${addresses}">
                            <div class="list-group-item">
                                <div class="d-flex flex-wrap align-items-start gap-2">
                                    <div class="flex-fill">
                                        <div>
                                            <strong><c:out value="${a.receiverName}"/></strong>
                                            <span class="text-secondary ms-1">| <c:out value="${a.phone}"/></span>
                                            <c:if test="${a.defaultAddress}"><span class="badge bg-primary text-white ms-1">Mặc định</span></c:if>
                                        </div>
                                        <div class="text-secondary"><c:out value="${a.fullAddress}"/></div>
                                    </div>
                                    <div class="d-flex flex-wrap gap-1">
                                        <a href="<c:url value='/user/addresses/${a.id}/edit'/>" class="btn btn-sm"><i class="ti ti-edit me-1"></i>Sửa</a>
                                        <c:if test="${not a.defaultAddress}">
                                            <form method="post" action="<c:url value='/user/addresses/${a.id}/default'/>" class="m-0">
                                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                                <button type="submit" class="btn btn-sm btn-outline-primary">Đặt mặc định</button>
                                            </form>
                                        </c:if>
                                        <form method="post" action="<c:url value='/user/addresses/${a.id}/delete'/>" class="m-0"
                                              onsubmit="return confirm('Xóa địa chỉ này?');">
                                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                            <button type="submit" class="btn btn-sm btn-outline-danger" title="Xóa"><i class="ti ti-trash"></i></button>
                                        </form>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>
</body>
</html>
