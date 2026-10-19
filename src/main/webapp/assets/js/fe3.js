(function () {
    'use strict';

    var app = document.getElementById('fe3App');
    if (!app) return;

    var page = app.dataset.page;
    var ctx = app.dataset.context || '';
    var role = page.indexOf('mentor-') === 0 ? 'mentor'
        : page.indexOf('hr-') === 0 ? 'hr' : 'intern';
    var base = ctx + '/' + role + '/be4';
    var csrfToken = null;
    var enrollments = [];
    var currentReports = [];
    var selectedReport = null;

    function byId(id) { return document.getElementById(id); }

    function notice(message, kind) {
        var box = byId('fe3Notice');
        box.textContent = message;
        box.className = 'alert alert-' + (kind || 'danger');
        box.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    }

    function clearNotice() {
        var box = byId('fe3Notice');
        box.textContent = '';
        box.className = 'alert d-none';
    }

    function errorMessage(error) {
        notice(error && error.message ? error.message : 'Không thể tải dữ liệu. Vui lòng thử lại.');
    }

    async function api(path, params, form) {
        var url = base + path;
        if (params) url += '?' + new URLSearchParams(params).toString();
        var options = { credentials: 'same-origin', headers: { Accept: 'application/json' } };
        if (form) {
            if (!csrfToken) {
                var tokenResponse = await api('/csrf');
                csrfToken = tokenResponse.csrfToken;
            }
            options.method = 'POST';
            options.headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8';
            options.headers['X-CSRF-Token'] = csrfToken;
            options.body = new URLSearchParams(form).toString();
        }
        var response = await fetch(url, options);
        var type = response.headers.get('content-type') || '';
        if (response.redirected && response.url.indexOf('/auth/login') !== -1) {
            throw new Error('Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.');
        }
        if (type.indexOf('application/json') === -1) {
            throw new Error('API BE4 chưa có trên bản ứng dụng đang chạy hoặc phiên đăng nhập đã hết hạn.');
        }
        var data = await response.json();
        if (!response.ok) throw new Error(data && data.error ? data.error : 'Yêu cầu thất bại (' + response.status + ').');
        return data;
    }

    function node(tag, text, className) {
        var result = document.createElement(tag);
        if (text !== undefined && text !== null) result.textContent = String(text);
        if (className) result.className = className;
        return result;
    }

    function cell(row, text) { row.appendChild(node('td', text)); }
    function statusCell(row, value) {
        var holder = node('td');
        var badge = node('span', status(value), 'fe3-status fe3-status--'
            + String(value || '').toLowerCase().replace(/[^a-z_]/g, ''));
        holder.appendChild(badge);
        row.appendChild(holder);
    }
    function progressCell(row, percentage) {
        var holder = node('td');
        if (percentage == null) {
            holder.appendChild(node('span', 'Chưa cập nhật', 'fe3-muted'));
        } else {
            var line = node('div', null, 'fe3-progress');
            var fill = node('span');
            fill.style.width = Math.max(0, Math.min(100, percentage)) + '%';
            line.appendChild(fill);
            holder.appendChild(line);
            holder.appendChild(node('small', percentage + '%', 'fe3-progress__label'));
        }
        row.appendChild(holder);
    }
    function emptyRow(body, columns, message) {
        body.replaceChildren();
        var row = node('tr');
        var value = node('td', message);
        value.colSpan = columns;
        row.appendChild(value);
        body.appendChild(row);
    }

    function date(value) { return value ? new Date(value).toLocaleDateString('vi-VN') : '—'; }
    function moment(value) { return value ? new Date(value).toLocaleString('vi-VN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '—'; }

    function date(value) { return value ? String(value).slice(0, 10) : '—'; }
    function moment(value) {
        if (!value) return '—';
        if (/Z$|[+-]\d\d:\d\d$/.test(String(value))) {
            return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date(value));
        }
        return String(value).replace('T', ' ').slice(0, 16); // Legacy local timestamps retain their original meaning.
    }

    function status(value) {
        return ({ ACTIVE: 'Đang tham gia', COMPLETED: 'Hoàn thành', CANCELLED: 'Đã hủy',
            TODO: 'Chưa bắt đầu', IN_PROGRESS: 'Đang thực hiện', DONE: 'Hoàn thành',
            DRAFT: 'Bản nháp', SUBMITTED: 'Đã nộp', REVIEWED: 'Đã phản hồi',
            OPEN: 'Đang mở', CLOSED: 'Đã đóng' })[value] || value || '—';
    }
    function link(path, params) { return ctx + path + (params ? '?' + new URLSearchParams(params).toString() : ''); }
    function currentEnrollment() {
        var select = byId('enrollmentSelect');
        return enrollments.find(function (item) { return String(item.enrollmentId) === select.value; }) || null;
    }
    function requireEnrollment() {
        var value = currentEnrollment();
        if (!value) throw new Error('Hãy chọn đợt thực tập.');
        return value;
    }
    function formValues(form) { return Object.fromEntries(new FormData(form).entries()); }
    async function submitting(form, work) {
        var button = form.querySelector('button[type="submit"]');
        if (button) button.disabled = true;
        try { await work(); } catch (error) { errorMessage(error); }
        finally { if (button) button.disabled = false; }
    }

    async function loadEnrollments(onChange) {
        enrollments = await api('/enrollments');
        var select = byId('enrollmentSelect');
        select.replaceChildren();
        if (!enrollments.length) {
            select.appendChild(node('option', 'Chưa có đợt thực tập'));
            select.disabled = true;
            await onChange();
            return;
        }
        enrollments.forEach(function (entry) {
            var option = node('option', entry.internName + ' — ' + entry.programName + ' (' + status(entry.status) + ')');
            option.value = entry.enrollmentId;
            select.appendChild(option);
        });
        var requested = new URLSearchParams(window.location.search).get('enrollmentId');
        var preferred = enrollments.find(function (item) { return String(item.enrollmentId) === requested; })
            || enrollments.find(function (item) { return item.status === 'ACTIVE'; }) || enrollments[0];
        select.value = preferred.enrollmentId;
        select.addEventListener('change', function () { clearNotice(); onChange().catch(errorMessage); });
        await onChange();
    }

    async function mentorDashboard() {
        var results = await Promise.all([api('/dashboard'), api('/enrollments')]);
        var metrics = results[0];
        var rows = results[1];
        ['activeInterns', 'tasksTodo', 'tasksInProgress', 'tasksDone',
            'reportsSubmitted', 'reportsReviewed', 'evaluationsCompleted'].forEach(function (key) {
            var target = byId('metric' + key.charAt(0).toUpperCase() + key.slice(1));
            target.textContent = metrics[key] == null ? '0' : metrics[key];
        });
        var body = byId('enrollmentRows');
        if (!rows.length) { emptyRow(body, 5, 'Chưa được phân công TTS.'); return; }
        body.replaceChildren();
        rows.forEach(function (entry) {
            var row = node('tr');
            cell(row, entry.internName);
            cell(row, entry.programName);
            cell(row, date(entry.startDate) + ' – ' + date(entry.endDate));
            statusCell(row, entry.status);
            var action = node('td');
            var anchor = node('a', 'Xem nhiệm vụ', 'btn btn-outline-primary btn-sm');
            anchor.href = link('/mentor/tasks', { enrollmentId: entry.enrollmentId });
            action.appendChild(anchor);
            row.appendChild(action);
            body.appendChild(row);
        });
    }

    async function mentorTasks() {
        var shownAssignment = null;
        async function showProgress(assignmentId) {
            shownAssignment = assignmentId;
            var panel = byId('progressPanel');
            panel.classList.remove('d-none');
            panel.replaceChildren(node('h3', 'Lịch sử tiến độ', 'h6'));
            var history = await api('/progress', { assignmentId: assignmentId });
            if (!history.length) panel.appendChild(node('p', 'Chưa có cập nhật.'));
            history.forEach(function (entry) {
                panel.appendChild(node('p', entry.completionPercentage + '% · ' + moment(entry.createdAt)
                    + ' · ' + (entry.updatedBy == null ? 'Chưa ghi tác giả (dữ liệu cũ)' : 'Tài khoản #' + entry.updatedBy)
                    + (entry.note ? ' · ' + entry.note : '')));
            });
        }
        async function reload() {
            var entry = currentEnrollment();
            var body = byId('taskRows');
            byId('progressPanel').classList.add('d-none');
            if (!entry) { emptyRow(body, 5, 'Chưa có TTS được phân công.'); return; }
            var results = await Promise.all([
                api('/tasks', { enrollmentId: entry.enrollmentId }),
                api('/assignments', { enrollmentId: entry.enrollmentId })
            ]);
            var tasks = new Map(results[0].map(function (task) { return [task.taskId, task]; }));
            var assigned = results[1];
            if (!assigned.length) { emptyRow(body, 5, 'Chưa có nhiệm vụ được giao.'); return; }
            var histories = await Promise.all(assigned.map(function (assignment) {
                return api('/progress', { assignmentId: assignment.assignmentId });
            }));
            body.replaceChildren();
            assigned.forEach(function (assignment, index) {
                var task = tasks.get(assignment.taskId);
                var percentages = histories[index].map(function (item) { return item.completionPercentage; });
                var progress = percentages.length ? Math.max.apply(null, percentages) : null;
                var row = node('tr');
                cell(row, task ? task.title : 'Task #' + assignment.taskId);
                cell(row, task ? date(task.dueDate) : '—');
                statusCell(row, assignment.status);
                progressCell(row, progress);
                var action = node('td');
                var button = node('button', 'Lịch sử', 'btn btn-outline-primary btn-sm');
                button.type = 'button';
                button.addEventListener('click', function () { showProgress(assignment.assignmentId).catch(errorMessage); });
                action.appendChild(button);
                row.appendChild(action);
                body.appendChild(row);
            });
            if (shownAssignment && assigned.some(function (item) { return item.assignmentId === shownAssignment; })) {
                await showProgress(shownAssignment);
            }
        }
        await loadEnrollments(reload);
        byId('taskCreateForm').addEventListener('submit', function (event) {
            event.preventDefault();
            submitting(event.currentTarget, async function () {
                var entry = requireEnrollment();
                if (entry.status !== 'ACTIVE') throw new Error('Chỉ giao task cho đợt đang tham gia.');
                var values = formValues(byId('taskCreateForm'));
                if (!values.description.trim() || !values.dueDate) throw new Error('Cần mô tả và hạn hoàn thành nằm trong thời gian chương trình.');
                var created = await api('/tasks/create-assign', null, Object.assign(values, {enrollmentId: entry.enrollmentId}));
                var task = created.task;
                byId('taskId').value = task.taskId;
                byId('taskCreateForm').reset();
                notice('Đã tạo và giao task #' + task.taskId + '.', 'success');
                await reload();
            });
        });
        byId('taskAssignForm').addEventListener('submit', function (event) {
            event.preventDefault();
            submitting(event.currentTarget, async function () {
                var entry = requireEnrollment();
                if (entry.status !== 'ACTIVE') throw new Error('Chỉ giao task cho đợt đang tham gia.');
                await api('/assignments', null, { taskId: byId('taskId').value, enrollmentId: entry.enrollmentId });
                notice('Đã giao task cho TTS.', 'success');
                await reload();
            });
        });
    }

    async function mentorReports() {
        async function select(report) {
            selectedReport = report;
            byId('reportDetail').textContent = 'Tuần ' + report.weekNumber + ' · ' + status(report.status)
                + '\nNộp lúc: ' + moment(report.submittedAt) + '\n\n' + (report.content || 'Không có nội dung.');
            var list = byId('feedbackList');
            list.replaceChildren();
            var feedback = await api('/feedback', { reportId: report.reportId });
            if (!feedback.length) list.appendChild(node('p', 'Chưa có phản hồi.'));
            feedback.forEach(function (item) {
                var panel = node('div', null, 'fe3-feedback');
                panel.appendChild(node('strong', 'Người phản hồi #' + item.reviewerUserId + ' · ' + moment(item.createdAt)
                    + (item.rating ? ' · ' + item.rating + '/5' : '')));
                panel.appendChild(node('p', item.comment || 'Không có nhận xét.'));
                list.appendChild(panel);
            });
            byId('feedbackForm').classList.toggle('d-none',
                !['SUBMITTED', 'REVIEWED'].includes(report.status) || requireEnrollment().status !== 'ACTIVE');
        }
        async function reload() {
            var entry = currentEnrollment();
            var list = byId('reportList');
            list.replaceChildren();
            byId('reportDetail').textContent = 'Chọn một báo cáo.';
            byId('feedbackList').replaceChildren();
            byId('feedbackForm').classList.add('d-none');
            selectedReport = null;
            if (!entry) { list.textContent = 'Chưa có TTS được phân công.'; return; }
            currentReports = (await api('/reports', { enrollmentId: entry.enrollmentId }))
                .filter(function (report) { return report.status !== 'DRAFT'; });
            if (!currentReports.length) { list.textContent = 'Chưa có báo cáo đã nộp.'; return; }
            currentReports.forEach(function (report) {
                var button = node('button', 'Tuần ' + report.weekNumber + ' · ' + status(report.status), 'fe3-list-button');
                button.type = 'button';
                button.addEventListener('click', function () { select(report).catch(errorMessage); });
                list.appendChild(button);
            });
        }
        await loadEnrollments(reload);
        byId('feedbackForm').addEventListener('submit', function (event) {
            event.preventDefault();
            submitting(event.currentTarget, async function () {
                if (!selectedReport) throw new Error('Hãy chọn báo cáo.');
                var values = formValues(byId('feedbackForm'));
                if (!values.comment.trim()) throw new Error('Cần nhập nhận xét có nội dung.');
                await api('/feedback', null, {
                    reportId: selectedReport.reportId, comment: values.comment, rating: values.rating
                });
                byId('feedbackForm').reset();
                notice('Đã gửi phản hồi.', 'success');
                await reload();
            });
        });
    }

    function gradeBadge(avg) {
        if (avg >= 4.5) return { text: 'Xuất sắc', cls: 'badge bg-warning text-dark', icon: '', gpa: 'A' };
        if (avg >= 3.8) return { text: 'Giỏi', cls: 'badge bg-success text-white', icon: '', gpa: 'B+' };
        if (avg >= 3.0) return { text: 'Khá', cls: 'badge bg-primary text-white', icon: '', gpa: 'B' };
        if (avg >= 2.0) return { text: 'Trung bình', cls: 'badge bg-secondary text-white', icon: '', gpa: 'C' };
        return { text: 'Chưa đạt', cls: 'badge bg-danger text-white', icon: '', gpa: 'D' };
    }

    function renderAssessmentCard(result, entry, isLatest) {
        var card = node('article', null, 'eval-card-history p-3 mb-3 border rounded shadow-sm bg-white');

        var skills = Number(result.skillsScore) || 0;
        var attitude = Number(result.attitudeScore) || 0;
        var avg = (skills + attitude) / 2;
        var score10 = (avg * 2).toFixed(1);
        var grade = gradeBadge(avg);

        // Try extracting structured metadata if present
        var evalData = null;
        var match = (result.comment || '').match(/<!--EVAL_DATA:([\s\S]*?)-->/);
        if (match) {
            try {
                evalData = JSON.parse(match[1]);
            } catch (e) {
                evalData = null;
            }
        }

        if (evalData && evalData.score10 != null) {
            score10 = Number(evalData.score10).toFixed(1);
            if (evalData.skillAvg != null && evalData.attitudeAvg != null) {
                avg = (Number(evalData.skillAvg) + Number(evalData.attitudeAvg)) / 2;
                grade = gradeBadge(avg);
            }
        }

        // Header
        var header = node('div', null, 'd-flex flex-wrap justify-content-between align-items-center mb-3 pb-2 border-bottom');
        var meta = node('div');
        if (isLatest) {
            meta.appendChild(node('span', 'MỚI NHẤT', 'badge bg-success me-2'));
        }
        meta.appendChild(node('strong', 'Mentor: ' + (result.mentorName || 'Hướng dẫn')));
        meta.appendChild(node('span', ' · ' + moment(result.createdAt), 'text-muted small ms-2'));
        header.appendChild(meta);

        var gradePill = node('span', grade.text, grade.cls + ' fs-6 px-3 py-1 rounded-pill');
        header.appendChild(gradePill);
        card.appendChild(header);

        // Score summary
        var grid = node('div', null, 'row g-2 mb-3 text-center');

        var col1 = node('div', null, 'col-4');
        var b1 = node('div', null, 'p-2 rounded bg-light border');
        b1.appendChild(node('small', 'Kỹ năng chuyên môn', 'text-muted d-block small'));
        var sLabel = (evalData && evalData.skillAvg != null) ? Number(evalData.skillAvg).toFixed(1) + ' / 5' : (skills + ' / 5');
        b1.appendChild(node('strong', sLabel, 'text-primary fs-5'));
        col1.appendChild(b1);
        grid.appendChild(col1);

        var col2 = node('div', null, 'col-4');
        var b2 = node('div', null, 'p-2 rounded bg-light border');
        b2.appendChild(node('small', 'Ý thức & Thái độ', 'text-muted d-block small'));
        var aLabel = (evalData && evalData.attitudeAvg != null) ? Number(evalData.attitudeAvg).toFixed(1) + ' / 5' : (attitude + ' / 5');
        b2.appendChild(node('strong', aLabel, 'text-success fs-5'));
        col2.appendChild(b2);
        grid.appendChild(col2);

        var col3 = node('div', null, 'col-4');
        var b3 = node('div', null, 'p-2 rounded bg-light border');
        b3.appendChild(node('small', 'Điểm quy đổi (Hệ 10)', 'text-muted d-block small'));
        b3.appendChild(node('strong', score10 + ' / 10.0', 'text-dark fs-5'));
        col3.appendChild(b3);
        grid.appendChild(col3);

        card.appendChild(grid);

        // Mentor genuine comment
        var rawComment = (evalData && evalData.overallComment)
            ? evalData.overallComment
            : (result.comment || '')
                .replace(/<!--EVAL_DATA:(.*?)-->/g, '')
                .replace(/BẢNG ĐIỂM CHI TIẾT[\s\S]*/g, '')
                .replace(/===.*?===[\s\S]*/g, '')
                .replace(/^NHẬN XÉT CHUNG CỦA MENTOR:\s*/i, '')
                .trim();

        var commentBox = node('div', null, 'mt-2 p-3 rounded bg-light border-start border-3 border-primary');
        commentBox.appendChild(node('div', 'Nhận xét chung của Mentor:', 'fw-bold small text-muted mb-1'));
        commentBox.appendChild(node('p', rawComment || 'Không có nhận xét.', 'mb-0 fe3-report-content'));
        card.appendChild(commentBox);

        // Render detailed criteria tables if available
        if (evalData && ((evalData.skills && evalData.skills.length) || (evalData.attitude && evalData.attitude.length))) {
            var detailWrapper = node('div', null, 'mt-3 pt-3 border-top');

            function scoreName(s) {
                var val = Number(s);
                if (val === 5) return '5/5 - Xuất sắc';
                if (val === 4) return '4/5 - Khá';
                if (val === 3) return '3/5 - Đạt';
                if (val === 2) return '2/5 - Yếu';
                return '1/5 - Chưa đạt';
            }

            function scoreBadgeClass(s) {
                var val = Number(s);
                if (val >= 5) return 'badge bg-success-subtle text-success border border-success-subtle';
                if (val >= 4) return 'badge bg-primary-subtle text-primary border border-primary-subtle';
                if (val >= 3) return 'badge bg-info-subtle text-info-emphasis border border-info-subtle';
                if (val >= 2) return 'badge bg-warning-subtle text-warning-emphasis border border-warning-subtle';
                return 'badge bg-danger-subtle text-danger border border-danger-subtle';
            }

            function renderCriteriaTable(title, items) {
                if (!items || !items.length) return;
                var sec = node('div', null, 'mb-3');
                sec.appendChild(node('div', title, 'fw-bold small text-secondary mb-2'));
                var tableDiv = node('div', null, 'table-responsive');
                var table = node('table', null, 'table table-sm table-bordered align-middle bg-white mb-0');
                var thead = node('thead', null, 'table-light small');
                var thr = node('tr');
                var th1 = node('th', 'Tiêu chí đánh giá');
                th1.style.width = '42%';
                thr.appendChild(th1);
                var th2 = node('th', 'Mức điểm', 'text-center');
                th2.style.width = '20%';
                thr.appendChild(th2);
                var th3 = node('th', 'Nhận xét chi tiết');
                th3.style.width = '38%';
                thr.appendChild(th3);
                thead.appendChild(thr);
                table.appendChild(thead);

                var tbody = node('tbody', null, 'small');
                items.forEach(function (it, idx) {
                    var tr = node('tr');
                    var tdName = node('td');
                    tdName.appendChild(node('strong', (idx + 1) + '. ' + it.title, 'd-block text-dark'));
                    if (it.desc) {
                        tdName.appendChild(node('span', it.desc, 'text-muted d-block small'));
                    }
                    tr.appendChild(tdName);

                    var tdScore = node('td', null, 'text-center');
                    tdScore.appendChild(node('span', scoreName(it.score), scoreBadgeClass(it.score)));
                    tr.appendChild(tdScore);

                    var tdComm = node('td');
                    if (it.comment && it.comment.trim()) {
                        tdComm.appendChild(node('span', it.comment.trim(), 'text-dark fst-italic'));
                    } else {
                        tdComm.appendChild(node('span', '—', 'text-muted'));
                    }
                    tr.appendChild(tdComm);
                    tbody.appendChild(tr);
                });
                table.appendChild(tbody);
                tableDiv.appendChild(table);
                sec.appendChild(tableDiv);
                detailWrapper.appendChild(sec);
            }

            renderCriteriaTable('I. Kỹ năng chuyên môn & Năng lực thực hiện (8 tiêu chí)', evalData.skills);
            renderCriteriaTable('II. Ý thức kỷ luật & Thái độ làm việc (5 tiêu chí)', evalData.attitude);
            card.appendChild(detailWrapper);
        }

        return card;
    }

    function showEvaluation(result, target) {
        target.replaceChildren();
        if (!result) { target.appendChild(node('p', 'Chưa có đánh giá cuối kỳ.')); return; }
        var scoreVal = Number(result.score) || 0;
        var avg5 = scoreVal / 20;
        var grade = gradeBadge(avg5);
        var box = node('div', null, 'p-3 rounded bg-white border');
        var head = node('div', null, 'd-flex justify-content-between align-items-center mb-2');
        head.appendChild(node('h4', 'Kết quả: ' + scoreVal + ' / 100', 'h5 mb-0 fw-bold text-primary'));
        head.appendChild(node('span', grade.text, grade.cls + ' fs-6 px-3 py-1 rounded-pill'));
        box.appendChild(head);
        box.appendChild(node('p', 'Ngày đánh giá: ' + moment(result.createdAt), 'text-muted small mb-2'));
        box.appendChild(node('p', result.comment || 'Không có nhận xét.', 'fe3-report-content mb-0 p-2 bg-light rounded'));
        target.appendChild(box);
    }

    async function evaluationPage(mentor) {
        async function reload() {
            var entry = currentEnrollment();
            var form = mentor ? byId('evaluationForm') : null;
            if (form) form.classList.add('d-none');
            var target = byId('evaluationResult');
            if (!entry) { target.textContent = 'Chưa có đợt thực tập.'; return; }
            var results = await Promise.all([api('/assessments', { enrollmentId: entry.enrollmentId }), api('/evaluation', { enrollmentId: entry.enrollmentId })]);
            target.replaceChildren();

            var headerDiv = node('div', null, 'd-flex flex-wrap justify-content-between align-items-center mb-3 gap-2');
            headerDiv.appendChild(node('h3', 'Lịch sử & Kết quả đánh giá', 'h5 fw-bold mb-0'));

            var rightTools = node('div', null, 'd-flex align-items-center gap-2');
            if (results[0].length) {
                rightTools.appendChild(node('span', results[0].length + ' bản ghi đánh giá', 'badge bg-primary-subtle text-primary border'));
                if (!mentor && entry.status === 'COMPLETED' && typeof window.showInternCertificate === 'function') {
                    var latest = results[0][0];
                    var evalData = null;
                    var match = (latest.comment || '').match(/<!--EVAL_DATA:([\s\S]*?)-->/);
                    if (match) {
                        try { evalData = JSON.parse(match[1]); } catch (e) { evalData = null; }
                    }
                    var s10 = (evalData && evalData.score10 != null)
                        ? Number(evalData.score10).toFixed(1)
                        : (((Number(latest.skillsScore) + Number(latest.attitudeScore)) / 2) * 2).toFixed(1);
                    var g = gradeBadge((Number(latest.skillsScore) + Number(latest.attitudeScore)) / 2);

                    var certBtn = node('button', null, 'btn btn-outline-success btn-sm');
                    certBtn.type = 'button';
                    certBtn.innerHTML = '<i class="bi bi-award me-1"></i> Giấy chứng nhận';
                    certBtn.addEventListener('click', function () {
                        window.showInternCertificate({
                            internName: entry.internName,
                            programName: entry.programName,
                            dates: date(entry.startDate) + ' – ' + date(entry.endDate),
                            score10: s10,
                            grade: g.text,
                            mentorName: latest.mentorName
                        });
                    });
                    rightTools.appendChild(certBtn);
                }
            }
            headerDiv.appendChild(rightTools);
            target.appendChild(headerDiv);

            if (!results[0].length && !results[1]) {
                target.appendChild(node('div', 'Chưa có đánh giá nào cho đợt thực tập này.', 'alert alert-info'));
            } else {
                results[0].forEach(function (result, index) {
                    target.appendChild(renderAssessmentCard(result, entry, index === 0));
                });
            }

            if (results[1] && !results[0].length) {
                var legacy = node('section', null, 'fe3-feedback mt-3 p-3 border rounded bg-light');
                legacy.appendChild(node('h4', 'Đánh giá thang 100 (Hệ thống cũ)', 'h6 fw-bold text-muted mb-2'));
                var body = node('div');
                showEvaluation(results[1], body);
                legacy.appendChild(body);
                target.appendChild(legacy);
            }

            var statusBadge = byId('activeStatusBadge');
            if (statusBadge) {
                statusBadge.textContent = entry.status === 'ACTIVE' ? 'Đang thực tập (Có thể đánh giá)' : status(entry.status);
                statusBadge.className = 'badge ' + (entry.status === 'ACTIVE' ? 'bg-success' : 'bg-secondary');
            }

            if (form && entry.status === 'ACTIVE') form.classList.remove('d-none');
        }
        await loadEnrollments(reload);
        if (mentor) {
            byId('evaluationForm').addEventListener('submit', function (event) {
                event.preventDefault();
                submitting(event.currentTarget, async function () {
                    var entry = requireEnrollment();
                    if (entry.status !== 'ACTIVE') throw new Error('Chỉ đánh giá đợt đang tham gia.');
                    var payload = (typeof window.buildEvaluationPayload === 'function')
                        ? window.buildEvaluationPayload(entry.enrollmentId)
                        : formValues(byId('evaluationForm'));
                    if (!payload.comment || !payload.comment.trim()) throw new Error('Cần nhập nhận xét.');
                    if (payload.comment.length>10000) throw new Error('Nhận xét và bảng tiêu chí vượt 10.000 ký tự. Vui lòng rút gọn nội dung.');
                    await api('/assessments', null, {
                        enrollmentId: entry.enrollmentId,
                        skillsScore: payload.skillsScore,
                        attitudeScore: payload.attitudeScore,
                        comment: payload.comment.trim()
                    });
                    notice('Đã lưu đánh giá kết quả thực tập thành công.', 'success');
                    if (typeof window.resetEvaluationForm === 'function') {
                        window.resetEvaluationForm();
                    }
                    await reload();
                });
            });
        }
    }

    async function internTasks() {
        async function reload() {
            var entry = currentEnrollment();
            var body = byId('taskRows');
            if (!entry) { emptyRow(body, 4, 'Bạn chưa tham gia chương trình nào.'); return; }
            var results = await Promise.all([
                api('/tasks', { enrollmentId: entry.enrollmentId }),
                api('/assignments', { enrollmentId: entry.enrollmentId })
            ]);
            var tasks = new Map(results[0].map(function (task) { return [task.taskId, task]; }));
            if (!results[1].length) { emptyRow(body, 4, 'Chưa có nhiệm vụ được giao.'); return; }
            body.replaceChildren();
            results[1].forEach(function (assignment) {
                var task = tasks.get(assignment.taskId);
                var row = node('tr');
                cell(row, task ? task.title : 'Task #' + assignment.taskId);
                cell(row, task ? date(task.dueDate) : '—');
                statusCell(row, assignment.status);
                var action = node('td');
                var anchor = node('a', 'Xem chi tiết', 'btn btn-outline-primary btn-sm');
                anchor.href = link('/intern/tasks/detail', {
                    enrollmentId: entry.enrollmentId, assignmentId: assignment.assignmentId
                });
                action.appendChild(anchor);
                row.appendChild(action);
                body.appendChild(row);
            });
        }
        await loadEnrollments(reload);
    }

    async function internTaskDetail() {
        var params = new URLSearchParams(window.location.search);
        var enrollmentId = Number(params.get('enrollmentId'));
        var assignmentId = Number(params.get('assignmentId'));
        if (!Number.isSafeInteger(enrollmentId) || enrollmentId <= 0
                || !Number.isSafeInteger(assignmentId) || assignmentId <= 0) {
            throw new Error('Liên kết nhiệm vụ không hợp lệ. Hãy mở lại từ danh sách task.');
        }
        async function reload() {
            var results = await Promise.all([
                api('/enrollments'),
                api('/assignments', { enrollmentId: enrollmentId }),
                api('/tasks', { enrollmentId: enrollmentId }),
                api('/progress', { assignmentId: assignmentId })
            ]);
            var entry = results[0].find(function (item) { return item.enrollmentId === enrollmentId; });
            var assignment = results[1].find(function (item) { return item.assignmentId === assignmentId; });
            var task = assignment && results[2].find(function (item) { return item.taskId === assignment.taskId; });
            if (!entry || !assignment || !task) throw new Error('Không tìm thấy nhiệm vụ này trong đợt thực tập của bạn.');
            byId('taskTitle').textContent = task.title;
            byId('taskDescription').textContent = task.description || 'Không có mô tả.';
            byId('taskDueDate').textContent = date(task.dueDate);
            byId('taskStatus').textContent = status(assignment.status);
            var history = byId('progressHistory');
            history.replaceChildren();
            if (!results[3].length) history.textContent = 'Chưa có cập nhật tiến độ.';
            results[3].forEach(function (item) {
                var panel = node('div', null, 'fe3-feedback');
                panel.appendChild(node('strong', item.completionPercentage + '% · ' + moment(item.createdAt)));
                panel.appendChild(node('small', item.updatedBy == null ? 'Chưa ghi tác giả (dữ liệu cũ)' : 'Tài khoản #' + item.updatedBy));
                panel.appendChild(node('p', item.note || 'Không có ghi chú.'));
                history.appendChild(panel);
            });
            var writable = entry.status === 'ACTIVE' && assignment.status !== 'DONE' && task.status === 'OPEN';
            byId('progressForm').classList.toggle('d-none', !writable);
            byId('progressReadOnly').textContent = writable ? '' : 'Nhiệm vụ này chỉ còn quyền xem.';
            if (writable) {
                var latest = results[3].length ? Math.max.apply(null, results[3].map(function (item) {
                    return item.completionPercentage;
                })) : 0;
                byId('completionPercentage').min = latest;
                byId('completionPercentage').value = latest;
            }
        }
        await reload();
        byId('progressForm').addEventListener('submit', function (event) {
            event.preventDefault();
            submitting(event.currentTarget, async function () {
                var values = formValues(byId('progressForm'));
                await api('/progress', null, {
                    assignmentId: assignmentId,
                    completionPercentage: values.completionPercentage,
                    note: values.note
                });
                notice('Đã cập nhật tiến độ.', 'success');
                byId('progressNote').value = '';
                await reload();
            });
        });
    }

    async function internReports() {
        async function select(report) {
            selectedReport = report;
            byId('reportDetail').textContent = 'Tuần ' + report.weekNumber + ' · ' + status(report.status)
                + '\nNgày nộp: ' + moment(report.submittedAt) + '\n\n' + (report.content || 'Bản nháp chưa có nội dung.');
            var editable = report.status === 'DRAFT' && requireEnrollment().status === 'ACTIVE';
            byId('reportEditForm').classList.toggle('d-none', !editable);
            if (editable) {
                byId('editReportResults').value = report.results === null ? (report.content || '') : (report.results || '');
                byId('editReportDifficulties').value = report.difficulties || '';
                byId('editReportNextPlan').value = report.nextPlan || '';
                byId('legacyReportNote').classList.toggle('d-none', !report.legacyContent);
            }
            var feedbackList = byId('feedbackList');
            feedbackList.replaceChildren();
            var feedback = await api('/feedback', { reportId: report.reportId });
            if (!feedback.length) feedbackList.appendChild(node('p', 'Chưa có phản hồi.'));
            feedback.forEach(function (item) {
                var panel = node('div', null, 'fe3-feedback');
                panel.appendChild(node('strong', 'Người phản hồi #' + item.reviewerUserId + ' · ' + moment(item.createdAt)
                    + (item.rating ? ' · ' + item.rating + '/5' : '')));
                panel.appendChild(node('p', item.comment || 'Không có nhận xét.'));
                feedbackList.appendChild(panel);
            });
        }
        async function reload(selectId) {
            var entry = currentEnrollment();
            var list = byId('reportList');
            list.replaceChildren();
            byId('reportDetail').textContent = 'Chọn một báo cáo.';
            byId('reportEditForm').classList.add('d-none');
            byId('feedbackList').replaceChildren();
            selectedReport = null;
            byId('reportCreateForm').classList.toggle('d-none', !entry || entry.status !== 'ACTIVE');
            if (!entry) { list.textContent = 'Bạn chưa tham gia chương trình nào.'; return; }
            currentReports = await api('/reports', { enrollmentId: entry.enrollmentId });
            if (!currentReports.length) { list.textContent = 'Chưa có báo cáo.'; return; }
            currentReports.forEach(function (report) {
                var button = node('button', 'Tuần ' + report.weekNumber + ' · ' + status(report.status), 'fe3-list-button');
                button.type = 'button';
                button.addEventListener('click', function () { select(report).catch(errorMessage); });
                list.appendChild(button);
            });
            var chosen = currentReports.find(function (report) { return report.reportId === selectId; });
            if (chosen) await select(chosen);
        }
        await loadEnrollments(reload);
        byId('reportCreateForm').addEventListener('submit', function (event) {
            event.preventDefault();
            submitting(event.currentTarget, async function () {
                var entry = requireEnrollment();
                if (entry.status !== 'ACTIVE') throw new Error('Đợt thực tập không còn hoạt động.');
                var values = formValues(byId('reportCreateForm'));
                var created = await api('/reports', null, {
                    enrollmentId: entry.enrollmentId, weekNumber: values.weekNumber, results: values.results, difficulties: values.difficulties, nextPlan: values.nextPlan
                });
                byId('reportCreateForm').reset();
                notice('Đã lưu báo cáo nháp tuần ' + created.weekNumber + '.', 'success');
                await reload(created.reportId);
            });
        });
        byId('reportEditForm').addEventListener('submit', function (event) {
            event.preventDefault();
            submitting(event.currentTarget, async function () {
                if (!selectedReport) throw new Error('Hãy chọn báo cáo.');
                var reportId = selectedReport.reportId;
                await api('/reports/update', null, Object.assign(formValues(byId('reportEditForm')), {reportId: reportId}));
                notice('Đã lưu chỉnh sửa.', 'success');
                await reload(reportId);
            });
        });
        byId('reportSubmitButton').addEventListener('click', async function (event) {
            var button = event.currentTarget;
            button.disabled = true;
            try {
                if (!selectedReport) throw new Error('Hãy chọn báo cáo.');
                var values = formValues(byId('reportEditForm'));
                if (!values.results.trim() || !values.difficulties.trim() || !values.nextPlan.trim()) throw new Error('Điền đủ kết quả, khó khăn và kế hoạch trước khi nộp.');
                if (values.results !== (selectedReport.results || '') || values.difficulties !== (selectedReport.difficulties || '') || values.nextPlan !== (selectedReport.nextPlan || '')) {
                    throw new Error('Nội dung đã thay đổi. Hãy lưu chỉnh sửa trước khi nộp.');
                }
                if (!window.confirm('Nộp báo cáo tuần ' + selectedReport.weekNumber + '? Sau khi nộp sẽ không sửa được.')) return;
                await api('/reports/submit', null, { reportId: selectedReport.reportId });
                notice('Đã nộp báo cáo.', 'success');
                await reload(selectedReport.reportId);
            } catch (error) { errorMessage(error); }
            finally { button.disabled = false; }
        });
    }

    async function hrDashboard() {
        var data = await api('/dashboard');
        byId('metricTotalInterns').textContent = data.totalInterns ?? 0;
        byId('metricActiveEnrollments').textContent = data.activeEnrollments ?? 0;
        byId('metricCompletedEnrollments').textContent = data.completedEnrollments ?? 0;
        byId('metricCompletionRatePercent').textContent =
            (data.completionRatePercent ?? 0) + '%';

        function renderRanks(targetId, values, emptyMessage) {
            var target = byId(targetId);
            target.replaceChildren();
            var entries = Array.isArray(values) ? values : [];
            if (!entries.length) {
                target.appendChild(node('p', emptyMessage, 'fe3-muted mb-0'));
                return;
            }
            var maximum = Math.max(1, ...entries.map(function (entry) {
                return Number(entry.count) || 0;
            }));
            entries.forEach(function (entry) {
                var item = node('div', null, 'fe3-rank-item');
                var heading = node('div', null, 'fe3-rank-heading');
                heading.appendChild(node('span', entry.label || 'Chưa xác định'));
                heading.appendChild(node('strong', Number(entry.count) || 0));
                item.appendChild(heading);
                var track = node('div', null, 'fe3-rank-track');
                var fill = node('span');
                fill.style.width = Math.max(0, Math.min(100,
                    (Number(entry.count) || 0) / maximum * 100)) + '%';
                track.appendChild(fill);
                item.appendChild(track);
                target.appendChild(item);
            });
        }

        renderRanks('universityStats', data.internsByUniversity, 'Chưa có dữ liệu trường.');
        renderRanks('majorStats', data.internsByMajor, 'Chưa có dữ liệu ngành.');
        renderRanks('mentorStats', data.activeInternsByMentor, 'Chưa có TTS được phân mentor.');
    }

    function summaryDisplayScore100(entry) {
        if(Number.isInteger(entry.skillScore) && Number.isInteger(entry.attitudeScore))
            return (entry.skillScore+entry.attitudeScore)*10;
        return entry.score===null || entry.score===undefined?NaN:Number(entry.score);
    }

    async function hrEvaluations() {
        var entries = await api('/evaluations');
        var search = byId('evaluationSearch');
        var scored = entries.map(summaryDisplayScore100)
            .filter(function (score) { return Number.isFinite(score); });
        byId('metricEvaluationCount').textContent = entries.length;
        var profileIds = entries.map(function (entry) { return entry.internProfileId; });
        var identitiesAvailable = profileIds.every(function (id) {
            return Number.isSafeInteger(id) && id > 0;
        });
        byId('metricEvaluatedInterns').textContent = identitiesAvailable
            ? new Set(profileIds).size : '—';
        byId('metricAverageScore').textContent = scored.length
            ? (scored.reduce(function (sum, score) { return sum + score; }, 0) / scored.length).toFixed(1)
            : '—';

        function render() {
            var term = search.value.trim().toLocaleLowerCase('vi');
            var filtered = entries.filter(function (entry) {
                return !term || [entry.enrollmentId, entry.internName, entry.programName,
                    entry.mentorName, entry.comment].some(function (part) {
                    return String(part || '').toLocaleLowerCase('vi').includes(term);
                });
            });
            var body = byId('evaluationRows');
            if (!filtered.length) {
                emptyRow(body, 6, entries.length ? 'Không tìm thấy kết quả phù hợp.'
                    : 'Chưa có đánh giá cuối kỳ nào.');
                return;
            }
            body.replaceChildren();
            filtered.forEach(function (entry) {
                var row = node('tr');
                var intern = node('td');
                intern.appendChild(node('strong', entry.internName || '—'));
                intern.appendChild(node('small', 'Đợt #' + entry.enrollmentId, 'fe3-table-meta d-block text-muted'));
                row.appendChild(intern);
                cell(row, entry.programName);
                cell(row, entry.mentorName);

                var scoreTd = node('td');
                var scoreVal = Number(entry.score) || 0;
                var avg5 = (entry.skillScore && entry.attitudeScore)
                    ? ((entry.skillScore + entry.attitudeScore) / 2)
                    : (scoreVal / 20);
                var grade = gradeBadge(avg5);

                var scoreBadge = node('div', null, 'd-flex flex-column align-items-start gap-1');
                scoreBadge.appendChild(node('span', grade.text, grade.cls + ' rounded-pill px-2 py-0 small'));
                if (entry.skillScore && entry.attitudeScore) {
                    scoreBadge.appendChild(node('span', 'KN: ' + entry.skillScore + '/5 · TĐ: ' + entry.attitudeScore + '/5', 'small text-muted'));
                    scoreBadge.appendChild(node('strong', (avg5 * 2).toFixed(1) + ' / 10.0', 'text-primary small'));
                } else {
                    scoreBadge.appendChild(node('strong', scoreVal + ' / 100đ', 'fe3-score small'));
                }
                scoreTd.appendChild(scoreBadge);
                row.appendChild(scoreTd);

                cell(row, moment(entry.createdAt));

                var commentTd = node('td');
                var commentText = (entry.comment || 'Không có nhận xét.')
                    .replace(/<!--EVAL_DATA:(.*?)-->/g, '')
                    .replace(/BẢNG ĐIỂM CHI TIẾT[\s\S]*/g, '')
                    .replace(/===.*?===[\s\S]*/g, '')
                    .replace(/^NHẬN XÉT CHUNG CỦA MENTOR:\s*/i, '')
                    .trim();
                commentTd.appendChild(node('span', commentText || 'Không có nhận xét.', 'small'));
                row.appendChild(commentTd);

                body.appendChild(row);
            });
        }
        search.addEventListener('input', render);
        render();
    }

    var start = ({
        'mentor-dashboard': mentorDashboard,
        'mentor-tasks': mentorTasks,
        'mentor-reports': mentorReports,
        'mentor-evaluation': function () { return evaluationPage(true); },
        'intern-tasks': internTasks,
        'intern-task-detail': internTaskDetail,
        'intern-reports': internReports,
        'intern-evaluation': function () { return evaluationPage(false); },
        'hr-evaluations': hrEvaluations,
        'hr-dashboard': hrDashboard
    })[page];
    if (start) start().catch(errorMessage);
})();
