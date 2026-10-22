<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Đội ngũ mentor" />
<c:set var="activePage" value="hr-mentors" />
<c:set var="pageStylesheet" value="fe3-mentor.css" />
<c:set var="pageScript" value="fe3-mentor.js" />
<c:set var="mentorNotice" value="${requestScope.errorMessage}" />
<c:set var="mentorError" value="${mentors eq null ? mentorNotice : ''}" />
<c:remove var="errorMessage" scope="request" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="fe3-page fm-page" data-mentor-page="directory">
    <section class="fm-hero" aria-labelledby="mentor-title">
        <div>
            <span class="fm-eyebrow">NHÂN SỰ · HƯỚNG DẪN THỰC TẬP</span>
            <h2 id="mentor-title">Đội ngũ mentor</h2>
            <p>Đúng chuyên môn. Đủ sức chứa. Theo dõi đội ngũ và phân công người đồng hành cho từng thực tập sinh.</p>
            <div class="fm-hero-actions">
                <a class="fm-button fm-button--primary" href="#mentor-create" data-open-create>＋ Thêm mentor</a>
                <a class="fm-button" href="${ctx}/hr/enrollments?ui=mentor">Xem phân công <span aria-hidden="true">↗</span></a>
            </div>
        </div>
        <div class="fm-hero-art" aria-hidden="true">
            <svg viewBox="0 0 200 160" fill="none"><path d="M100 52V92M44 116V92H156V116" stroke="currentColor" stroke-width="2" stroke-dasharray="4 5"/><circle cx="100" cy="37" r="27"/><circle cx="44" cy="131" r="22"/><circle cx="100" cy="131" r="22"/><circle cx="156" cy="131" r="22"/><path d="M91 36a9 9 0 1 0 18 0m-23 16c2-12 26-12 28 0M36 132h16m40 0h16m40 0h16" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
            <span>MỘT ĐỘI NGŨ · CÙNG MỤC TIÊU</span>
        </div>
    </section>

    <c:if test="${not empty successMessage}"><div class="fm-notice fm-notice--success" role="status"><c:out value="${successMessage}" /></div></c:if>
    <c:if test="${not empty mentorNotice}"><div class="fm-notice fm-notice--error" role="alert"><c:out value="${mentorNotice}" /> <a href="${ctx}/hr/mentors">Tải lại</a></div></c:if>

    <c:set var="activeCount" value="0" /><c:set var="fullCount" value="0" /><c:set var="totalMentees" value="0" /><c:set var="freePlaces" value="0" />
    <c:forEach var="mentor" items="${mentors}">
        <c:if test="${mentor.status eq 'ACTIVE'}">
            <c:set var="activeCount" value="${activeCount + 1}" />
            <c:set var="totalMentees" value="${totalMentees + mentor.currentMenteeCount}" />
            <c:choose>
                <c:when test="${mentor.currentMenteeCount ge mentor.maxMentees}"><c:set var="fullCount" value="${fullCount + 1}" /></c:when>
                <c:otherwise><c:set var="freePlaces" value="${freePlaces + mentor.maxMentees - mentor.currentMenteeCount}" /></c:otherwise>
            </c:choose>
        </c:if>
    </c:forEach>
    <section class="fm-metrics" aria-label="Tổng quan theo bộ lọc phòng ban">
        <div><span>Mentor đang hoạt động</span><strong><c:out value="${empty mentorError ? activeCount : '—'}" /></strong><small>Trong phòng ban đang chọn</small></div>
        <div><span>TTS đang được hướng dẫn</span><strong><c:out value="${empty mentorError ? totalMentees : '—'}" /></strong><small>Chỉ ghi danh ACTIVE hiện tại</small></div>
        <div><span>Chỗ hướng dẫn còn lại</span><strong><c:out value="${empty mentorError ? freePlaces : '—'}" /></strong><small>Tổng sức chứa còn khả dụng</small></div>
        <div><span>Mentor đã đủ tải</span><strong><c:out value="${empty mentorError ? fullCount : '—'}" /></strong><small>Cần chọn mentor khác khi phân</small></div>
    </section>

    <details class="fm-create fm-panel" id="mentor-create">
        <summary><span>＋ Thêm hồ sơ mentor</span><small>Từ tài khoản MENTOR đã được cấp</small></summary>
        <div class="fm-create-body">
            <c:choose>
                <c:when test="${not empty mentorError}"><p class="fm-muted">Dữ liệu chưa tải được. Vui lòng tải lại trước khi thêm mentor.</p></c:when>
                <c:when test="${empty departments or empty mentorUsers}">
                    <div class="fm-notice">
                        <c:if test="${empty departments}">Chưa có phòng ban. Hãy tạo phòng ban trong phần thiết lập trước. </c:if>
                        <c:if test="${empty mentorUsers}">Không có tài khoản MENTOR đủ điều kiện. Admin cần cấp tài khoản MENTOR đang hoạt động và chưa có hồ sơ mentor.</c:if>
                    </div>
                </c:when>
                <c:otherwise>
                    <form method="post" action="${ctx}/hr/mentors" class="fm-form" data-mentor-form>
                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />" />
                        <input type="hidden" name="action" value="create-mentor" />
                        <div class="fm-field"><label for="mentor-user">Tài khoản mentor <span>*</span></label>
                            <select id="mentor-user" name="userId" required><option value="">Chọn tài khoản đã được cấp</option>
                                <c:forEach var="user" items="${mentorUsers}"><option value="${user.userId}"><c:out value="${user.fullName}" /> · <c:out value="${user.email}" /></option></c:forEach>
                            </select><small>Hồ sơ mentor không tạo tài khoản đăng nhập mới.</small>
                        </div>
                        <div class="fm-field"><label for="mentor-department">Phòng ban <span>*</span></label>
                            <select id="mentor-department" name="departmentId" required><option value="">Chọn phòng ban</option>
                                <c:forEach var="department" items="${departments}"><option value="${department.departmentId}" ${selectedDepartmentId eq department.departmentId ? 'selected' : ''}><c:out value="${department.departmentName}" /></option></c:forEach>
                            </select><small>Mentor chỉ được phân vào chương trình cùng phòng ban.</small>
                        </div>
                        <div class="fm-field"><label for="mentor-specialization">Chuyên môn</label><input id="mentor-specialization" name="specialization" maxlength="200" placeholder="Ví dụ: Java, kiểm thử phần mềm…" /><small>Tối đa 200 ký tự.</small></div>
                        <div class="fm-field"><label for="mentor-capacity">Số TTS tối đa <span>*</span></label><input id="mentor-capacity" name="maxMentees" type="number" min="1" max="2147483647" step="1" value="5" required /><small>Số nguyên lớn hơn 0; server kiểm tra lại sức chứa.</small></div>
                        <div class="fm-form-footer"><p>Các trường có dấu * là bắt buộc.</p><button class="fm-button fm-button--primary" type="submit" data-submit-label="Tạo hồ sơ mentor">Tạo hồ sơ mentor</button></div>
                    </form>
                </c:otherwise>
            </c:choose>
        </div>
    </details>

    <section class="fm-directory" aria-labelledby="directory-title">
        <div class="fm-section-heading"><div><span class="fm-eyebrow">DANH SÁCH & SỨC CHỨA</span><h3 id="directory-title">Người đồng hành của thực tập sinh</h3></div><span class="fm-count" data-visible-count><c:out value="${fn:length(mentors)}" /> mentor</span></div>
        <div class="fm-toolbar fm-panel">
            <form method="get" action="${ctx}/hr/mentors" class="fm-department-filter">
                <label for="department-filter">Phòng ban</label><select id="department-filter" name="departmentId"><option value="">Tất cả phòng ban</option><c:forEach var="department" items="${departments}"><option value="${department.departmentId}" ${selectedDepartmentId eq department.departmentId ? 'selected' : ''}><c:out value="${department.departmentName}" /></option></c:forEach></select><button type="submit" class="fm-button">Lọc</button>
            </form>
            <div class="fm-field fm-search"><label for="mentor-search">Tìm nhanh trong danh sách</label><input id="mentor-search" type="search" placeholder="Tên, email hoặc chuyên môn…" data-search autocomplete="off" /></div>
            <div class="fm-field fm-capacity-filter"><label for="capacity-filter">Sức chứa</label><select id="capacity-filter" data-capacity-filter><option value="all">Tất cả mentor</option><option value="available">Còn chỗ</option><option value="full">Đã đủ tải</option><option value="inactive">Tạm ngừng</option></select></div>
        </div>
        <c:choose>
            <c:when test="${not empty mentorError}"><div class="fm-empty fm-panel"><h4>Chưa tải được danh sách</h4><p>Không dùng số 0 thay cho dữ liệu đang lỗi. Hãy tải lại hoặc báo QA kèm log server.</p></div></c:when>
            <c:when test="${empty mentors}"><div class="fm-empty fm-panel"><span class="fm-empty-icon" aria-hidden="true">◇</span><h4>Chưa có mentor trong danh sách này</h4><p>Thử chọn tất cả phòng ban hoặc thêm hồ sơ mentor đầu tiên.</p><a class="fm-button" href="#mentor-create" data-open-create>Thêm mentor</a></div></c:when>
            <c:otherwise>
                <div class="fm-mentor-grid">
                    <c:forEach var="mentor" items="${mentors}">
                        <c:set var="remaining" value="${mentor.maxMentees - mentor.currentMenteeCount}" />
                        <c:set var="capacityState" value="${mentor.status ne 'ACTIVE' ? 'inactive' : (remaining le 0 ? 'full' : 'available')}" />
                        <article class="fm-mentor-card fm-panel" data-filter-item data-capacity="${capacityState}" data-search-text="<c:out value='${mentor.fullName} ${mentor.email} ${mentor.specialization} ${mentor.departmentName}' />">
                            <div class="fm-person"><span class="fm-avatar" aria-hidden="true"><c:out value="${fn:toUpperCase(fn:substring(mentor.fullName, 0, 1))}" /></span><div><h4><c:out value="${mentor.fullName}" /></h4><span class="fm-muted"><c:out value="${mentor.email}" /></span></div></div>
                            <div class="fm-card-tags"><span class="fm-chip"><c:out value="${empty mentor.departmentName ? 'Chưa có phòng ban' : mentor.departmentName}" /></span><span class="fm-chip fm-chip--${capacityState}"><c:choose><c:when test="${capacityState eq 'inactive'}">Tạm ngừng</c:when><c:when test="${capacityState eq 'full'}">Đã đủ tải</c:when><c:otherwise>Còn <c:out value="${remaining}" /> chỗ</c:otherwise></c:choose></span></div>
                            <p class="fm-specialization"><c:out value="${empty mentor.specialization ? 'Chưa cập nhật chuyên môn' : mentor.specialization}" /></p>
                            <div class="fm-workload"><span>Đang hướng dẫn</span><strong><c:out value="${mentor.currentMenteeCount}" /> <small>/ <c:out value="${mentor.maxMentees}" /> TTS</small></strong></div>
                            <meter class="fm-meter" min="0" max="${mentor.maxMentees}" value="${mentor.currentMenteeCount}" aria-label="Tải hướng dẫn của <c:out value='${mentor.fullName}' />"></meter>
                            <div class="fm-card-footer"><small>Mã mentor #<c:out value="${mentor.mentorId}" /></small><a href="${ctx}/hr/enrollments?ui=mentor&amp;mentorId=${mentor.mentorId}">Xem phân công <span aria-hidden="true">→</span></a></div>
                        </article>
                    </c:forEach>
                </div>
                <div class="fm-empty fm-panel" data-no-results hidden><h4>Không tìm thấy mentor phù hợp</h4><p>Đổi từ khóa hoặc bộ lọc sức chứa để xem thêm.</p><button class="fm-button" type="button" data-clear-filters>Xóa bộ lọc nhanh</button></div>
                <p class="fm-footnote" data-filter-feedback aria-live="polite">Số liệu cập nhật khi tải trang. Tìm nhanh không thay đổi tổng quan theo phòng ban.</p>
            </c:otherwise>
        </c:choose>
    </section>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
