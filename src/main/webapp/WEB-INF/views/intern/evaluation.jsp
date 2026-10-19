<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Kết quả đánh giá thực tập" />
<c:set var="activePage" value="intern-evaluation" />
<c:set var="pageStylesheet" value="fe3.css" />
<c:set var="pageScript" value="fe3.js" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<style>
    @media print {
        header, footer, nav, .navbar, .fe3-hero, .no-print, #enrollmentSelectGroup {
            display: none !important;
        }
        body {
            background: #fff !important;
            font-size: 12pt;
        }
        .fe3-page {
            max-width: 100% !important;
            margin: 0 !important;
            padding: 0 !important;
        }
        .eval-card-history {
            border: 2px solid #333 !important;
            box-shadow: none !important;
            page-break-inside: avoid;
        }
    }
</style>

<section class="fe3-page" id="fe3App" data-page="intern-evaluation" data-context="${ctx}">
    <div class="fe3-hero">
        <div>
            <span class="fe3-hero__eyebrow"><i class="bi bi-award"></i> Thành quả thực tập</span>
            <h2>Kết quả & Chứng nhận đánh giá thực tập</h2>
            <p>Xem bảng điểm chi tiết, đánh giá hai tiêu chí (Kỹ năng chuyên môn & Thái độ làm việc), xếp loại tốt nghiệp và in phiếu đánh giá chính thức.</p>
        </div>
        <div class="fe3-hero__mark" aria-hidden="true"><i class="bi bi-trophy"></i></div>
    </div>

    <div id="fe3Notice" class="alert d-none" role="status" aria-live="polite"></div>

    <div class="fe3-panel" id="enrollmentSelectGroup">
        <label class="form-label fw-bold" for="enrollmentSelect"><i class="bi bi-bookmarks me-1 text-primary"></i>Chọn đợt thực tập của bạn</label>
        <select class="form-select" id="enrollmentSelect"><option value="">Đang tải danh sách đợt thực tập…</option></select>
    </div>

    <div class="fe3-panel" id="evaluationResult" aria-live="polite">
        <div class="text-center py-4 text-muted">
            <div class="spinner-border spinner-border-sm text-primary me-2" role="status"></div>
            Đang tải thông tin kết quả đánh giá...
        </div>
    </div>

    <!-- Modal Giấy chứng nhận hoàn thành thực tập -->
    <div class="modal fade" id="certificateModal" tabindex="-1" aria-labelledby="certificateModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-lg modal-dialog-centered">
            <div class="modal-content border-0 shadow">
                <div class="modal-header border-bottom no-print">
                    <h5 class="modal-title h6 fw-bold text-dark" id="certificateModalLabel">
                        <i class="bi bi-award-fill text-warning me-2"></i>Chứng nhận hoàn thành thực tập
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4 p-md-5" id="certBody">
                    <div class="certificate-inner p-4 p-md-5 rounded border border-3 border-warning position-relative text-center" style="background: #fffdf9;">
                        <div class="text-uppercase fw-bold text-muted small letter-spacing-1">CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM</div>
                        <div class="fw-bold small mb-4">Độc lập – Tự do – Hạnh phúc</div>
                        <div class="text-muted small mb-3">————— ❖ —————</div>

                        <div class="my-3">
                            <i class="bi bi-trophy-fill text-warning display-4"></i>
                            <h2 class="h3 fw-bold text-primary mt-2 text-uppercase mb-0">GIẤY CHỨNG NHẬN THỰC TẬP</h2>
                            <div class="text-muted fst-italic small">CERTIFICATE OF INTERNSHIP COMPLETION</div>
                        </div>

                        <p class="fs-6 mt-4 mb-1 text-muted">Chứng nhận Thực tập sinh:</p>
                        <h3 class="h3 fw-bold text-dark text-uppercase mb-3" id="certInternName">—</h3>

                        <p class="text-secondary mb-3 leading-relaxed">
                            Đã hoàn thành chương trình thực tập chuyên môn tại doanh nghiệp:
                            <br><strong class="text-primary fs-5 d-block mt-1" id="certProgramName">—</strong>
                            <span class="small text-muted mt-1 d-inline-block">Thời gian: <span id="certDates">—</span></span>
                        </p>

                        <div class="row justify-content-center g-3 my-3">
                            <div class="col-6 col-sm-4">
                                <div class="p-2 border rounded bg-white shadow-sm">
                                    <small class="text-muted d-block small">Điểm quy đổi (Hệ 10)</small>
                                    <strong class="fs-4 text-primary" id="certScore10">—</strong>
                                </div>
                            </div>
                            <div class="col-6 col-sm-4">
                                <div class="p-2 border rounded bg-white shadow-sm">
                                    <small class="text-muted d-block small">Xếp loại tốt nghiệp</small>
                                    <strong class="fs-4 text-success" id="certGrade">—</strong>
                                </div>
                            </div>
                        </div>

                        <div class="row mt-5 pt-3 text-center align-items-end">
                            <div class="col-6">
                                <div class="small text-muted mb-4">Người hướng dẫn (Mentor)</div>
                                <div class="fw-bold text-dark mt-4" id="certMentorName">—</div>
                            </div>
                            <div class="col-6">
                                <div class="small text-muted mb-4">Đại diện Ban Nhân sự & Đào tạo</div>
                                <div class="fw-bold text-dark mt-4">BAN ĐÀO TẠO & DOANH NGHIỆP</div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer border-top no-print">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button>
                    <button type="button" class="btn btn-primary" onclick="window.print()">
                        <i class="bi bi-printer me-1"></i> In giấy chứng nhận
                    </button>
                </div>
            </div>
        </div>
    </div>
</section>

<script>
    window.showInternCertificate = function(data) {
        if (!data) return;
        document.getElementById('certInternName').textContent = data.internName || 'Thực tập sinh';
        document.getElementById('certProgramName').textContent = data.programName || 'Chương trình thực tập';
        document.getElementById('certDates').textContent = data.dates || '—';
        document.getElementById('certScore10').textContent = data.score10 ? (data.score10 + ' / 10.0') : '—';
        document.getElementById('certGrade').textContent = data.grade || 'Đạt';
        document.getElementById('certMentorName').textContent = data.mentorName || 'Mentor hướng dẫn';

        var modalEl = document.getElementById('certificateModal');
        if (modalEl) {
            if(window.openAppModal) window.openAppModal(modalEl);
            else if(window.bootstrap) bootstrap.Modal.getOrCreateInstance(modalEl).show();
        }
    };
</script>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
