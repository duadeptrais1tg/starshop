<%@ page pageEncoding="UTF-8" %>
<%-- Footer dùng chung cho mọi decorator --%>
<footer class="footer footer-transparent d-print-none ss-footer">
    <div class="container-xl">
        <div class="row text-center align-items-center flex-row-reverse">
            <div class="col-lg-auto ms-lg-auto">
                <ul class="list-inline list-inline-dots mb-0">
                    <li class="list-inline-item"><a href="<c:url value='/'/>" class="link-secondary">Trang chủ</a></li>
                    <li class="list-inline-item"><a href="<c:url value='/products/search'/>" class="link-secondary">Sản phẩm</a></li>
                    <li class="list-inline-item"><a href="<c:url value='/user/orders'/>" class="link-secondary">Tra cứu đơn hàng</a></li>
                </ul>
            </div>
            <div class="col-12 col-lg-auto mt-3 mt-lg-0">
                <ul class="list-inline list-inline-dots mb-0">
                    <li class="list-inline-item">
                        &copy; 2026 <a href="<c:url value='/'/>" class="link-secondary">StarShop</a> – Chuỗi cửa hàng hoa tươi
                    </li>
                </ul>
            </div>
        </div>
    </div>
</footer>
