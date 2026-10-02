<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>${empty categoryId ? 'Thêm danh mục' : 'Sửa danh mục'}</title>
</head>
<body>
<div class="mb-3">
    <a href="<c:url value='${baseUrl}'/>" class="btn btn-sm"><i class="ti ti-arrow-left me-1"></i>Danh sách danh mục</a>
</div>

<c:if test="${not empty error}">
    <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
</c:if>

<c:set var="formAction" value="${baseUrl}${empty categoryId ? '' : '/'}${categoryId}"/>
<%-- form:form tự thêm input ẩn _csrf; enctype multipart để upload ảnh --%>
<form:form modelAttribute="form" method="post" action="${pageContext.request.contextPath}${formAction}"
           enctype="multipart/form-data" cssClass="card" novalidate="novalidate">
    <div class="card-body">
        <div class="row">
            <div class="col-lg-7">
                <div class="mb-3">
                    <label class="form-label required" for="name">Tên danh mục</label>
                    <form:input path="name" id="name" cssClass="form-control" cssErrorClass="form-control is-invalid"
                                maxlength="100" placeholder="Ví dụ: Hoa sinh nhật"/>
                    <form:errors path="name" cssClass="invalid-feedback"/>
                </div>
                <div class="mb-3">
                    <label class="form-label" for="slug">Slug (đường dẫn)</label>
                    <form:input path="slug" id="slug" cssClass="form-control" cssErrorClass="form-control is-invalid"
                                maxlength="120" placeholder="Để trống để tự tạo từ tên, ví dụ hoa-sinh-nhat"/>
                    <form:errors path="slug" cssClass="invalid-feedback"/>
                </div>
                <div class="mb-3">
                    <label class="form-label" for="parentId">Danh mục cha</label>
                    <form:select path="parentId" id="parentId" cssClass="form-select">
                        <form:option value="">— Không có (danh mục gốc) —</form:option>
                        <c:forEach var="p" items="${parents}">
                            <form:option value="${p.id}"><c:out value="${p.name}"/></form:option>
                        </c:forEach>
                    </form:select>
                </div>
                <div class="mb-3">
                    <label class="form-check form-switch">
                        <form:checkbox path="active" cssClass="form-check-input"/>
                        <span class="form-check-label">Hiển thị trên trang khách</span>
                    </label>
                </div>
            </div>
            <div class="col-lg-5">
                <label class="form-label" for="image">Ảnh danh mục</label>
                <c:if test="${not empty form.currentImageUrl}">
                    <div class="mb-2">
                        <img src="<c:out value='${form.currentImageUrl}'/>" alt="Ảnh hiện tại" class="rounded border"
                             style="max-width: 160px; max-height: 160px; object-fit: cover">
                    </div>
                    <label class="form-check mb-2">
                        <form:checkbox path="removeImage" cssClass="form-check-input"/>
                        <span class="form-check-label">Xóa ảnh hiện tại</span>
                    </label>
                </c:if>
                <input type="file" id="image" name="image" class="form-control" accept="image/jpeg,image/png,image/webp">
                <small class="form-hint">JPG, PNG hoặc WEBP, tối đa 5MB. Chọn ảnh mới sẽ thay ảnh cũ.</small>
            </div>
        </div>
    </div>
    <div class="card-footer text-end">
        <a href="<c:url value='${baseUrl}'/>" class="btn me-2">Hủy</a>
        <button type="submit" class="btn btn-primary"><i class="ti ti-device-floppy me-1"></i>Lưu</button>
    </div>
</form:form>
</body>
</html>
