<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>${empty productId ? 'Thêm sản phẩm' : 'Sửa sản phẩm'}</title>
</head>
<body>
<div class="d-flex flex-wrap gap-2 mb-3">
    <a href="<c:url value='/vendor/products'/>" class="btn btn-sm"><i class="ti ti-arrow-left me-1"></i>Danh sách sản phẩm</a>
    <c:if test="${not empty productSlug}">
        <a href="<c:url value='/products/${productSlug}'/>" class="btn btn-sm ms-auto" target="_blank" rel="noopener">
            <i class="ti ti-external-link me-1"></i>Xem trên trang khách
        </a>
    </c:if>
</div>
<c:if test="${not empty message}">
    <div class="alert alert-success" role="alert"><c:out value="${message}"/></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
</c:if>

<div class="row g-3">
    <div class="${empty productId ? 'col-12' : 'col-lg-8'}">
        <c:set var="formAction" value="/vendor/products${empty productId ? '' : '/'}${productId}"/>
        <form:form modelAttribute="form" method="post" action="${pageContext.request.contextPath}${formAction}"
                   enctype="multipart/form-data" cssClass="card" novalidate="novalidate">
            <div class="card-body">
                <div class="row g-3">
                    <div class="col-md-8">
                        <label class="form-label required" for="name">Tên sản phẩm</label>
                        <form:input path="name" id="name" cssClass="form-control" cssErrorClass="form-control is-invalid" maxlength="200"/>
                        <form:errors path="name" cssClass="invalid-feedback"/>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label required" for="categoryId">Danh mục</label>
                        <form:select path="categoryId" id="categoryId" cssClass="form-select" cssErrorClass="form-select is-invalid">
                            <form:option value="" label="-- Chọn danh mục --"/>
                            <form:options items="${categories}" itemValue="id" itemLabel="name"/>
                        </form:select>
                        <form:errors path="categoryId" cssClass="invalid-feedback"/>
                    </div>
                    <div class="col-6 col-md-4">
                        <label class="form-label required" for="price">Giá bán (₫)</label>
                        <form:input path="price" id="price" type="number" min="1" step="1" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                        <form:errors path="price" cssClass="invalid-feedback"/>
                    </div>
                    <div class="col-6 col-md-4">
                        <label class="form-label" for="originalPrice">Giá gốc (₫)</label>
                        <form:input path="originalPrice" id="originalPrice" type="number" min="1" step="1" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                        <form:errors path="originalPrice" cssClass="invalid-feedback"/>
                        <div class="form-hint">Để trống nếu không giảm giá.</div>
                    </div>
                    <div class="col-6 col-md-4">
                        <label class="form-label required" for="stock">Tồn kho</label>
                        <form:input path="stock" id="stock" type="number" min="0" step="1" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                        <form:errors path="stock" cssClass="invalid-feedback"/>
                    </div>
                    <div class="col-12">
                        <label class="form-label" for="description">Mô tả</label>
                        <form:textarea path="description" id="description" rows="6" maxlength="5000" cssClass="form-control" cssErrorClass="form-control is-invalid"
                                       placeholder="Thành phần bó hoa, kích thước, ý nghĩa, cách bảo quản..."/>
                        <form:errors path="description" cssClass="invalid-feedback"/>
                    </div>
                    <div class="col-12">
                        <label class="form-label ${empty productId ? 'required' : ''}" for="images">
                            ${empty productId ? 'Ảnh sản phẩm' : 'Thêm ảnh'}
                        </label>
                        <input type="file" name="images" id="images" class="form-control" multiple accept="image/jpeg,image/png,image/webp">
                        <div class="form-hint">
                            JPG/PNG/WEBP, mỗi ảnh tối đa 5MB, tối đa ${maxImages} ảnh / sản phẩm.
                            <c:if test="${empty productId}">Ảnh đầu tiên là ảnh đại diện (đổi được sau khi lưu).</c:if>
                        </div>
                        <div class="d-flex flex-wrap gap-2 mt-2" id="image-preview"></div>
                    </div>
                    <div class="col-12">
                        <label class="form-check form-switch">
                            <form:checkbox path="active" cssClass="form-check-input"/>
                            <span class="form-check-label">Đang bán (bỏ chọn để ngừng bán, khách sẽ không thấy sản phẩm)</span>
                        </label>
                    </div>
                </div>
            </div>
            <div class="card-footer text-end">
                <a href="<c:url value='/vendor/products'/>" class="btn me-2">Hủy</a>
                <button type="submit" class="btn btn-primary"><i class="ti ti-device-floppy me-1"></i>${empty productId ? 'Thêm sản phẩm' : 'Lưu thay đổi'}</button>
            </div>
        </form:form>
    </div>

    <%-- Ảnh hiện có: chọn ảnh đại diện / xóa --%>
    <c:if test="${not empty productId}">
        <div class="col-lg-4">
            <div class="card">
                <div class="card-header"><h3 class="card-title">Ảnh sản phẩm (${images.size()}/${maxImages})</h3></div>
                <div class="card-body">
                    <div class="row g-2">
                        <c:forEach var="img" items="${images}">
                            <div class="col-6">
                                <div class="card card-sm ${img.thumbnail ? 'border-primary' : ''}">
                                    <img src="<c:out value='${img.url}'/>" alt="Ảnh sản phẩm" class="card-img-top ss-vendor-thumb">
                                    <div class="card-body p-1 d-flex gap-1 justify-content-center">
                                        <c:choose>
                                            <c:when test="${img.thumbnail}"><span class="badge bg-primary text-white">Ảnh đại diện</span></c:when>
                                            <c:otherwise>
                                                <form method="post" action="<c:url value='/vendor/products/${productId}/images/${img.id}/thumbnail'/>">
                                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                                    <button type="submit" class="btn btn-sm btn-ghost-primary" title="Đặt làm ảnh đại diện"><i class="ti ti-star"></i></button>
                                                </form>
                                            </c:otherwise>
                                        </c:choose>
                                        <form method="post" action="<c:url value='/vendor/products/${productId}/images/${img.id}/delete'/>"
                                              onsubmit="return confirm('Xóa ảnh này?');">
                                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                            <button type="submit" class="btn btn-sm btn-ghost-danger" title="Xóa ảnh"><i class="ti ti-trash"></i></button>
                                        </form>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </div>
        </div>
    </c:if>
</div>

<script>
    // Xem trước ảnh vừa chọn (chưa upload)
    (function () {
        var input = document.getElementById('images');
        var preview = document.getElementById('image-preview');
        input.addEventListener('change', function () {
            preview.innerHTML = '';
            Array.prototype.forEach.call(input.files, function (file, index) {
                if (!file.type.startsWith('image/')) {
                    return;
                }
                var img = document.createElement('img');
                img.src = URL.createObjectURL(file);
                img.className = 'rounded border ss-vendor-preview' + (index === 0 ? ' border-primary' : '');
                img.alt = file.name;
                preview.appendChild(img);
            });
        });
    })();
</script>
</body>
</html>
