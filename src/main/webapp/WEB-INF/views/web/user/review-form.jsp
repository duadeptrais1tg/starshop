<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Đánh giá sản phẩm</title>
</head>
<body>
<div class="row justify-content-center">
    <div class="col-lg-8">
        <c:choose>
            <c:when test="${not empty blockedError}">
                <div class="card">
                    <div class="empty">
                        <div class="empty-icon"><i class="ti ti-message-off fs-1 text-secondary"></i></div>
                        <p class="empty-title"><c:out value="${blockedError}"/></p>
                        <div class="empty-action"><a href="<c:url value='/user/orders'/>" class="btn btn-primary">Đơn hàng của tôi</a></div>
                    </div>
                </div>
            </c:when>
            <c:otherwise>
                <div class="mb-3">
                    <a href="<c:url value='/user/orders/${target.orderId}'/>" class="btn btn-sm"><i class="ti ti-arrow-left me-1"></i>Đơn <c:out value="${target.orderCode}"/></a>
                </div>
                <form:form modelAttribute="form" method="post" action="${pageContext.request.contextPath}/user/reviews"
                           enctype="multipart/form-data" cssClass="card" id="review-form" novalidate="novalidate">
                    <form:hidden path="orderItemId"/>
                    <div class="card-header">
                        <div class="d-flex align-items-center gap-3">
                            <c:choose>
                                <c:when test="${not empty target.imageUrl}"><span class="avatar avatar-lg" style="background-image: url('<c:out value="${target.imageUrl}"/>')"></span></c:when>
                                <c:otherwise><span class="avatar avatar-lg bg-primary-lt text-primary"><i class="ti ti-flower"></i></span></c:otherwise>
                            </c:choose>
                            <div>
                                <h3 class="card-title m-0">Đánh giá: <c:out value="${target.productName}"/></h3>
                                <div class="text-secondary small">Đơn <c:out value="${target.orderCode}"/> · số lượng ${target.quantity}</div>
                            </div>
                        </div>
                    </div>
                    <div class="card-body">
                        <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>

                        <%-- Chọn sao: radio 5 -> 1 (CSS đảo chiều để tô các sao bên trái) --%>
                        <label class="form-label required">Chất lượng sản phẩm</label>
                        <div class="ss-rate mb-1" role="radiogroup" aria-label="Số sao">
                            <c:forEach var="i" begin="1" end="5">
                                <c:set var="s" value="${6 - i}"/>
                                <input type="radio" id="star-${s}" name="rating" value="${s}" ${form.rating == s ? 'checked' : ''}>
                                <label for="star-${s}" title="${s} sao">★</label>
                            </c:forEach>
                        </div>
                        <div class="text-secondary small mb-3" id="rating-text"></div>
                        <form:errors path="rating" cssClass="text-danger small d-block mb-2"/>

                        <label class="form-label required" for="content">Nhận xét</label>
                        <form:textarea path="content" id="content" rows="5" cssClass="form-control" cssErrorClass="form-control is-invalid"
                                       maxlength="${maxContent * 2}"
                                       placeholder="Hoa có tươi không, đúng mẫu không, đóng gói và giao hàng thế nào..."/>
                        <div class="d-flex justify-content-between small mt-1">
                            <form:errors path="content" cssClass="text-danger"/>
                            <span class="ms-auto" id="content-count" aria-live="polite"></span>
                        </div>

                        <div class="row g-3 mt-1">
                            <div class="col-md-7">
                                <label class="form-label" for="images">Ảnh (tối đa ${maxImages})</label>
                                <input type="file" id="images" name="images" class="form-control" multiple accept="image/jpeg,image/png,image/webp">
                                <div class="form-hint">JPG/PNG/WEBP, mỗi ảnh tối đa 5MB.</div>
                            </div>
                            <div class="col-md-5">
                                <label class="form-label" for="video">Video (1 video)</label>
                                <input type="file" id="video" name="video" class="form-control" accept="video/mp4">
                                <div class="form-hint">MP4, tối đa 30MB.</div>
                            </div>
                        </div>
                        <div class="d-flex flex-wrap gap-2 mt-2" id="media-preview"></div>
                    </div>
                    <div class="card-footer text-end">
                        <button type="submit" class="btn btn-primary" id="review-submit"><i class="ti ti-send me-1"></i>Gửi đánh giá</button>
                    </div>
                </form:form>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<script>
    (function () {
        var form = document.getElementById('review-form');
        if (!form) {
            return;
        }
        var min = ${minContent}, max = ${maxContent}, maxImages = ${maxImages};
        var labels = {1: 'Rất tệ', 2: 'Tệ', 3: 'Bình thường', 4: 'Tốt', 5: 'Tuyệt vời'};
        var content = document.getElementById('content');
        var counter = document.getElementById('content-count');
        var images = document.getElementById('images');
        var video = document.getElementById('video');
        var preview = document.getElementById('media-preview');

        // Đếm ký tự hiển thị (Array.from tách theo ký tự Unicode, giống cách server đếm)
        function length() { return Array.from(content.value.trim()).length; }
        function updateCount() {
            var n = length();
            counter.textContent = n < min ? 'Còn thiếu ' + (min - n) + ' ký tự (' + n + '/' + min + ')' : n + '/' + max + ' ký tự';
            counter.className = 'ms-auto ' + (n < min || n > max ? 'text-danger' : 'text-success');
        }
        function updateRating() {
            var checked = form.querySelector('input[name="rating"]:checked');
            document.getElementById('rating-text').textContent = checked ? labels[checked.value] : 'Chọn số sao';
        }
        function updatePreview() {
            preview.innerHTML = '';
            Array.prototype.slice.call(images.files, 0, maxImages).forEach(function (f) {
                var img = document.createElement('img');
                img.src = URL.createObjectURL(f);
                img.className = 'rounded border ss-vendor-preview';
                img.alt = f.name;
                preview.appendChild(img);
            });
            if (video.files[0]) {
                var v = document.createElement('video');
                v.src = URL.createObjectURL(video.files[0]);
                v.className = 'rounded border ss-vendor-preview';
                v.muted = true;
                preview.appendChild(v);
            }
        }

        content.addEventListener('input', updateCount);
        form.querySelectorAll('input[name="rating"]').forEach(function (r) { r.addEventListener('change', updateRating); });
        images.addEventListener('change', updatePreview);
        video.addEventListener('change', updatePreview);
        form.addEventListener('submit', function (e) {
            var n = length();
            var problem = !form.querySelector('input[name="rating"]:checked') ? 'Vui lòng chọn số sao.'
                : n < min ? 'Nội dung đánh giá tối thiểu ' + min + ' ký tự.'
                : n > max ? 'Nội dung đánh giá tối đa ' + max + ' ký tự.'
                : images.files.length > maxImages ? 'Tối đa ' + maxImages + ' ảnh.' : null;
            if (problem) {
                e.preventDefault();
                StarShop.toast(problem, 'danger');
                return;
            }
            document.getElementById('review-submit').disabled = true;
        });
        updateCount();
        updateRating();
    })();
</script>
</body>
</html>
