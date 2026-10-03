<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>${empty carrierId ? 'Thêm nhà vận chuyển' : 'Sửa nhà vận chuyển'}</title>
</head>
<body>
<div class="mb-3">
    <a href="<c:url value='/admin/carriers'/>" class="btn btn-sm"><i class="ti ti-arrow-left me-1"></i>Danh sách nhà vận chuyển</a>
</div>
<c:if test="${not empty error}">
    <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
</c:if>

<c:set var="formAction" value="/admin/carriers${empty carrierId ? '' : '/'}${carrierId}"/>
<form:form modelAttribute="form" method="post" action="${pageContext.request.contextPath}${formAction}"
           cssClass="card" style="max-width: 640px" novalidate="novalidate">
    <div class="card-body">
        <div class="mb-3">
            <label class="form-label required" for="name">Tên nhà vận chuyển</label>
            <form:input path="name" id="name" cssClass="form-control" cssErrorClass="form-control is-invalid"
                        maxlength="100" placeholder="Ví dụ: Giao Hàng Nhanh"/>
            <form:errors path="name" cssClass="invalid-feedback"/>
        </div>
        <div class="mb-3">
            <label class="form-label required" for="shippingFee">Phí vận chuyển (₫)</label>
            <form:input path="shippingFee" id="shippingFee" type="number" min="0" step="1000"
                        cssClass="form-control" cssErrorClass="form-control is-invalid" placeholder="30000"/>
            <form:errors path="shippingFee" cssClass="invalid-feedback"/>
        </div>
        <label class="form-check form-switch">
            <form:checkbox path="active" cssClass="form-check-input"/>
            <span class="form-check-label">Đang hoạt động (khách chọn được khi đặt hàng)</span>
        </label>
    </div>
    <div class="card-footer text-end">
        <a href="<c:url value='/admin/carriers'/>" class="btn me-2">Hủy</a>
        <button type="submit" class="btn btn-primary"><i class="ti ti-device-floppy me-1"></i>Lưu</button>
    </div>
</form:form>
</body>
</html>
