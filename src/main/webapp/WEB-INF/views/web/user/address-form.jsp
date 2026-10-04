<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>${empty addressId ? 'Thêm địa chỉ' : 'Sửa địa chỉ'}</title>
</head>
<body>
<div class="row g-3">
    <div class="col-lg-3"><%@ include file="account-nav.jsp" %></div>
    <div class="col-lg-9">
        <c:if test="${not empty error}">
            <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
        </c:if>
        <c:set var="formAction" value="/user/addresses${empty addressId ? '' : '/'}${addressId}"/>
        <form:form modelAttribute="form" method="post" action="${pageContext.request.contextPath}${formAction}"
                   cssClass="card" novalidate="novalidate">
            <c:if test="${not empty redirect}"><input type="hidden" name="redirect" value="<c:out value='${redirect}'/>"></c:if>
            <div class="card-header"><h3 class="card-title">${empty addressId ? 'Thêm địa chỉ nhận hàng' : 'Sửa địa chỉ nhận hàng'}</h3></div>
            <div class="card-body">
                <div class="row g-3">
                    <div class="col-md-6">
                        <label class="form-label required" for="receiverName">Tên người nhận</label>
                        <form:input path="receiverName" id="receiverName" cssClass="form-control" cssErrorClass="form-control is-invalid"
                                    maxlength="100" autocomplete="name"/>
                        <form:errors path="receiverName" cssClass="invalid-feedback"/>
                    </div>
                    <div class="col-md-6">
                        <label class="form-label required" for="phone">Số điện thoại</label>
                        <form:input path="phone" id="phone" type="tel" cssClass="form-control" cssErrorClass="form-control is-invalid"
                                    maxlength="12" placeholder="0912345678" autocomplete="tel"/>
                        <form:errors path="phone" cssClass="invalid-feedback"/>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label required" for="province">Tỉnh / Thành phố</label>
                        <form:input path="province" id="province" cssClass="form-control" cssErrorClass="form-control is-invalid"
                                    maxlength="100" placeholder="TP. Hồ Chí Minh"/>
                        <form:errors path="province" cssClass="invalid-feedback"/>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label required" for="district">Quận / Huyện</label>
                        <form:input path="district" id="district" cssClass="form-control" cssErrorClass="form-control is-invalid"
                                    maxlength="100" placeholder="Quận 1"/>
                        <form:errors path="district" cssClass="invalid-feedback"/>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label required" for="ward">Phường / Xã</label>
                        <form:input path="ward" id="ward" cssClass="form-control" cssErrorClass="form-control is-invalid"
                                    maxlength="100" placeholder="Phường Bến Nghé"/>
                        <form:errors path="ward" cssClass="invalid-feedback"/>
                    </div>
                    <div class="col-12">
                        <label class="form-label required" for="detail">Địa chỉ chi tiết</label>
                        <form:input path="detail" id="detail" cssClass="form-control" cssErrorClass="form-control is-invalid"
                                    maxlength="255" placeholder="Số nhà, tên đường" autocomplete="street-address"/>
                        <form:errors path="detail" cssClass="invalid-feedback"/>
                    </div>
                    <div class="col-12">
                        <label class="form-check">
                            <form:checkbox path="defaultAddress" cssClass="form-check-input"/>
                            <span class="form-check-label">Đặt làm địa chỉ mặc định</span>
                        </label>
                    </div>
                </div>
            </div>
            <div class="card-footer text-end">
                <a href="${empty redirect ? pageContext.request.contextPath.concat('/user/addresses') : fn:escapeXml(redirect)}" class="btn me-2">Hủy</a>
                <button type="submit" class="btn btn-primary"><i class="ti ti-device-floppy me-1"></i>Lưu địa chỉ</button>
            </div>
        </form:form>
    </div>
</div>
</body>
</html>
