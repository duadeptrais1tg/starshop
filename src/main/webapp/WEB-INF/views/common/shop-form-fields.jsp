<%@ page pageEncoding="UTF-8" %>
<%--
  Các ô nhập thông tin shop, dùng chung cho form "Mở shop" và form sửa shop của vendor.
  Trang gọi khai báo taglib c, form; nằm trong form:form modelAttribute="shopForm" enctype multipart.
  Biến tùy chọn: "currentLogo", "currentBanner" (URL ảnh hiện tại).
--%>
<div class="row g-3">
    <div class="col-md-6">
        <label class="form-label required" for="name">Tên shop</label>
        <form:input path="name" id="name" cssClass="form-control" cssErrorClass="form-control is-invalid" maxlength="150"
                    placeholder="Ví dụ: Hoa Tươi Mỗi Ngày"/>
        <form:errors path="name" cssClass="invalid-feedback"/>
        <div class="form-hint">Tên hiển thị với khách, không trùng với shop khác.</div>
    </div>
    <div class="col-md-6">
        <label class="form-label required" for="phone">Số điện thoại shop</label>
        <form:input path="phone" id="phone" type="tel" cssClass="form-control" cssErrorClass="form-control is-invalid" maxlength="12"/>
        <form:errors path="phone" cssClass="invalid-feedback"/>
    </div>
    <div class="col-12">
        <label class="form-label required" for="pickupAddress">Địa chỉ lấy hàng</label>
        <form:input path="pickupAddress" id="pickupAddress" cssClass="form-control" cssErrorClass="form-control is-invalid"
                    maxlength="255" placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành phố"/>
        <form:errors path="pickupAddress" cssClass="invalid-feedback"/>
        <div class="form-hint">Shipper đến địa chỉ này để lấy hàng.</div>
    </div>
    <div class="col-12">
        <label class="form-label" for="description">Mô tả shop</label>
        <form:textarea path="description" id="description" rows="4" cssClass="form-control"
                       cssErrorClass="form-control is-invalid" maxlength="2000"
                       placeholder="Giới thiệu ngắn về shop, loại hoa, khu vực giao hàng..."/>
        <form:errors path="description" cssClass="invalid-feedback"/>
    </div>
    <div class="col-md-4">
        <label class="form-label" for="logo">Logo</label>
        <c:if test="${not empty currentLogo}">
            <div class="mb-2"><span class="avatar avatar-lg" style="background-image: url('<c:out value="${currentLogo}"/>')"></span></div>
        </c:if>
        <input type="file" name="logo" id="logo" class="form-control" accept="image/jpeg,image/png,image/webp">
        <div class="form-hint">Ảnh vuông, JPG/PNG/WEBP, tối đa 5MB.<c:if test="${not empty currentLogo}"> Để trống nếu giữ logo cũ.</c:if></div>
    </div>
    <div class="col-md-8">
        <label class="form-label" for="banner">Banner</label>
        <c:if test="${not empty currentBanner}">
            <div class="mb-2"><img src="<c:out value='${currentBanner}'/>" alt="Banner hiện tại" class="rounded w-100 ss-shop-banner-preview"></div>
        </c:if>
        <input type="file" name="banner" id="banner" class="form-control" accept="image/jpeg,image/png,image/webp">
        <div class="form-hint">Ảnh ngang (khoảng 1200×300), tối đa 5MB.<c:if test="${not empty currentBanner}"> Để trống nếu giữ banner cũ.</c:if></div>
    </div>
</div>
