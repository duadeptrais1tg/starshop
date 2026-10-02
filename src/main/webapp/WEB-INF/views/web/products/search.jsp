<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>${empty criteria.keyword ? 'Sản phẩm' : 'Tìm kiếm'}</title>
</head>
<body>
<div class="row g-3">
    <%-- Bộ lọc: cột trái trên màn hình lớn, thu gọn thành nút trên điện thoại --%>
    <div class="col-lg-3">
        <button class="btn w-100 d-lg-none mb-2" type="button" data-bs-toggle="collapse" data-bs-target="#filters"
                aria-expanded="false" aria-controls="filters">
            <i class="ti ti-adjustments-horizontal me-1"></i>Bộ lọc
        </button>
        <div class="collapse d-lg-block" id="filters">
            <form id="filter-form" method="get" action="<c:url value='/products/search'/>" class="card">
                <div class="card-body">
                    <div class="mb-3">
                        <label class="form-label" for="f-keyword">Tên sản phẩm</label>
                        <input type="search" id="f-keyword" name="keyword" class="form-control" maxlength="100"
                               placeholder="Ví dụ: hoa hồng" value="<c:out value='${criteria.keyword}'/>">
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="f-category">Danh mục</label>
                        <select id="f-category" name="categoryId" class="form-select">
                            <option value="">Tất cả danh mục</option>
                            <c:forEach var="cat" items="${categories}">
                                <option value="${cat.id}" ${criteria.categoryId == cat.id ? 'selected' : ''}><c:out value="${cat.name}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Khoảng giá (₫)</label>
                        <div class="d-flex gap-2 align-items-center">
                            <input type="number" name="minPrice" class="form-control" min="0" step="1000" placeholder="Từ"
                                   value="${criteria.minPrice}" aria-label="Giá từ">
                            <span>–</span>
                            <input type="number" name="maxPrice" class="form-control" min="0" step="1000" placeholder="Đến"
                                   value="${criteria.maxPrice}" aria-label="Giá đến">
                        </div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="f-shop">Shop</label>
                        <select id="f-shop" name="shopId" class="form-select">
                            <option value="">Tất cả shop</option>
                            <c:forEach var="s" items="${shops}">
                                <option value="${s.id}" ${criteria.shopId == s.id ? 'selected' : ''}><c:out value="${s.name}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="mb-1">
                        <div class="form-label">Đánh giá</div>
                        <label class="form-check">
                            <input class="form-check-input" type="radio" name="rating" value="" ${empty criteria.rating ? 'checked' : ''}>
                            <span class="form-check-label">Tất cả</span>
                        </label>
                        <c:forEach var="r" begin="1" end="4">
                            <c:set var="stars" value="${5 - r}"/>
                            <label class="form-check">
                                <input class="form-check-input" type="radio" name="rating" value="${stars}" ${criteria.rating == stars ? 'checked' : ''}>
                                <span class="form-check-label ss-stars">
                                    <c:forEach var="i" begin="1" end="5"><span class="ss-star ${i <= stars ? 'on' : ''}">★</span></c:forEach>
                                    ${stars < 5 ? 'trở lên' : ''}
                                </span>
                            </label>
                        </c:forEach>
                    </div>
                </div>
                <div class="card-footer d-flex gap-2">
                    <button type="submit" class="btn btn-primary flex-fill"><i class="ti ti-filter me-1"></i>Áp dụng</button>
                    <c:if test="${criteria.filtered}">
                        <a href="<c:url value='/products/search'/>" class="btn">Xóa lọc</a>
                    </c:if>
                </div>
            </form>
        </div>
    </div>

    <%-- Kết quả --%>
    <div class="col-lg-9">
        <div class="d-flex flex-wrap align-items-center gap-2 mb-3">
            <div class="text-secondary">
                <c:choose>
                    <c:when test="${not empty criteria.keyword}">
                        Tìm thấy <strong>${page.totalElements}</strong> sản phẩm cho "<strong><c:out value="${criteria.keyword}"/></strong>"
                    </c:when>
                    <c:otherwise><strong>${page.totalElements}</strong> sản phẩm</c:otherwise>
                </c:choose>
            </div>
            <div class="ms-auto d-flex align-items-center gap-2">
                <label for="f-sort" class="text-secondary text-nowrap mb-0">Sắp xếp</label>
                <%-- form="filter-form": ô này thuộc form bộ lọc, đổi là gửi lại kèm toàn bộ bộ lọc --%>
                <select id="f-sort" name="sort" form="filter-form" class="form-select form-select-sm"
                        onchange="this.form.submit()">
                    <c:forEach var="s" items="${sorts}">
                        <option value="${s}" ${criteria.sort eq s.name() ? 'selected' : ''}>${s.label}</option>
                    </c:forEach>
                </select>
            </div>
        </div>

        <c:choose>
            <c:when test="${page.totalElements == 0}">
                <div class="card">
                    <div class="empty">
                        <div class="empty-icon"><i class="ti ti-search fs-1 text-secondary"></i></div>
                        <p class="empty-title">Không tìm thấy sản phẩm phù hợp</p>
                        <p class="empty-subtitle text-secondary">Thử từ khóa khác hoặc bỏ bớt điều kiện lọc.</p>
                        <div class="empty-action"><a href="<c:url value='/products/search'/>" class="btn btn-primary">Xem tất cả sản phẩm</a></div>
                    </div>
                </div>
            </c:when>
            <c:otherwise>
                <div class="row row-cards">
                    <c:forEach var="product" items="${page.content}">
                        <div class="col-6 col-md-4">
                            <%@ include file="/WEB-INF/views/common/product-card.jsp" %>
                        </div>
                    </c:forEach>
                </div>
                <c:if test="${page.totalPages > 1}">
                    <div class="card mt-3">
                        <%@ include file="/WEB-INF/views/common/pagination.jsp" %>
                    </div>
                </c:if>
            </c:otherwise>
        </c:choose>
    </div>
</div>
</body>
</html>
