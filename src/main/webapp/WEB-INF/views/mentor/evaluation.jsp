<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Đánh giá kết quả thực tập" />
<c:set var="activePage" value="mentor-evaluation" />
<c:set var="pageStylesheet" value="fe3.css" />
<c:set var="pageScript" value="fe3.js" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<style>
    .crit-card {
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        padding: 12px 16px;
        transition: border-color 0.15s ease, box-shadow 0.15s ease;
    }
    .crit-card:hover {
        border-color: #cbd5e1;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
    }
    .crit-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-wrap: wrap;
        gap: 8px;
        margin-bottom: 4px;
    }
    .crit-title {
        font-size: 0.95rem;
        font-weight: 600;
        color: #1e293b;
    }
    .crit-desc {
        font-size: 0.82rem;
        color: #64748b;
        margin-bottom: 8px;
    }
    .score-btn-group {
        display: inline-flex;
        gap: 3px;
        background: #f1f5f9;
        padding: 2px;
        border-radius: 6px;
    }
    .score-btn {
        border: none;
        background: transparent;
        color: #475569;
        font-size: 0.8rem;
        font-weight: 600;
        padding: 4px 10px;
        border-radius: 4px;
        cursor: pointer;
        transition: all 0.15s ease;
        user-select: none;
    }
    .score-btn:hover {
        background: #e2e8f0;
        color: #0f172a;
    }
    .score-btn.active {
        background: #1d6fb8;
        color: #ffffff !important;
        box-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
    }
    .score-summary-card {
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 10px;
        padding: 16px 20px;
    }
    .score-summary-item {
        display: flex;
        flex-direction: column;
    }
    .score-summary-item small {
        color: #64748b;
        font-size: 0.8rem;
        text-transform: uppercase;
        letter-spacing: 0.5px;
    }
    .score-summary-item strong {
        font-size: 1.25rem;
        font-weight: 700;
        margin-top: 2px;
    }
    .eval-card-history {
        border: 1px solid #e2e8f0;
        border-left: 4px solid var(--brand, #1d6fb8);
        border-radius: 8px;
        padding: 16px 20px;
        margin-bottom: 16px;
        background: #ffffff;
    }
</style>

<section class="fe3-page" id="fe3App" data-page="mentor-evaluation" data-context="${ctx}">
    <div class="fe3-hero">
        <div>
            <span class="fe3-hero__eyebrow"><i class="bi bi-clipboard-check"></i> Đánh giá thực tập</span>
            <h2>Đánh giá kết quả thực tập sinh</h2>
            <p>Người hướng dẫn (Mentor) trực tiếp đánh giá năng lực chuyên môn (8 tiêu chí) và ý thức, thái độ làm việc (5 tiêu chí), ghi nhận xét chi tiết cho từng mục.</p>
        </div>
        <div class="fe3-hero__mark" aria-hidden="true"><i class="bi bi-award"></i></div>
    </div>

    <div id="fe3Notice" class="alert d-none" role="status" aria-live="polite"></div>

    <!-- Chọn thực tập sinh -->
    <div class="fe3-panel">
        <label class="form-label fw-bold" for="enrollmentSelect"><i class="bi bi-person-check me-1 text-primary"></i>Chọn thực tập sinh & đợt thực tập</label>
        <select class="form-select" id="enrollmentSelect"><option value="">Đang tải danh sách…</option></select>
    </div>

    <!-- Phiếu đánh giá -->
    <div class="fe3-panel">
        <div class="d-flex justify-content-between align-items-center mb-3 pb-2 border-bottom">
            <div>
                <h3 class="h5 fw-bold mb-0 text-dark"><i class="bi bi-file-earmark-check me-2 text-primary"></i>Phiếu đánh giá thực tập</h3>
                <small class="text-muted">Đánh giá khách quan theo 8 tiêu chí chuyên môn và 5 tiêu chí thái độ (thang điểm 1–5)</small>
            </div>
            <span class="badge bg-secondary-subtle text-secondary fs-6" id="activeStatusBadge">Đang chọn TTS</span>
        </div>

        <form id="evaluationForm" class="d-none">
            <!-- PHẦN I: KỸ NĂNG & NĂNG LỰC CHUYÊN MÔN (8 TIÊU CHÍ) -->
            <div class="mb-4">
                <div class="d-flex justify-content-between align-items-center mb-2 pb-1 border-bottom">
                    <h4 class="h6 fw-bold text-primary mb-0">
                        <i class="bi bi-code-square me-1"></i> I. Kỹ năng chuyên môn & Năng lực thực hiện công việc (8 tiêu chí)
                    </h4>
                    <span class="badge bg-primary-subtle text-primary border" id="skillGroupAvgBadge">TB: 4.0 / 5</span>
                </div>
                <p class="text-muted small mb-3">Đánh giá kiến thức nền tảng, kỹ năng code, thiết kế CSDL, debug, sử dụng Git và mức độ hoàn thành nhiệm vụ.</p>
                <div id="skillsCriteriaList" class="d-flex flex-column gap-2"></div>
                <input type="hidden" id="evaluationSkills" name="skillsScore" value="4">
            </div>

            <!-- PHẦN II: Ý THỨC KỶ LUẬT & THÁI ĐỘ LÀM VIỆC (5 TIÊU CHÍ) -->
            <div class="mb-4">
                <div class="d-flex justify-content-between align-items-center mb-2 pb-1 border-bottom">
                    <h4 class="h6 fw-bold text-success mb-0">
                        <i class="bi bi-person-check-fill me-1"></i> II. Ý thức kỷ luật & Thái độ làm việc (5 tiêu chí)
                    </h4>
                    <span class="badge bg-success-subtle text-success border" id="attitudeGroupAvgBadge">TB: 4.0 / 5</span>
                </div>
                <p class="text-muted small mb-3">Đánh giá tính chuyên cần, kỷ luật giờ giấc, tinh thần trách nhiệm, làm việc nhóm và thái độ cầu tiến.</p>
                <div id="attitudeCriteriaList" class="d-flex flex-column gap-2"></div>
                <input type="hidden" id="evaluationAttitude" name="attitudeScore" value="4">
            </div>

            <!-- BẢNG TÍNH ĐIỂM TỔNG HỢP & XẾP LOẠI DỰ KIẾN -->
            <div class="score-summary-card mb-4">
                <div class="row g-3 text-center text-sm-start align-items-center">
                    <div class="col-6 col-sm-3 score-summary-item">
                        <small>Điểm TB Kỹ năng (8 mục)</small>
                        <strong class="text-primary" id="calcSkillScore">4.0 / 5</strong>
                    </div>
                    <div class="col-6 col-sm-3 score-summary-item">
                        <small>Điểm TB Thái độ (5 mục)</small>
                        <strong class="text-success" id="calcAttitudeScore">4.0 / 5</strong>
                    </div>
                    <div class="col-6 col-sm-3 score-summary-item">
                        <small>Điểm TB quy đổi (Hệ 10)</small>
                        <strong class="text-dark" id="calcScore10">8.0 / 10.0</strong>
                    </div>
                    <div class="col-6 col-sm-3 text-sm-end score-summary-item">
                        <small class="mb-1">Xếp loại dự kiến</small>
                        <span class="badge bg-success fs-6 py-2 px-3" id="calcGradeBadge">Giỏi</span>
                    </div>
                </div>
            </div>

            <!-- NHẬN XÉT CỦA MENTOR -->
            <div class="mb-4">
                <label class="form-label fw-bold" for="evaluationComment">
                    <i class="bi bi-chat-left-text me-1 text-primary"></i> Nhận xét & Đánh giá tổng kết của Mentor <span class="text-danger">*</span>
                </label>
                <textarea class="form-control" id="evaluationComment" name="comment" rows="4" maxlength="3000"
                          placeholder="Nhập nhận xét tổng quan về tinh thần làm việc, kết quả thực tập, điểm mạnh, điểm cần khắc phục và định hướng phát triển cho TTS..." required></textarea>
                <div class="form-text text-muted text-end"><span id="charCount">0</span> / 3000 ký tự</div>
            </div>

            <button class="btn btn-brand btn-lg w-100" type="submit">
                <i class="bi bi-check2-circle me-1"></i> Lưu đánh giá kết quả thực tập
            </button>
        </form>
    </div>

    <!-- Lịch sử đánh giá -->
    <div class="fe3-panel">
        <h3 class="h5 fw-bold mb-3"><i class="bi bi-clock-history me-2 text-primary"></i>Lịch sử & Kết quả đánh giá</h3>
        <div id="evaluationResult" aria-live="polite">Vui lòng chọn đợt thực tập để xem kết quả.</div>
    </div>
</section>

<script>
    (function () {
        var SKILL_CRITERIA = [
            { id: 1, title: 'Kiến thức nền tảng & Tư duy lập trình', desc: 'Nắm vững kiến thức lập trình cơ bản, OOP, cấu trúc dữ liệu và giải thuật.' },
            { id: 2, title: 'Kỹ năng thiết kế & Phát triển ứng dụng', desc: 'Hiểu kiến trúc hệ thống (MVC/layered), luồng dữ liệu và cài đặt chức năng nghiệp vụ.' },
            { id: 3, title: 'Thao tác & Thiết kế Cơ sở dữ liệu', desc: 'Kỹ năng viết truy vấn SQL, thiết kế bảng quan hệ và tối ưu truy vấn dữ liệu.' },
            { id: 4, title: 'Quy chuẩn mã nguồn & Clean Code', desc: 'Viết code sạch, tuân thủ naming convention, cấu trúc hàm/lớp rõ ràng và dễ bảo trì.' },
            { id: 5, title: 'Kỹ năng Debug, xử lý lỗi & Ngoại lệ', desc: 'Khả năng phân tích log, phát hiện bug độc lập và đề xuất phương án khắc phục hiệu quả.' },
            { id: 6, title: 'Quản lý mã nguồn với Git & Phối hợp nhóm', desc: 'Sử dụng Git thành thạo (commit rõ ràng, quản lý branch, pull request) và phối hợp mã nguồn.' },
            { id: 7, title: 'Năng lực tự học & Đọc tài liệu kỹ thuật', desc: 'Chủ động tìm hiểu tài liệu kỹ thuật, thư viện mới và tiếp cận công nghệ nhanh chóng.' },
            { id: 8, title: 'Tiến độ & Mức độ hoàn thành nhiệm vụ', desc: 'Tinh thần cam kết deadline, hoàn thành đầy đủ các nhiệm vụ và tính đúng đắn của sản phẩm.' }
        ];

        var ATTITUDE_CRITERIA = [
            { id: 1, title: 'Tính chuyên cần & Kỷ luật giờ giấc', desc: 'Đi làm đúng giờ, tuân thủ lịch họp và thực hiện nghiêm túc nội quy công ty/dự án.' },
            { id: 2, title: 'Tinh thần trách nhiệm & Cam kết công việc', desc: 'Tận tâm với nhiệm vụ được giao, chủ động báo cáo tiến độ và nêu bật các vướng mắc.' },
            { id: 3, title: 'Kỹ năng giao tiếp & Làm việc nhóm', desc: 'Giao tiếp hòa nhã, cởi mở, lắng nghe và hợp tác hiệu quả trong tập thể.' },
            { id: 4, title: 'Tinh thần cầu tiến & Tiếp thu góp ý', desc: 'Lắng nghe chân thành, tích cực ghi nhận và tiếp thu phản hồi từ Mentor để cải thiện.' },
            { id: 5, title: 'Tính chủ động, trung thực & Hỗ trợ tập thể', desc: 'Chủ động tìm kiếm việc khi xong task, trung thực và sẵn sàng hỗ trợ các thành viên khác.' }
        ];

        function createCriterionCard(item, index, groupType) {
            var card = document.createElement('div');
            card.className = 'crit-card';
            card.dataset.critId = item.id;
            card.dataset.groupType = groupType;

            var header = document.createElement('div');
            header.className = 'crit-header';

            var titleWrap = document.createElement('div');
            var idxSpan = document.createElement('span');
            idxSpan.className = 'fw-bold me-1 ' + (groupType === 'skill' ? 'text-primary' : 'text-success');
            idxSpan.textContent = index + '.';
            var titleSpan = document.createElement('span');
            titleSpan.className = 'crit-title';
            titleSpan.textContent = item.title;
            titleWrap.appendChild(idxSpan);
            titleWrap.appendChild(titleSpan);
            header.appendChild(titleWrap);

            var btnGroup = document.createElement('div');
            btnGroup.className = 'score-btn-group';
            var scoreLabels = ['1 - Chưa đạt', '2 - Yếu', '3 - Đạt', '4 - Khá', '5 - Xuất sắc'];
            for (var val = 1; val <= 5; val++) {
                var btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'score-btn' + (val === 4 ? ' active' : '');
                btn.dataset.val = val;
                btn.title = scoreLabels[val - 1];
                btn.textContent = val + 'đ';
                (function (targetBtn, bGroup) {
                    targetBtn.addEventListener('click', function () {
                        bGroup.querySelectorAll('.score-btn').forEach(function (b) { b.classList.remove('active'); });
                        targetBtn.classList.add('active');
                        calculateOverall();
                    });
                })(btn, btnGroup);
                btnGroup.appendChild(btn);
            }
            header.appendChild(btnGroup);
            card.appendChild(header);

            var desc = document.createElement('div');
            desc.className = 'crit-desc';
            desc.textContent = item.desc;
            card.appendChild(desc);

            var commentWrap = document.createElement('div');
            commentWrap.className = 'input-group input-group-sm mt-2';
            var iconSpan = document.createElement('span');
            iconSpan.className = 'input-group-text bg-light text-muted';
            iconSpan.innerHTML = '<i class="bi bi-chat-quote"></i>';
            var input = document.createElement('input');
            input.type = 'text';
            input.className = 'form-control crit-comment';
            input.maxLength = 500;
            input.placeholder = 'Nhận xét riêng cho mục ' + index + ' (tùy chọn)...';
            commentWrap.appendChild(iconSpan);
            commentWrap.appendChild(input);
            card.appendChild(commentWrap);

            return card;
        }

        function renderCriteria() {
            var skillList = document.getElementById('skillsCriteriaList');
            var attitudeList = document.getElementById('attitudeCriteriaList');
            if (!skillList || !attitudeList) return;

            skillList.innerHTML = '';
            SKILL_CRITERIA.forEach(function (it, idx) {
                skillList.appendChild(createCriterionCard(it, idx + 1, 'skill'));
            });

            attitudeList.innerHTML = '';
            ATTITUDE_CRITERIA.forEach(function (it, idx) {
                attitudeList.appendChild(createCriterionCard(it, idx + 1, 'attitude'));
            });
        }

        function calculateOverall() {
            var skillCards = document.querySelectorAll('#skillsCriteriaList .crit-card');
            var attitudeCards = document.querySelectorAll('#attitudeCriteriaList .crit-card');

            var skillSum = 0;
            skillCards.forEach(function (card) {
                var act = card.querySelector('.score-btn.active');
                skillSum += act ? parseInt(act.dataset.val, 10) : 4;
            });
            var skillAvg = skillCards.length ? (skillSum / skillCards.length) : 4;

            var attitudeSum = 0;
            attitudeCards.forEach(function (card) {
                var act = card.querySelector('.score-btn.active');
                attitudeSum += act ? parseInt(act.dataset.val, 10) : 4;
            });
            var attitudeAvg = attitudeCards.length ? (attitudeSum / attitudeCards.length) : 4;

            var overallAvg = (skillAvg + attitudeAvg) / 2;
            var score10 = (overallAvg * 2).toFixed(1);

            // Update DOM displays
            var sGroupBadge = document.getElementById('skillGroupAvgBadge');
            var aGroupBadge = document.getElementById('attitudeGroupAvgBadge');
            if (sGroupBadge) sGroupBadge.textContent = 'TB: ' + skillAvg.toFixed(1) + ' / 5';
            if (aGroupBadge) aGroupBadge.textContent = 'TB: ' + attitudeAvg.toFixed(1) + ' / 5';

            var sDisplay = document.getElementById('calcSkillScore');
            var aDisplay = document.getElementById('calcAttitudeScore');
            var s10Display = document.getElementById('calcScore10');
            var badge = document.getElementById('calcGradeBadge');

            if (sDisplay) sDisplay.textContent = skillAvg.toFixed(1) + ' / 5';
            if (aDisplay) aDisplay.textContent = attitudeAvg.toFixed(1) + ' / 5';
            if (s10Display) s10Display.textContent = score10 + ' / 10.0';

            if (badge) {
                if (overallAvg >= 4.5) {
                    badge.className = 'badge bg-warning text-dark fs-6 py-2 px-3';
                    badge.textContent = 'Xuất sắc';
                } else if (overallAvg >= 3.8) {
                    badge.className = 'badge bg-success fs-6 py-2 px-3';
                    badge.textContent = 'Giỏi';
                } else if (overallAvg >= 3.0) {
                    badge.className = 'badge bg-primary fs-6 py-2 px-3';
                    badge.textContent = 'Khá';
                } else if (overallAvg >= 2.0) {
                    badge.className = 'badge bg-secondary fs-6 py-2 px-3';
                    badge.textContent = 'Trung bình';
                } else {
                    badge.className = 'badge bg-danger fs-6 py-2 px-3';
                    badge.textContent = 'Chưa đạt';
                }
            }

            // Sync hidden inputs for backend fallback
            var hiddenS = document.getElementById('evaluationSkills');
            var hiddenA = document.getElementById('evaluationAttitude');
            if (hiddenS) hiddenS.value = Math.max(1, Math.min(5, Math.round(skillAvg)));
            if (hiddenA) hiddenA.value = Math.max(1, Math.min(5, Math.round(attitudeAvg)));
        }

        window.buildEvaluationPayload = function (enrollmentId) {
            var skillCards = document.querySelectorAll('#skillsCriteriaList .crit-card');
            var attitudeCards = document.querySelectorAll('#attitudeCriteriaList .crit-card');

            var skillsData = [];
            var skillSum = 0;
            skillCards.forEach(function (card) {
                var id = parseInt(card.dataset.critId, 10);
                var title = card.querySelector('.crit-title').textContent.trim();
                var desc = card.querySelector('.crit-desc').textContent.trim();
                var activeBtn = card.querySelector('.score-btn.active');
                var score = activeBtn ? parseInt(activeBtn.dataset.val, 10) : 4;
                var commentInput = card.querySelector('.crit-comment');
                var comment = commentInput ? commentInput.value.trim() : '';
                skillSum += score;
                skillsData.push({ id: id, title: title, desc: desc, score: score, comment: comment });
            });

            var attitudeData = [];
            var attitudeSum = 0;
            attitudeCards.forEach(function (card) {
                var id = parseInt(card.dataset.critId, 10);
                var title = card.querySelector('.crit-title').textContent.trim();
                var desc = card.querySelector('.crit-desc').textContent.trim();
                var activeBtn = card.querySelector('.score-btn.active');
                var score = activeBtn ? parseInt(activeBtn.dataset.val, 10) : 4;
                var commentInput = card.querySelector('.crit-comment');
                var comment = commentInput ? commentInput.value.trim() : '';
                attitudeSum += score;
                attitudeData.push({ id: id, title: title, desc: desc, score: score, comment: comment });
            });

            var skillAvg = skillsData.length ? (skillSum / skillsData.length) : 4;
            var attitudeAvg = attitudeData.length ? (attitudeSum / attitudeData.length) : 4;
            var overallAvg = (skillAvg + attitudeAvg) / 2;
            var score10 = (overallAvg * 2).toFixed(1);

            var skillsScore = Math.max(1, Math.min(5, Math.round(skillAvg)));
            var attitudeScore = Math.max(1, Math.min(5, Math.round(attitudeAvg)));

            var overallComment = (document.getElementById('evaluationComment').value || '').trim();

            // Build human-readable breakdown
            var breakdownLines = [];
            breakdownLines.push('NHẬN XÉT CHUNG CỦA MENTOR:');
            breakdownLines.push(overallComment);
            breakdownLines.push('');
            breakdownLines.push('BẢNG ĐIỂM CHI TIẾT THEO TIÊU CHÍ:');
            breakdownLines.push('I. KỸ NĂNG & NĂNG LỰC CHUYÊN MÔN (TB: ' + skillAvg.toFixed(1) + '/5):');
            skillsData.forEach(function (it, idx) {
                var line = (idx + 1) + '. ' + it.title + ': ' + it.score + '/5';
                if (it.comment) line += ' - ' + it.comment;
                breakdownLines.push(line);
            });
            breakdownLines.push('');
            breakdownLines.push('II. Ý THỨC KỶ LUẬT & THÁI ĐỘ LÀM VIỆC (TB: ' + attitudeAvg.toFixed(1) + '/5):');
            attitudeData.forEach(function (it, idx) {
                var line = (idx + 1) + '. ' + it.title + ': ' + it.score + '/5';
                if (it.comment) line += ' - ' + it.comment;
                breakdownLines.push(line);
            });

            var meta = {
                skillAvg: Number(skillAvg.toFixed(2)),
                attitudeAvg: Number(attitudeAvg.toFixed(2)),
                score10: Number(score10),
                overallComment: overallComment,
                skills: skillsData,
                attitude: attitudeData
            };

            var fullComment = breakdownLines.join('\n') + '\n<!--EVAL_DATA:' + JSON.stringify(meta) + '-->';

            return {
                enrollmentId: enrollmentId,
                skillsScore: skillsScore,
                attitudeScore: attitudeScore,
                comment: fullComment
            };
        };

        window.resetEvaluationForm = function () {
            document.querySelectorAll('#skillsCriteriaList .crit-card, #attitudeCriteriaList .crit-card').forEach(function (card) {
                card.querySelectorAll('.score-btn').forEach(function (b) {
                    if (b.dataset.val === '4') b.classList.add('active');
                    else b.classList.remove('active');
                });
                var input = card.querySelector('.crit-comment');
                if (input) input.value = '';
            });
            var commentArea = document.getElementById('evaluationComment');
            if (commentArea) commentArea.value = '';
            var countSpan = document.getElementById('charCount');
            if (countSpan) countSpan.textContent = '0';
            calculateOverall();
        };

        document.addEventListener('DOMContentLoaded', function () {
            renderCriteria();
            calculateOverall();

            var commentArea = document.getElementById('evaluationComment');
            var countSpan = document.getElementById('charCount');
            if (commentArea && countSpan) {
                commentArea.addEventListener('input', function () {
                    countSpan.textContent = commentArea.value.length;
                });
            }
        });
    })();
</script>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
