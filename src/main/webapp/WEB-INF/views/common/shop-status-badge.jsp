<%@ page pageEncoding="UTF-8" %>
<%-- Badge trạng thái shop. Cần biến "badgeStatus" (ShopStatus) trước khi include. --%>
<c:choose>
    <c:when test="${badgeStatus eq 'PENDING'}"><span class="badge bg-warning-lt">${badgeStatus.label}</span></c:when>
    <c:when test="${badgeStatus eq 'APPROVED'}"><span class="badge bg-success-lt">${badgeStatus.label}</span></c:when>
    <c:when test="${badgeStatus eq 'SUSPENDED'}"><span class="badge bg-danger-lt">${badgeStatus.label}</span></c:when>
    <c:otherwise><span class="badge bg-secondary-lt">${badgeStatus.label}</span></c:otherwise>
</c:choose>
