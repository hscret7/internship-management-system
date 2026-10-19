<%@ page contentType="text/html; charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>

<jsp:include page="/WEB-INF/views/common/header.jsp">
    <jsp:param name="title" value="Lịch sử phụ cấp" />
</jsp:include>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h2><i class="bi bi-cash-coin me-2"></i> Lịch sử phụ cấp</h2>
</div>

<c:if test="${not empty param.error}">
    <div class="alert alert-danger"><c:out value="${param.error}"/></div>
</c:if>

<div class="card">
    <div class="card-body">
        <table class="table table-bordered table-striped">
            <thead>
                <tr>
                    <th>Kỳ áp dụng</th>
                    <th>Loại phụ cấp</th>
                    <th>Số tiền (VNĐ)</th>
                    <th>Trạng thái</th>
                    <th>Ngày thanh toán</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${empty allowances}">
                        <tr><td colspan="5" class="text-center">Không có dữ liệu phụ cấp.</td></tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="a" items="${allowances}">
                            <tr>
                                <td><c:out value="${a.period}" /></td>
                                <td><c:out value="${a.allowanceType}" /></td>
                                <td><fmt:formatNumber value="${a.amount}" type="currency" currencySymbol="" /></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${a.status eq 'PAID'}">
                                            <span class="badge bg-success">Đã thanh toán</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-warning">Chờ thanh toán</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty a.paidAt}">
                                            <c:out value="${fn:replace(a.paidAt, 'T', ' ')}" />
                                        </c:when>
                                        <c:otherwise>-</c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
    </div>
</div>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
