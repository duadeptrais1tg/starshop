<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Doanh thu</title>
</head>
<body>
<%-- Lọc khoảng ngày + chọn nhanh --%>
<div class="card mb-3">
    <div class="card-body">
        <form method="get" action="<c:url value='/vendor/revenue'/>" class="row g-2 align-items-end">
            <div class="col-6 col-md-3">
                <label class="form-label" for="from">Từ ngày</label>
                <input type="date" id="from" name="from" class="form-control" value="${report.from}">
            </div>
            <div class="col-6 col-md-3">
                <label class="form-label" for="to">Đến ngày</label>
                <input type="date" id="to" name="to" class="form-control" value="${report.to}">
            </div>
            <div class="col-12 col-md-2">
                <button type="submit" class="btn btn-primary w-100"><i class="ti ti-filter me-1"></i>Xem</button>
            </div>
            <div class="col-12 col-md-4 d-flex flex-wrap gap-1 justify-content-md-end">
                <a class="btn btn-sm" href="<c:url value='/vendor/revenue'><c:param name='from' value='${last7}'/><c:param name='to' value='${today}'/></c:url>">7 ngày</a>
                <a class="btn btn-sm" href="<c:url value='/vendor/revenue'><c:param name='from' value='${last30}'/><c:param name='to' value='${today}'/></c:url>">30 ngày</a>
                <a class="btn btn-sm" href="<c:url value='/vendor/revenue'><c:param name='from' value='${monthStart}'/><c:param name='to' value='${today}'/></c:url>">Tháng này</a>
                <a class="btn btn-sm" href="<c:url value='/vendor/revenue'><c:param name='from' value='${yearStart}'/><c:param name='to' value='${today}'/></c:url>">Năm nay</a>
            </div>
        </form>
        <div class="text-secondary small mt-2">
            Từ ${report.fromLabel} đến ${report.toLabel} · chỉ tính đơn <strong>đã giao</strong> (theo ngày giao).
            Doanh thu = tiền hàng − giảm giá sản phẩm, không gồm phí vận chuyển.
        </div>
    </div>
</div>

<%-- Tổng quan --%>
<div class="row row-cards mb-3">
    <div class="col-6 col-lg-3">
        <div class="card card-sm"><div class="card-body">
            <div class="text-secondary small">Doanh thu</div>
            <div class="fw-bold fs-2"><fmt:formatNumber value="${report.revenue}" pattern="#,##0"/>₫</div>
        </div></div>
    </div>
    <div class="col-6 col-lg-3">
        <div class="card card-sm"><div class="card-body">
            <div class="text-secondary small">Chiết khấu app</div>
            <div class="fw-bold fs-2 text-danger">−<fmt:formatNumber value="${report.commissionRounded}" pattern="#,##0"/>₫</div>
        </div></div>
    </div>
    <div class="col-6 col-lg-3">
        <div class="card card-sm"><div class="card-body">
            <div class="text-secondary small">Thực nhận</div>
            <div class="fw-bold fs-2 text-success"><fmt:formatNumber value="${report.net}" pattern="#,##0"/>₫</div>
        </div></div>
    </div>
    <div class="col-6 col-lg-3">
        <div class="card card-sm"><div class="card-body">
            <div class="text-secondary small">Đơn đã giao</div>
            <div class="fw-bold fs-2">${report.deliveredOrders}</div>
        </div></div>
    </div>
</div>

<div class="row g-3">
    <div class="col-lg-8">
        <div class="card mb-3">
            <div class="card-header">
                <h3 class="card-title">Doanh thu theo ${report.monthly ? 'tháng' : 'ngày'}</h3>
            </div>
            <div class="card-body">
                <div style="position: relative; height: 300px">
                    <canvas id="revenue-chart" aria-label="Biểu đồ doanh thu theo ${report.monthly ? 'tháng' : 'ngày'}" role="img"></canvas>
                </div>
            </div>
        </div>

        <%-- Top 5 sản phẩm bán chạy --%>
        <div class="card">
            <div class="card-header"><h3 class="card-title">Top 5 sản phẩm bán chạy</h3></div>
            <c:choose>
                <c:when test="${empty report.topProducts}">
                    <div class="card-body text-secondary">Chưa có sản phẩm nào được giao trong khoảng này.</div>
                </c:when>
                <c:otherwise>
                    <div class="table-responsive">
                        <table class="table table-vcenter card-table">
                            <thead><tr><th class="w-1">#</th><th>Sản phẩm</th><th class="text-end">Đã bán</th><th class="text-end">Doanh số</th></tr></thead>
                            <tbody>
                            <c:forEach var="p" items="${report.topProducts}">
                                <tr>
                                    <td class="fw-bold">${p.rank}</td>
                                    <td>
                                        <div class="d-flex align-items-center gap-2">
                                            <c:choose>
                                                <c:when test="${not empty p.imageUrl}"><span class="avatar avatar-sm" style="background-image: url('<c:out value="${p.imageUrl}"/>')"></span></c:when>
                                                <c:otherwise><span class="avatar avatar-sm bg-primary-lt text-primary"><i class="ti ti-flower"></i></span></c:otherwise>
                                            </c:choose>
                                            <a href="<c:url value='/products/${p.slug}'/>" class="text-reset" target="_blank" rel="noopener"><c:out value="${p.name}"/></a>
                                        </div>
                                    </td>
                                    <td class="text-end">${p.quantity}</td>
                                    <td class="text-end"><fmt:formatNumber value="${p.revenue}" pattern="#,##0"/>₫</td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <%-- Số đơn theo trạng thái --%>
    <div class="col-lg-4">
        <div class="card">
            <div class="card-header">
                <h3 class="card-title">Đơn theo trạng thái</h3>
                <span class="ms-auto text-secondary small">${report.totalOrders} đơn đặt</span>
            </div>
            <div class="list-group list-group-flush">
                <c:forEach var="entry" items="${report.statusCounts}">
                    <div class="list-group-item d-flex align-items-center">
                        <c:set var="badgeOrderStatus" value="${entry.key}"/><%@ include file="/WEB-INF/views/common/order-status-badge.jsp" %>
                        <span class="ms-auto fw-bold">${entry.value}</span>
                    </div>
                </c:forEach>
            </div>
            <div class="card-footer small text-secondary">Tính theo ngày đặt hàng trong khoảng đã chọn.</div>
        </div>
    </div>
</div>

<script type="application/json" id="chart-data">${chartJson}</script>
<script src="<c:url value='/vendor-template/chartjs/chart.umd.min.js'/>"></script>
<script>
    (function () {
        var data = JSON.parse(document.getElementById('chart-data').textContent);
        var money = new Intl.NumberFormat('vi-VN');
        new Chart(document.getElementById('revenue-chart'), {
            type: 'bar',
            data: {
                labels: data.labels,
                datasets: [
                    {label: 'Doanh thu', data: data.revenue, backgroundColor: 'rgba(214, 51, 108, .75)', borderRadius: 4, order: 2},
                    {label: 'Thực nhận', data: data.net, type: 'line', borderColor: '#2fb344', backgroundColor: '#2fb344',
                        tension: .3, pointRadius: 2, order: 1}
                ]
            },
            options: {
                maintainAspectRatio: false,
                interaction: {mode: 'index', intersect: false},
                scales: {
                    y: {beginAtZero: true, ticks: {callback: function (v) { return money.format(v) + '₫'; }}},
                    x: {ticks: {autoSkip: true, maxRotation: 0}}
                },
                plugins: {
                    tooltip: {callbacks: {label: function (ctx) { return ctx.dataset.label + ': ' + money.format(ctx.parsed.y) + '₫'; }}}
                }
            }
        });
    })();
</script>
</body>
</html>
