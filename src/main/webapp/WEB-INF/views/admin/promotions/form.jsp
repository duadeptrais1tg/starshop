<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>${empty promotionId ? 'Tạo khuyến mãi' : 'Sửa khuyến mãi'}</title>
</head>
<body>
<div class="mb-3">
    <a href="<c:url value='/admin/promotions'/>" class="btn btn-sm"><i class="ti ti-arrow-left me-1"></i>Danh sách khuyến mãi</a>
</div>
<c:if test="${locked}">
    <div class="alert alert-warning">
        <i class="ti ti-lock me-1"></i>Chương trình đã có người sử dụng: chỉ sửa được tên, mô tả, thời gian kết thúc,
        số lượt và giới hạn mỗi người. Các giá trị khác được giữ nguyên để không ảnh hưởng đơn đã đặt.
    </div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
</c:if>

<c:set var="formAction" value="/admin/promotions${empty promotionId ? '' : '/'}${promotionId}"/>
<form:form modelAttribute="form" method="post" action="${pageContext.request.contextPath}${formAction}"
           cssClass="card" novalidate="novalidate">
    <div class="card-body">
        <div class="row g-3">
            <div class="col-md-8">
                <label class="form-label required" for="name">Tên chương trình</label>
                <form:input path="name" id="name" cssClass="form-control" cssErrorClass="form-control is-invalid" maxlength="150"
                            placeholder="Ví dụ: Mừng ngày Phụ nữ Việt Nam 20/10"/>
                <form:errors path="name" cssClass="invalid-feedback"/>
            </div>
            <div class="col-md-4">
                <label class="form-label">&nbsp;</label>
                <label class="form-check form-switch mt-2">
                    <form:checkbox path="active" cssClass="form-check-input"/>
                    <span class="form-check-label">Bật chương trình</span>
                </label>
            </div>
            <div class="col-12">
                <label class="form-label" for="description">Mô tả</label>
                <form:textarea path="description" id="description" cssClass="form-control" rows="2" maxlength="1000"/>
            </div>

            <%-- ===== Các trường "giá trị": bị khóa khi đã có người dùng ===== --%>
            <div class="col-md-4">
                <label class="form-label required" for="type">Loại khuyến mãi</label>
                <form:select path="type" id="type" cssClass="form-select" disabled="${locked}">
                    <c:forEach var="t" items="${types}"><form:option value="${t}">${t.label}</form:option></c:forEach>
                </form:select>
                <c:if test="${locked}"><form:hidden path="type"/></c:if>
            </div>
            <div class="col-md-4">
                <label class="form-label required" for="scope">Phạm vi</label>
                <form:select path="scope" id="scope" cssClass="form-select" disabled="${locked}">
                    <c:forEach var="s" items="${scopes}"><form:option value="${s}">${s.label}</form:option></c:forEach>
                </form:select>
                <c:if test="${locked}"><form:hidden path="scope"/></c:if>
            </div>
            <div class="col-md-4" id="category-group">
                <label class="form-label required" for="categoryId">Danh mục</label>
                <form:select path="categoryId" id="categoryId" cssClass="form-select" disabled="${locked}">
                    <form:option value="">-- Chọn danh mục --</form:option>
                    <c:forEach var="cat" items="${categories}"><form:option value="${cat.id}"><c:out value="${cat.name}"/></form:option></c:forEach>
                </form:select>
                <c:if test="${locked}"><form:hidden path="categoryId"/></c:if>
            </div>
            <div class="col-md-4">
                <label class="form-label required" for="discountValue">Giá trị giảm <span id="value-unit">(%)</span></label>
                <form:input path="discountValue" id="discountValue" type="number" min="0" step="any" readonly="${locked}"
                            cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                <form:errors path="discountValue" cssClass="invalid-feedback"/>
            </div>
            <div class="col-md-4" id="max-group">
                <label class="form-label" for="maxDiscount">Giảm tối đa (₫)</label>
                <form:input path="maxDiscount" id="maxDiscount" type="number" min="0" step="1000" readonly="${locked}"
                            cssClass="form-control" cssErrorClass="form-control is-invalid" placeholder="Không giới hạn"/>
                <form:errors path="maxDiscount" cssClass="invalid-feedback"/>
            </div>
            <div class="col-md-4">
                <label class="form-label" for="minOrderValue">Đơn tối thiểu (₫)</label>
                <form:input path="minOrderValue" id="minOrderValue" type="number" min="0" step="1000" readonly="${locked}"
                            cssClass="form-control" cssErrorClass="form-control is-invalid" placeholder="0"/>
                <form:errors path="minOrderValue" cssClass="invalid-feedback"/>
            </div>
            <div class="col-md-6">
                <label class="form-label required" for="startAt">Bắt đầu</label>
                <form:input path="startAt" id="startAt" type="datetime-local" readonly="${locked}"
                            cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                <form:errors path="startAt" cssClass="invalid-feedback"/>
            </div>
            <div class="col-md-6">
                <label class="form-label required" for="endAt">Kết thúc</label>
                <form:input path="endAt" id="endAt" type="datetime-local" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                <form:errors path="endAt" cssClass="invalid-feedback"/>
            </div>

            <%-- ===== Mã coupon ===== --%>
            <div class="col-12"><hr class="my-1"></div>
            <div class="col-md-4">
                <label class="form-label" for="couponCode">Mã coupon</label>
                <form:input path="couponCode" id="couponCode" cssClass="form-control text-uppercase" cssErrorClass="form-control is-invalid"
                            maxlength="20" readonly="${locked}" placeholder="Để trống = tự áp dụng"/>
                <form:errors path="couponCode" cssClass="invalid-feedback"/>
            </div>
            <div class="col-md-4 coupon-only">
                <label class="form-label" for="usageLimit">Tổng số lượt</label>
                <form:input path="usageLimit" id="usageLimit" type="number" min="1" cssClass="form-control"
                            cssErrorClass="form-control is-invalid" placeholder="Không giới hạn"/>
                <form:errors path="usageLimit" cssClass="invalid-feedback"/>
            </div>
            <div class="col-md-4 coupon-only">
                <label class="form-label required" for="perUserLimit">Mỗi người được dùng</label>
                <form:input path="perUserLimit" id="perUserLimit" type="number" min="1" cssClass="form-control"
                            cssErrorClass="form-control is-invalid"/>
                <form:errors path="perUserLimit" cssClass="invalid-feedback"/>
            </div>
            <div class="col-12 text-secondary small">
                Không có mã: khuyến mãi tự áp dụng cho mọi đơn đủ điều kiện (không giới hạn lượt).
                Có mã: khách phải nhập mã khi đặt hàng; số lượt và giới hạn mỗi người chỉ áp dụng cho mã.
            </div>
        </div>
    </div>
    <div class="card-footer text-end">
        <a href="<c:url value='/admin/promotions'/>" class="btn me-2">Hủy</a>
        <button type="submit" class="btn btn-primary"><i class="ti ti-device-floppy me-1"></i>Lưu</button>
    </div>
</form:form>

<script>
    // Hiện/ẩn ô theo loại, phạm vi và có mã hay không (server vẫn kiểm tra lại toàn bộ)
    (function () {
        var type = document.getElementById('type');
        var scope = document.getElementById('scope');
        var code = document.getElementById('couponCode');
        function refresh() {
            var percent = type.value === 'PRODUCT_PERCENT';
            document.getElementById('value-unit').textContent = percent ? '(%)' : '(₫)';
            document.getElementById('max-group').style.display = percent ? '' : 'none';
            document.getElementById('category-group').style.display = scope.value === 'CATEGORY' ? '' : 'none';
            var hasCode = code.value.trim() !== '';
            document.querySelectorAll('.coupon-only').forEach(function (el) { el.style.opacity = hasCode ? '1' : '.5'; });
        }
        [type, scope, code].forEach(function (el) { el.addEventListener('input', refresh); el.addEventListener('change', refresh); });
        refresh();
    })();
</script>
</body>
</html>
