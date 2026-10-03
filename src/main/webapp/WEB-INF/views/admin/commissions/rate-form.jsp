<%@ page pageEncoding="UTF-8" %>
<%-- Form thêm mức chiết khấu. Cần "rateFormAction" (đường dẫn POST, không gồm context path). --%>
<form:form modelAttribute="form" method="post" action="${pageContext.request.contextPath}${rateFormAction}" novalidate="novalidate">
    <div class="card-body">
        <div class="row g-2">
            <div class="col-12 col-md-4">
                <label class="form-label required" for="rate">Tỉ lệ (%)</label>
                <form:input path="rate" id="rate" type="number" min="0" max="100" step="0.01"
                            cssClass="form-control" cssErrorClass="form-control is-invalid" placeholder="10"/>
                <form:errors path="rate" cssClass="invalid-feedback"/>
            </div>
            <div class="col-6 col-md-4">
                <label class="form-label required" for="effectiveFrom">Áp dụng từ</label>
                <form:input path="effectiveFrom" id="effectiveFrom" type="date"
                            cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                <form:errors path="effectiveFrom" cssClass="invalid-feedback"/>
            </div>
            <div class="col-6 col-md-4">
                <label class="form-label" for="effectiveTo">Đến ngày</label>
                <form:input path="effectiveTo" id="effectiveTo" type="date" cssClass="form-control"/>
            </div>
        </div>
        <small class="form-hint mt-2">
            Áp dụng từ hôm nay trở đi. Mức đang chạy sẽ tự kết thúc vào ngày trước đó; để trống "Đến ngày" nếu không giới hạn.
        </small>
    </div>
    <div class="card-footer text-end">
        <button type="submit" class="btn btn-primary"><i class="ti ti-plus me-1"></i>Thêm mức</button>
    </div>
</form:form>
