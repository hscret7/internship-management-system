<%--
    footer.jsp - Dong khung trang dung chung. Owner: FE1
    Luon include NGAY SAU noi dung trang, truoc </body>.

    ============================================================================
    DISCUSSION: vi sao file nay TU KHAI BAO taglib + ctx (khong dua vao trang cha)
    ----------------------------------------------------------------------------
    header.jsp dat ctx bang <c:set> -> mac dinh la PAGE SCOPE, chi song trong
    trang da include no. Neu trang cha include footer bang <jsp:include> (include
    DONG, chay luc request) thi footer duoc bien dich RIENG va KHONG thay bien
    page-scope cua trang cha. Hau qua tung gap tren 6 trang (hr|intern x
    allowances|attendance|support):
        ${ctx}        -> rong  => src="/assets/js/app.js" (THIEU context path)
        ${pageScript} -> rong  => <script src="/assets/js/">
    => app.js va notifications-bell.js deu KHONG nap duoc: mat het JS dung chung,
       chuong thong bao bam khong phan hoi (markup van con nen nhin nhu binh thuong).

    Vi vay footer TU set lai ctx. Gia tri luon giong header dat, nen voi 21 trang
    include TINH thi khong doi gi; voi trang include DONG thi sua duoc loi tren.
    Khai bao lai taglib cung prefix + cung URI la hop le theo spec JSP.

    DA KIEM CHUNG (10/10/2026, Chrome headless qua CDP):
        - /hr/support + /intern/support: truoc khi sua, bam chuong KHONG mo panel
          (aria-expanded giu "false", body ket o "Đang tải…") vi 2 file JS khong
          nap duoc; sau khi sua, panel mo that va hien dung danh sach.
        - /hr/catalog (include TINH): truoc va sau giau nguyen ket qua.
        - 10 trang INTERN + HR: chuong bam mo duoc, 0 loi JS, 0 request loi.
    --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

        </main>
        <%-- /.app-content --%>

    </div>
    <%-- /.app-main --%>

</div>
<%-- /.app-shell --%>

<c:if test="${not empty sessionScope.currentUser}">
    <%-- Modal Đổi mật khẩu cá nhân: đặt ngoài .app-shell, là con trực tiếp của <body> --%>
    <div class="modal fade" id="changePasswordModal" tabindex="-1" aria-labelledby="changePasswordModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content shadow border-0">
                <div class="modal-header border-bottom-0 pb-0">
                    <h5 class="modal-title h6 fw-bold" id="changePasswordModalLabel">
                        <i class="bi bi-shield-lock text-primary me-2"></i>Đổi mật khẩu cá nhân
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                </div>
                <form id="changePasswordForm">
                    <div class="modal-body pt-3">
                        <div id="changePasswordAlert" class="alert d-none" role="alert"></div>
                        <input type="hidden" name="csrfToken" value="<%= com.internship.util.CsrfToken.expose(request) %>">

                        <div class="mb-3">
                            <label class="form-label fw-semibold small" for="modalCurrentPassword">Mật khẩu hiện tại <span class="text-danger">*</span></label>
                            <div class="position-relative">
                                <input type="password" class="form-control pe-5" id="modalCurrentPassword" name="currentPassword" required autocomplete="current-password" placeholder="Nhập mật khẩu hiện tại">
                                <button type="button" class="btn-toggle-password position-absolute end-0 top-50 translate-middle-y me-2 border-0 bg-transparent text-secondary p-1" tabindex="-1" title="Hiện/ẩn mật khẩu" aria-label="Hiện/ẩn mật khẩu" onclick="togglePasswordVisibility(this, event)" style="cursor: pointer; z-index: 5; display: flex; align-items: center; justify-content: center;">
                                    <span class="pwd-toggle-icon" style="display: flex;">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 16 16"><path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8M1.173 8a13 13 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5s3.879 1.168 5.168 2.457A13 13 0 0 1 14.828 8q-.086.13-.195.288c-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5s-3.879-1.168-5.168-2.457A13 13 0 0 1 1.172 8z"/><path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5M4.5 8a3.5 3.5 0 1 1 7 0 3.5 3.5 0 0 1-7 0"/></svg>
                                    </span>
                                </button>
                            </div>
                        </div>

                        <div class="mb-3">
                            <label class="form-label fw-semibold small" for="modalNewPassword">Mật khẩu mới <span class="text-danger">*</span></label>
                            <div class="position-relative">
                                <input type="password" class="form-control pe-5" id="modalNewPassword" name="newPassword" minlength="8" required autocomplete="new-password" placeholder="Nhập mật khẩu mới">
                                <button type="button" class="btn-toggle-password position-absolute end-0 top-50 translate-middle-y me-2 border-0 bg-transparent text-secondary p-1" tabindex="-1" title="Hiện/ẩn mật khẩu" aria-label="Hiện/ẩn mật khẩu" onclick="togglePasswordVisibility(this, event)" style="cursor: pointer; z-index: 5; display: flex; align-items: center; justify-content: center;">
                                    <span class="pwd-toggle-icon" style="display: flex;">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 16 16"><path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8M1.173 8a13 13 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5s3.879 1.168 5.168 2.457A13 13 0 0 1 14.828 8q-.086.13-.195.288c-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5s-3.879-1.168-5.168-2.457A13 13 0 0 1 1.172 8z"/><path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5M4.5 8a3.5 3.5 0 1 1 7 0 3.5 3.5 0 0 1-7 0"/></svg>
                                    </span>
                                </button>
                            </div>
                            <div class="form-text small text-muted">Tối thiểu 8 ký tự, tối đa 72 byte UTF-8.</div>
                        </div>

                        <div class="mb-3">
                            <label class="form-label fw-semibold small" for="modalConfirmPassword">Xác nhận mật khẩu mới <span class="text-danger">*</span></label>
                            <div class="position-relative">
                                <input type="password" class="form-control pe-5" id="modalConfirmPassword" name="confirmPassword" minlength="8" required autocomplete="new-password" placeholder="Nhập lại mật khẩu mới">
                                <button type="button" class="btn-toggle-password position-absolute end-0 top-50 translate-middle-y me-2 border-0 bg-transparent text-secondary p-1" tabindex="-1" title="Hiện/ẩn mật khẩu" aria-label="Hiện/ẩn mật khẩu" onclick="togglePasswordVisibility(this, event)" style="cursor: pointer; z-index: 5; display: flex; align-items: center; justify-content: center;">
                                    <span class="pwd-toggle-icon" style="display: flex;">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 16 16"><path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8M1.173 8a13 13 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5s3.879 1.168 5.168 2.457A13 13 0 0 1 14.828 8q-.086.13-.195.288c-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5s-3.879-1.168-5.168-2.457A13 13 0 0 1 1.172 8z"/><path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5M4.5 8a3.5 3.5 0 1 1 7 0 3.5 3.5 0 0 1-7 0"/></svg>
                                    </span>
                                </button>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer border-top-0 pt-0">
                        <button type="button" class="btn btn-secondary px-3" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-brand px-3" id="btnSubmitChangePassword">
                            <span class="spinner-border spinner-border-sm me-1 d-none" role="status" aria-hidden="true"></span>
                            Cập nhật mật khẩu
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            var cpForm = document.getElementById('changePasswordForm');
            if (!cpForm) return;
            cpForm.addEventListener('submit', function(e) {
                e.preventDefault();
                var alertBox = document.getElementById('changePasswordAlert');
                var btnSubmit = document.getElementById('btnSubmitChangePassword');
                var spinner = btnSubmit.querySelector('.spinner-border');

                alertBox.className = 'alert d-none';
                btnSubmit.disabled = true;
                if (spinner) spinner.classList.remove('d-none');

                var formData = new URLSearchParams(new FormData(cpForm));
                fetch('${pageContext.request.contextPath}/auth/change-password', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                        'X-Requested-With': 'XMLHttpRequest'
                    },
                    body: formData.toString()
                })
                .then(function(res) { return res.json(); })
                .then(function(data) {
                    btnSubmit.disabled = false;
                    if (spinner) spinner.classList.add('d-none');
                    if (data.success) {
                        alertBox.className = 'alert alert-success';
                        alertBox.textContent = data.message || 'Đổi mật khẩu thành công!';
                        cpForm.reset();
                        setTimeout(function() {
                            window.location.assign('${pageContext.request.contextPath}/auth/login');
                        }, 1500);
                    } else {
                        alertBox.className = 'alert alert-danger';
                        alertBox.textContent = data.error || 'Đổi mật khẩu thất bại.';
                    }
                })
                .catch(function(err) {
                    btnSubmit.disabled = false;
                    if (spinner) spinner.classList.add('d-none');
                    alertBox.className = 'alert alert-danger';
                    alertBox.textContent = 'Lỗi kết nối máy chủ: ' + err.message;
                });
            });
        });
    </script>
</c:if>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="${ctx}/assets/js/app.js?v=20261019-khanh-base"></script>

<%-- Chuong thong bao nam trong header.jsp nen phai nap o MOI trang co khung chung,
     khong the de trong pageScript (chi co mot so trang set). Script tu thoat ngay
     neu khong tim thay #appBell, nen an toan voi ca trang khong co chuong. --%>
<script src="${ctx}/assets/js/notifications-bell.js?v=20261019-khanh-base"></script>

<c:if test="${not empty pageScript}">
    <script src="${ctx}/assets/js/${pageScript}?v=20261019-khanh-base"></script>
</c:if>
</body>
</html>
