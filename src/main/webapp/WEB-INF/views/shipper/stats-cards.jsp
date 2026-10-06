<%@ page pageEncoding="UTF-8" %>
<%-- Thẻ số liệu tổng quan của shipper (include tĩnh). Cần biến "stats" (ShipperStats), taglib c, fmt. --%>
<div class="row row-cards mb-3">
    <div class="col-6 col-lg-3">
        <a href="<c:url value='/shipper/orders?status=ASSIGNED'/>" class="card card-sm card-link">
            <div class="card-body d-flex align-items-center gap-3">
                <span class="avatar bg-warning-lt text-warning"><i class="ti ti-package"></i></span>
                <div><div class="fw-bold fs-2">${stats.waiting}</div><div class="text-secondary small">Chờ giao</div></div>
            </div>
        </a>
    </div>
    <div class="col-6 col-lg-3">
        <a href="<c:url value='/shipper/orders?status=DELIVERING'/>" class="card card-sm card-link">
            <div class="card-body d-flex align-items-center gap-3">
                <span class="avatar bg-blue-lt text-blue"><i class="ti ti-truck-delivery"></i></span>
                <div><div class="fw-bold fs-2">${stats.delivering}</div><div class="text-secondary small">Đang giao</div></div>
            </div>
        </a>
    </div>
    <div class="col-6 col-lg-3">
        <a href="<c:url value='/shipper/orders?status=DELIVERED'/>" class="card card-sm card-link">
            <div class="card-body d-flex align-items-center gap-3">
                <span class="avatar bg-success-lt text-success"><i class="ti ti-circle-check"></i></span>
                <div><div class="fw-bold fs-2">${stats.delivered}</div><div class="text-secondary small">Đã giao / ${stats.assigned} được phân công</div></div>
            </div>
        </a>
    </div>
    <div class="col-6 col-lg-3">
        <div class="card card-sm">
            <div class="card-body d-flex align-items-center gap-3">
                <span class="avatar bg-primary-lt text-primary"><i class="ti ti-percentage"></i></span>
                <div>
                    <div class="fw-bold fs-2">${empty stats.successRate ? '–' : stats.successRate}${empty stats.successRate ? '' : '%'}</div>
                    <div class="text-secondary small">Tỉ lệ giao thành công</div>
                </div>
            </div>
        </div>
    </div>
</div>
