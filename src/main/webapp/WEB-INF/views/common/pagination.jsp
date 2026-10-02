<%@ page pageEncoding="UTF-8" %>
<%--
  Phân trang dùng chung. Trang gọi include tĩnh file này, cần có attribute "page" (Spring Data Page).
  Giữ nguyên các tham số lọc hiện tại trên URL, chỉ thay tham số page (đánh số từ 1).
--%>
<c:if test="${page.totalPages > 1}">
    <c:set var="pgBase" value="${requestScope['jakarta.servlet.forward.servlet_path']}"/>
    <c:set var="pgCurrent" value="${page.number + 1}"/>
    <c:set var="pgFrom" value="${pgCurrent - 2 < 1 ? 1 : pgCurrent - 2}"/>
    <c:set var="pgTo" value="${pgFrom + 4 > page.totalPages ? page.totalPages : pgFrom + 4}"/>
    <div class="card-footer d-flex flex-wrap align-items-center gap-2">
        <p class="m-0 text-secondary">
            Hiển thị ${page.number * page.size + 1}–${page.number * page.size + page.numberOfElements}
            / ${page.totalElements}
        </p>
        <ul class="pagination m-0 ms-auto">
            <%-- Trang trước --%>
            <c:url var="pgUrl" value="${pgBase}">
                <c:forEach var="p" items="${param}"><c:if test="${p.key ne 'page' and p.key ne 'msg'}"><c:param name="${p.key}" value="${p.value}"/></c:if></c:forEach>
                <c:param name="page" value="${pgCurrent - 1}"/>
            </c:url>
            <li class="page-item ${page.first ? 'disabled' : ''}">
                <a class="page-link" href="${page.first ? '#' : pgUrl}" aria-label="Trang trước"><i class="ti ti-chevron-left"></i></a>
            </li>

            <%-- Các số trang quanh trang hiện tại --%>
            <c:forEach var="i" begin="${pgFrom}" end="${pgTo}">
                <c:url var="pgUrl" value="${pgBase}">
                    <c:forEach var="p" items="${param}"><c:if test="${p.key ne 'page' and p.key ne 'msg'}"><c:param name="${p.key}" value="${p.value}"/></c:if></c:forEach>
                    <c:param name="page" value="${i}"/>
                </c:url>
                <li class="page-item ${i == pgCurrent ? 'active' : ''}"><a class="page-link" href="${pgUrl}">${i}</a></li>
            </c:forEach>

            <%-- Trang sau --%>
            <c:url var="pgUrl" value="${pgBase}">
                <c:forEach var="p" items="${param}"><c:if test="${p.key ne 'page' and p.key ne 'msg'}"><c:param name="${p.key}" value="${p.value}"/></c:if></c:forEach>
                <c:param name="page" value="${pgCurrent + 1}"/>
            </c:url>
            <li class="page-item ${page.last ? 'disabled' : ''}">
                <a class="page-link" href="${page.last ? '#' : pgUrl}" aria-label="Trang sau"><i class="ti ti-chevron-right"></i></a>
            </li>
        </ul>
    </div>
</c:if>
