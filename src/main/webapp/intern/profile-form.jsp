<%--
    intern/profile-form.jsp - Partial form hồ sơ. Owner: FE1
    Được include từ intern/profile.jsp (US01 / task 3.3).

    TÊN CÁC FIELD KHÔNG ĐƯỢC ĐỔI - InternProfileServlet của BE2 đang đọc đúng:
        profileId, fullName, dob, gender, phone, email, university, major, gpa, skills
    Giá trị gender giữ nguyên 'Nam' / 'Nữ' / 'Khác' như bản BE2 đang lưu.

    MỌI giá trị lấy từ profile PHẢI bọc <c:out>. Đây là dữ liệu người dùng
    nhập ở trang này rồi render lại chính trang này, nên không escape là
    stored XSS. QA-11 (06/10/2026) đã tái hiện: skills chứa
    "</textarea><b id=...>" thì thẻ <b> thoát khỏi textarea và thành phần tử
    HTML thật. Chú ý 2 ngữ cảnh khác nhau:
        - trong attribute: value="<c:out value="${profile.x}"/>"
        - trong textarea : ><c:out value="${profile.x}"/></textarea>
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<div class="card">
    <div class="card-header">
        <span>Thông tin cá nhân</span>
        <c:if test="${not empty profile}">
            <%-- ACTIVE là hiệu lực hồ sơ, không phải duyệt đơn hay ghi danh thực tập. --%>
            <span class="badge-status ${profile.status eq 'ACTIVE' ? 'badge-status--ok' : 'badge-status--neutral'}">
                <c:choose>
                    <c:when test="${profile.status eq 'ACTIVE'}">Hồ sơ hiệu lực</c:when>
                    <c:when test="${profile.status eq 'INACTIVE'}">Tạm dừng</c:when>
                    <c:otherwise><c:out value="${profile.status}" /></c:otherwise>
                </c:choose>
            </span>
        </c:if>
    </div>

    <form action="${ctx}/intern/profile" method="post">
    <input type="hidden" name="csrfToken" value="${csrfToken}" />
        <div class="card-body">

            <input type="hidden" name="profileId" value="<c:out value="${profile.profileId}"/>">

            <div class="mb-3">
                <label class="form-label" for="fullName">Họ và tên <span class="req">*</span></label>
                <input type="text" id="fullName" name="fullName" class="form-control"
                       value="<c:out value="${profile.fullName}"/>" required maxlength="100"
                       placeholder="Nguyễn Văn Nam">
            </div>

            <div class="row g-3 mb-3">
                <div class="col-md-6">
                    <label class="form-label" for="dob">Ngày sinh</label>
                    <input type="date" id="dob" name="dob" class="form-control" value="<c:out value="${profile.dob}"/>">
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="gender">Giới tính</label>
                    <select id="gender" name="gender" class="form-select">
                        <option value="Nam" ${profile.gender eq 'Nam' ? 'selected' : ''}>Nam</option>
                        <option value="Nữ" ${profile.gender eq 'Nữ' ? 'selected' : ''}>Nữ</option>
                        <option value="Khác" ${profile.gender eq 'Khác' ? 'selected' : ''}>Khác</option>
                    </select>
                </div>
            </div>

            <div class="row g-3 mb-3">
                <div class="col-md-6">
                    <label class="form-label" for="phone">Số điện thoại <span class="req">*</span></label>
                    <input type="tel" id="phone" name="phone" class="form-control"
                           value="<c:out value="${profile.phone}"/>" required
                           pattern="[0-9+ .()\-]{9,15}" placeholder="0912345678">
                    <span class="form-text">Từ 9 đến 15 ký tự số.</span>
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="email">Email liên hệ <span class="req">*</span></label>
                    <input type="email" id="email" name="email" class="form-control"
                           value="<c:out value="${profile.email}"/>" required placeholder="nam@student.edu.vn">
                </div>
            </div>

            <div class="row g-3 mb-3">
                <div class="col-md-8">
                    <label class="form-label" for="university">Trường đại học / cao đẳng <span class="req">*</span></label>
                    <input type="text" id="university" name="university" class="form-control"
                           value="<c:out value="${profile.university}"/>" required maxlength="150"
                           placeholder="Đại học Bách Khoa Hà Nội">
                </div>
                <div class="col-md-4">
                    <label class="form-label" for="gpa">GPA (hệ 4)</label>
                    <input type="number" id="gpa" name="gpa" class="form-control"
                           value="<c:out value="${profile.gpa}"/>" step="0.01" min="0" max="4" placeholder="3.50">
                </div>
            </div>

            <div class="mb-3">
                <label class="form-label" for="major">Chuyên ngành <span class="req">*</span></label>
                <input type="text" id="major" name="major" class="form-control"
                       value="<c:out value="${profile.major}"/>" required maxlength="100"
                       placeholder="Công nghệ thông tin">
            </div>

            <div class="mb-0">
                <label class="form-label" for="skills">Kỹ năng / mô tả bản thân</label>
                <textarea id="skills" name="skills" rows="4" class="form-control"
                          maxlength="1000"
                          placeholder="Java, Servlet, MySQL, Git..."><c:out value="${profile.skills}"/></textarea>
            </div>

        </div>

        <div class="card-footer d-flex justify-content-between align-items-center">
            <span class="form-text"><span class="req">*</span> là thông tin bắt buộc.</span>
            <button type="submit" class="btn btn-brand">
                <i class="bi bi-save me-1"></i>${empty profile ? 'Tạo hồ sơ' : 'Cập nhật hồ sơ'}
            </button>
        </div>
    </form>
</div>
