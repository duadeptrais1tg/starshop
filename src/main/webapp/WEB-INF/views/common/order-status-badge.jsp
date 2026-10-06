<%@ page pageEncoding="UTF-8" %>
<%-- Badge trạng thái đơn. Cần biến "badgeOrderStatus" (OrderStatus) trước khi include. --%>
<c:choose>
    <c:when test="${badgeOrderStatus eq 'NEW'}"><span class="badge bg-warning-lt">${badgeOrderStatus.label}</span></c:when>
    <c:when test="${badgeOrderStatus eq 'CONFIRMED'}"><span class="badge bg-azure-lt">${badgeOrderStatus.label}</span></c:when>
    <c:when test="${badgeOrderStatus eq 'PICKED_UP' or badgeOrderStatus eq 'SHIPPING'}"><span class="badge bg-blue-lt">${badgeOrderStatus.label}</span></c:when>
    <c:when test="${badgeOrderStatus eq 'DELIVERED'}"><span class="badge bg-success-lt">${badgeOrderStatus.label}</span></c:when>
    <c:when test="${badgeOrderStatus eq 'RETURN_REQUESTED'}"><span class="badge bg-orange-lt">${badgeOrderStatus.label}</span></c:when>
    <c:when test="${badgeOrderStatus eq 'CANCELLED'}"><span class="badge bg-danger-lt">${badgeOrderStatus.label}</span></c:when>
    <c:otherwise><span class="badge bg-secondary-lt">${badgeOrderStatus.label}</span></c:otherwise>
</c:choose>
