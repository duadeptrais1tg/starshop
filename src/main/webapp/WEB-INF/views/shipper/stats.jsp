<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <title>Thống kê</title>
</head>
<body>
<%@ include file="stats-cards.jsp" %>

<div class="card">
    <div class="card-header">
        <h3 class="card-title">Theo tháng (6 tháng gần nhất)</h3>
        <span class="ms-auto small text-secondary">Giao thất bại: ${stats.failed} lần</span>
    </div>
    <div class="table-responsive">
        <table class="table table-vcenter card-table">
            <thead>
            <tr>
                <th>Tháng</th>
                <th class="text-end">Được phân công</th>
                <th class="text-end">Đã giao</th>
                <th class="text-end">Thất bại</th>
                <th style="width: 35%">Tỉ lệ thành công</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="m" items="${stats.months}">
                <tr>
                    <td class="fw-semibold">${m.label}</td>
                    <td class="text-end">${m.assigned}</td>
                    <td class="text-end text-success">${m.delivered}</td>
                    <td class="text-end ${m.failed > 0 ? 'text-danger' : ''}">${m.failed}</td>
                    <td>
                        <c:choose>
                            <c:when test="${empty m.successRate}"><span class="text-secondary small">Chưa có đơn hoàn tất</span></c:when>
                            <c:otherwise>
                                <div class="d-flex align-items-center gap-2">
                                    <div class="progress progress-sm flex-fill" role="progressbar" aria-valuenow="${m.successRate}" aria-valuemin="0" aria-valuemax="100"
                                         aria-label="Tỉ lệ thành công tháng ${m.label}">
                                        <div class="progress-bar ${m.successRate >= 90 ? 'bg-success' : (m.successRate >= 70 ? 'bg-warning' : 'bg-danger')}" style="width: ${m.successRate}%"></div>
                                    </div>
                                    <span class="fw-semibold" style="min-width: 3rem">${m.successRate}%</span>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
    <div class="card-footer small text-secondary">Tỉ lệ thành công = đã giao / (đã giao + thất bại), tính theo tháng được phân công.</div>
</div>
</body>
</html>
