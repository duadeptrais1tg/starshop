<%@ page pageEncoding="UTF-8" %>
<%-- Bảng lịch sử mức chiết khấu. Cần "history" (List<CommissionRateDto>) và "historyShopId" (null = mặc định). --%>
<c:choose>
    <c:when test="${empty history}">
        <div class="card-body text-secondary">Chưa có mức chiết khấu nào.</div>
    </c:when>
    <c:otherwise>
        <div class="table-responsive">
            <table class="table table-vcenter card-table">
                <thead>
                <tr><th>Tỉ lệ</th><th>Từ ngày</th><th>Đến ngày</th><th>Trạng thái</th><th class="w-1"></th></tr>
                </thead>
                <tbody>
                <c:forEach var="h" items="${history}">
                    <tr>
                        <td class="fw-bold"><fmt:formatNumber value="${h.rate}" pattern="#,##0.##"/>%</td>
                        <td>${h.effectiveFrom}</td>
                        <td>${empty h.effectiveTo ? 'Không thời hạn' : h.effectiveTo}</td>
                        <td>
                            <c:choose>
                                <c:when test="${h.status eq 'ACTIVE'}"><span class="badge bg-success-lt">${h.status.label}</span></c:when>
                                <c:when test="${h.status eq 'UPCOMING'}"><span class="badge bg-azure-lt">${h.status.label}</span></c:when>
                                <c:otherwise><span class="badge bg-secondary-lt">${h.status.label}</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:if test="${h.deletable}">
                                <form method="post" action="<c:url value='/admin/commissions/${h.id}/delete'/>" class="m-0"
                                      onsubmit="return confirm('Xóa mức chiết khấu chưa áp dụng này?');">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                    <c:if test="${not empty historyShopId}"><input type="hidden" name="shopId" value="${historyShopId}"></c:if>
                                    <button type="submit" class="btn btn-sm btn-outline-danger" title="Xóa"><i class="ti ti-trash"></i></button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:otherwise>
</c:choose>
