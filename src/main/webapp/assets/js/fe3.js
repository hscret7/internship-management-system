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

    function assessmentCard(result, index) {
        var panel = node('article', null, 'fe3-feedback border rounded p-3 mb-3');
        var heading = node('div', null, 'd-flex flex-wrap justify-content-between gap-2 mb-2');
        heading.appendChild(node('strong', 'Mentor: ' + (result.mentorName || '—')));
        heading.appendChild(node('span', (index === 0 ? 'Mới nhất · ' : '') + moment(result.createdAt), 'text-muted'));
        panel.appendChild(heading);
        var scores = node('div', null, 'row g-2 mb-3');
        [['Kỹ năng', result.skillsScore], ['Thái độ', result.attitudeScore]].forEach(function (criterion) {
            var box = node('div', null, 'col-sm-6');
            var detail = node('div', null, 'border rounded p-2');
            detail.appendChild(node('span', criterion[0], 'd-block text-muted'));
            detail.appendChild(node('strong', criterion[1] + ' / 5', 'fs-5'));
            box.appendChild(detail); scores.appendChild(box);
        });
        panel.appendChild(scores);
        panel.appendChild(node('p', result.comment || 'Không có nhận xét.', 'fe3-report-content mb-0'));
        return panel;
    }

    function showEvaluation(result, target) {
        target.replaceChildren();
        if (!result) { target.appendChild(node('p', 'Chưa có đánh giá thang 100.')); return; }
        target.appendChild(node('h2', 'Kết quả: ' + result.score + '/100', 'h5'));
        target.appendChild(node('p', 'Ngày đánh giá: ' + moment(result.createdAt), 'text-muted'));
        target.appendChild(node('p', result.comment || 'Không có nhận xét.', 'fe3-report-content'));
    }

    async function evaluationPage(mentor) {
        async function reload() {
            var entry = currentEnrollment();
            var form = mentor ? byId('evaluationForm') : null;
            if (form) form.classList.add('d-none');
            var target = byId('evaluationResult');
            if (!entry) { target.textContent = 'Chưa có đợt thực tập.'; return; }
            target.replaceChildren(node('p', 'Đang tải đánh giá…'));
            var results = await Promise.all([api('/assessments', { enrollmentId: entry.enrollmentId }), api('/evaluation', { enrollmentId: entry.enrollmentId })]);
            if (!currentEnrollment() || currentEnrollment().enrollmentId !== entry.enrollmentId) return;
            target.replaceChildren(node('h2', 'Lịch sử đánh giá hai tiêu chí', 'h5'));
            target.appendChild(node('p', 'Kỹ năng và thái độ được lưu riêng trên thang 1–5; không tự quy đổi điểm, xếp loại hoặc xác nhận hoàn thành.', 'text-muted'));
            target.appendChild(node('p', results[0].length + ' bản ghi đánh giá', 'fw-semibold'));
            if (!results[0].length) target.appendChild(node('p', 'Chưa có đánh giá hai tiêu chí.'));
            results[0].forEach(function (result, index) {
                target.appendChild(assessmentCard(result, index));
            });
            if (results[1]) { var legacy=node('section', null, 'fe3-feedback'); legacy.appendChild(node('h3', 'Đánh giá cũ (thang 100, không quy đổi)', 'h6'));var body=node('div');showEvaluation(results[1], body);legacy.appendChild(body);target.appendChild(legacy); }
            if (form && entry.status === 'ACTIVE') form.classList.remove('d-none');
        }
        await loadEnrollments(reload);
        if (mentor) {
            byId('evaluationForm').addEventListener('submit', function (event) {
                event.preventDefault();
                submitting(event.currentTarget, async function () {
                    var entry = requireEnrollment();
                    if (entry.status !== 'ACTIVE') throw new Error('Chỉ đánh giá đợt đang tham gia.');
                    var values = formValues(byId('evaluationForm'));
                    if (!values.comment.trim()) throw new Error('Cần nhập nhận xét.');
                    await api('/assessments', null, {
                        enrollmentId: entry.enrollmentId, skillsScore: values.skillsScore, attitudeScore: values.attitudeScore, comment: values.comment
                    });
                    notice('Đã lưu đánh giá cuối kỳ.', 'success');
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

    async function hrEvaluations() {
        var entries = await api('/evaluations');
        var search = byId('evaluationSearch');
        var scored = entries.map(function (entry) { return Number(entry.score); })
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
                intern.appendChild(node('small', 'Đợt #' + entry.enrollmentId, 'fe3-table-meta'));
                row.appendChild(intern);
                cell(row, entry.programName);
                cell(row, entry.mentorName);
                var score = node('td');
                score.appendChild(node('span', entry.score + '/100', 'fe3-score'));
                row.appendChild(score);
                cell(row, moment(entry.createdAt));
                cell(row, entry.comment || 'Không có nhận xét.');
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
