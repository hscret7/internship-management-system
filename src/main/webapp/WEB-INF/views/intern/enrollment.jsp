<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Chương trình thực tập của tôi" />

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="container-fluid mt-4">

    <div class="page-head mb-4">
        <h2 class="page-head__title">Chương trình thực tập của tôi</h2>
        <p class="page-head__sub">Thông tin chương trình bạn đang tham gia và mentor hướng dẫn.</p>
    </div>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger" role="alert"><c:out value="${errorMessage}"/></div>
    </c:if>

    <c:choose>
        <c:when test="${not empty enrollment}">

            <div class="card shadow-sm mb-4">
                <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                    <h5 class="card-title mb-0 fw-bold">Chương trình đang tham gia</h5>
                    <span class="badge bg-secondary"><c:out value="${enrollment.status}"/></span>
                </div>
                <div class="card-body">
                    <dl class="row mb-0">
                        <dt class="col-sm-3">Tên chương trình</dt>
                        <dd class="col-sm-9 fw-semibold"><c:out value="${enrollment.programName}"/></dd>

                        <dt class="col-sm-3">Ngày ghi danh</dt>
                        <dd class="col-sm-9"><c:out value="${enrollment.enrolledAt}"/></dd>
                    </dl>
                </div>
            </div>

            <div class="card shadow-sm">
                <div class="card-header bg-white py-3 border-bottom">
                    <h5 class="card-title mb-0 fw-bold">Mentor hướng dẫn</h5>
                </div>
                <div class="card-body">
                    <c:choose>
                        <c:when test="${not empty currentMentor}">
                            <dl class="row mb-0">
                                <dt class="col-sm-3">Mentor hiện tại</dt>
                                <dd class="col-sm-9 fw-semibold"><c:out value="${currentMentor.mentorName}"/></dd>

                                <dt class="col-sm-3">Ngày phân công</dt>
                                <dd class="col-sm-9"><c:out value="${currentMentor.assignedAt}"/></dd>
                            </dl>
                        </c:when>
                        <c:otherwise>
                            <p class="text-muted mb-0">
                                <i class="bi bi-person-dash me-1"></i>Chưa được phân mentor.
                            </p>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

        </c:when>
        <c:otherwise>
            <div class="card shadow-sm">
                <div class="card-body text-center py-5 text-muted">
                    <i class="bi bi-diagram-3 fs-2 d-block mb-2"></i>
                    Bạn chưa được ghi danh vào chương trình nào.
                </div>
            </div>
        </c:otherwise>
    </c:choose>

</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
