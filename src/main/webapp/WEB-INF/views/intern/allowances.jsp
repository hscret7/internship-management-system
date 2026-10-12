<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>

<c:set var="pageTitle" value="Lịch sử phụ cấp" />
<c:set var="activePage" value="intern-allowances" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <div>
        <h2 class="h4 mb-1"><i class="bi bi-cash-coin me-2"></i>Lịch sử phụ cấp (US26)</h2>
        <p class="text-muted mb-0">Theo dõi các khoản phụ cấp hàng tháng và trạng thái thanh toán.</p>
    </div>
</div>

<c:if test="${not empty error or not empty param.error}">
    <div class="alert alert-danger" role="alert">
        <c:out value="${not empty error ? error : param.error}"/>
    </div>
</c:if>

<div class="card shadow-sm border-0">
    <div class="card-body p-0">
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead class="table-light">
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
                            <tr><td colspan="5" class="text-center text-muted py-5"><i class="bi bi-cash-stack fs-2 d-block mb-2"></i>Không có dữ liệu phụ cấp.</td></tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="a" items="${allowances}">
                                <tr>
                                    <td><strong><c:out value="${a.period}" /></strong></td>
                                    <td><c:out value="${a.allowanceType}" /></td>
                                    <td><fmt:formatNumber value="${a.amount}" type="number" maxFractionDigits="0" /> VNĐ</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${a.status eq 'PAID'}">
                                                <span class="badge text-bg-success">Đã thanh toán</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge text-bg-warning">Chờ thanh toán</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty a.paidAt}">
                                                <c:out value="${a.paidAt}" />
                                            </c:when>
                                            <c:otherwise>—</c:otherwise>
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
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
