/*
 * intern-meetings.js - Man hinh "Lich hop cua toi" (US35 / task 35.5). Owner: FE1.
 *
 * Goi API theo hop dong BE1 trong docs/BE1_MEETING_CONTRACT.md:
 *     GET ${ctx}/meetings?programId&status&page&size
 *     -> {items:[{id,title,description,location,startsAt,endsAt,status,revision,...}],
 *         page,size,csrfToken}
 *
 * Vi sao viet tay thay vi dung framework: ca du an khong dung thu vien JS ngoai
 * (khong CDN), app.js cung viet tay. Giu dung mot kieu.
 *
 * QUY TAC AN TOAN: moi du lieu lay tu API deu do bang textContent, KHONG dung
 * innerHTML. Du lieu cuoc hop do nguoi dung nhap (title/location/description)
 * nen innerHTML se la loi XSS.
 */
(function () {
    'use strict';

    var app = document.getElementById('meetingsApp');
    if (!app) {
        return;
    }

    var ctx = app.getAttribute('data-context') || '';
    var rowsBody = document.getElementById('meetingRows');
    var notice = document.getElementById('meetingsNotice');
    var statusFilter = document.getElementById('meetingStatusFilter');
    var sizeSelect = document.getElementById('meetingSize');
    var reloadBtn = document.getElementById('meetingReload');
    var prevBtn = document.getElementById('meetingPrev');
    var nextBtn = document.getElementById('meetingNext');
    var pageLabel = document.getElementById('meetingPageLabel');

    var page = 1;
    var lastLoadedCount = 0;

    /* Mui gio hien thi da chot trong hop dong: luu UTC, hien Asia/Ho_Chi_Minh. */
    var TIME_ZONE = 'Asia/Ho_Chi_Minh';

    /*
     * Tach rieng ngay va gio roi ghep theo dung thu tu dd/MM/yyyy HH:mm.
     * KHONG dung mot Intl.DateTimeFormat gop ca ngay lan gio: voi locale vi-VN,
     * Intl xep gio TRUOC ngay ("16:00 10/10/2026"), nen moi phep cat chuoi theo
     * vi tri se sai (da bi bat loi khi chay thu: khoang gio bi in ra
     * "16:00 10/10/2026 – 17:30 10/10/2026" thay vi "10/10/2026 16:00 – 17:30").
     * Ghep tay thi thu tu hien thi khong con phu thuoc vao ICU cua trinh duyet.
     */
    var DATE_FORMATTER = null;
    var TIME_FORMATTER = null;

    function buildFormatters() {
        if (DATE_FORMATTER) {
            return;
        }
        DATE_FORMATTER = new Intl.DateTimeFormat('vi-VN', {
            timeZone: TIME_ZONE, day: '2-digit', month: '2-digit', year: 'numeric'
        });
        TIME_FORMATTER = new Intl.DateTimeFormat('vi-VN', {
            timeZone: TIME_ZONE, hour: '2-digit', minute: '2-digit', hour12: false
        });
    }

    /* "10/10/2026" hoac '' neu khong doc duoc. */
    function formatDatePart(value) {
        var date = parseInstant(value);
        if (!date) {
            return '';
        }
        buildFormatters();
        try {
            return DATE_FORMATTER.format(date);
        } catch (e) {
            return date.toISOString().slice(0, 10).split('-').reverse().join('/');
        }
    }

    /* "16:00" hoac '' neu khong doc duoc. */
    function formatTimePart(value) {
        var date = parseInstant(value);
        if (!date) {
            return '';
        }
        buildFormatters();
        try {
            return TIME_FORMATTER.format(date);
        } catch (e) {
            return date.toISOString().slice(11, 16);
        }
    }

    function formatInstant(value) {
        var day = formatDatePart(value);
        var time = formatTimePart(value);
        if (!day) {
            return '—';
        }
        return time ? day + ' ' + time : day;
    }

    /* "10/10/2026 16:00 – 17:30" khi cung ngay, nguoc lai in du ngay ket thuc. */
    function formatRange(startsAt, endsAt) {
        var startText = formatInstant(startsAt);
        if (startText === '—') {
            return '—';
        }
        var endDay = formatDatePart(endsAt);
        if (!endDay) {
            return startText;
        }
        var sameDay = endDay === formatDatePart(startsAt);
        var endText = sameDay
            ? formatTimePart(endsAt)
            : formatInstant(endsAt);
        return endText ? startText + ' – ' + endText : startText;
    }

    var STATUS_LABEL = {
        SCHEDULED: 'Sắp diễn ra',
        CANCELLED: 'Đã huỷ'
    };

    var STATUS_CLASS = {
        SCHEDULED: 'badge-status--pending',
        CANCELLED: 'badge-status--neutral'
    };

    function showNotice(kind, message) {
        if (!notice) {
            return;
        }
        notice.className = 'alert alert-' + kind;
        notice.textContent = message;
    }

    function clearNotice() {
        if (!notice) {
            return;
        }
        notice.className = 'alert d-none';
        notice.textContent = '';
    }

    function setRowsMessage(message) {
        rowsBody.textContent = '';
        var tr = document.createElement('tr');
        var td = document.createElement('td');
        td.colSpan = 5;
        td.className = 'text-center py-4 text-muted';
        td.textContent = message;
        tr.appendChild(td);
        rowsBody.appendChild(tr);
    }

    /*
     * BE1 tra startsAt/endsAt la Instant UTC. JSON cua Instant thuong co 'Z'
     * (vd "2026-10-10T09:00:00Z"); neu ben BE serialize kieu khac (LocalDateTime
     * khong kem mui gio) thi new Date() se hieu sai thanh gio may nguoi dung.
     * => Chuoi khong co 'Z'/'+hh:mm' duoc coi la UTC va bu them 'Z'.
     * Day la diem con phai chot voi BE1 truoc khi danh Done 35.5.
     */
    function parseInstant(value) {
        if (!value) {
            return null;
        }
        var text = String(value);
        var hasZone = /(Z|[+-]\d{2}:?\d{2})$/.test(text);
        var date = new Date(hasZone ? text : text + 'Z');
        return isNaN(date.getTime()) ? null : date;
    }

    function renderRows(items) {
        rowsBody.textContent = '';

        if (!items.length) {
            setRowsMessage('Bạn chưa có buổi họp nào trong chương trình thực tập.');
            return;
        }

        items.forEach(function (item) {
            var tr = document.createElement('tr');

            var tdTime = document.createElement('td');
            tdTime.className = 'ps-3';
            tdTime.textContent = formatRange(item.startsAt, item.endsAt);
            tr.appendChild(tdTime);

            var tdTitle = document.createElement('td');
            var strong = document.createElement('div');
            strong.className = 'fw-semibold';
            strong.textContent = item.title || '(Không có tiêu đề)';
            tdTitle.appendChild(strong);
            if (item.description) {
                var hint = document.createElement('div');
                hint.className = 'text-muted small';
                hint.textContent = item.description;
                tdTitle.appendChild(hint);
            }
            tr.appendChild(tdTitle);

            var tdLocation = document.createElement('td');
            tdLocation.textContent = item.location || '—';
            tr.appendChild(tdLocation);

            /* Hop dong moi chi tra organizerUserId (khong co ten nguoi to chuc).
               Chua co API tra ten -> hien thi ma so, va day la viec con thieu
               cua BE1, khong phai loi hien thi cua FE. */
            var tdOrganizer = document.createElement('td');
            tdOrganizer.className = 'text-muted';
            tdOrganizer.textContent = item.organizerUserId ? ('#' + item.organizerUserId) : '—';
            tr.appendChild(tdOrganizer);

            var tdStatus = document.createElement('td');
            tdStatus.className = 'pe-3';
            var badge = document.createElement('span');
            badge.className = 'badge-status ' + (STATUS_CLASS[item.status] || 'badge-status--neutral');
            badge.textContent = STATUS_LABEL[item.status] || (item.status || '—');
            tdStatus.appendChild(badge);
            tr.appendChild(tdStatus);

            rowsBody.appendChild(tr);
        });
    }

    function updatePager() {
        pageLabel.textContent = 'Trang ' + page;
        prevBtn.disabled = page <= 1;
        /* Hop dong khong tra ve tong so ban ghi, nen khong biet con trang sau hay
           khong. Suy ra: con trang sau khi trang vua roi tra ve du so dong da xin. */
        nextBtn.disabled = lastLoadedCount < parseInt(sizeSelect.value, 10);
    }

    function describeError(status) {
        switch (status) {
            case 400:
                return 'Tham số lọc không hợp lệ. Vui lòng bỏ bộ lọc và thử lại.';
            case 401:
                return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
            case 403:
                return 'Tài khoản của bạn không có quyền xem lịch họp này.';
            case 404:
                return 'Chức năng lịch họp chưa được bật trên hệ thống.';
            case 503:
                return 'Hệ thống đang bận hoặc không kết nối được cơ sở dữ liệu. Vui lòng thử lại sau.';
            default:
                return 'Không tải được lịch họp (mã lỗi ' + status + '). Vui lòng thử lại sau.';
        }
    }

    function load() {
        setRowsMessage('Đang tải…');
        clearNotice();
        prevBtn.disabled = true;
        nextBtn.disabled = true;

        var url = ctx + '/meetings?page=' + page + '&size=' + encodeURIComponent(sizeSelect.value);
        if (statusFilter.value) {
            url += '&status=' + encodeURIComponent(statusFilter.value);
        }

        fetch(url, {
            method: 'GET',
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        }).then(function (response) {
            if (!response.ok) {
                setRowsMessage('Không tải được danh sách buổi họp.');
                showNotice('danger', describeError(response.status));
                return null;
            }
            /* Phien het han thi servlet tra ve trang HTML dang nhap chu khong
               phai JSON -> response.json() se nem loi. Bat truoc cho ro rang. */
            var type = response.headers.get('Content-Type') || '';
            if (type.indexOf('json') === -1) {
                setRowsMessage('Không tải được danh sách buổi họp.');
                showNotice('warning', 'Phiên đăng nhập đã hết hạn hoặc máy chủ trả về dữ liệu không hợp lệ. Vui lòng đăng nhập lại.');
                return null;
            }
            return response.json();
        }).then(function (data) {
            if (!data) {
                return;
            }
            var items = Array.isArray(data.items) ? data.items : [];
            lastLoadedCount = items.length;
            renderRows(items);
            updatePager();
        }).catch(function () {
            setRowsMessage('Không tải được danh sách buổi họp.');
            showNotice('danger', 'Không kết nối được tới máy chủ. Kiểm tra kết nối mạng rồi thử lại.');
        });
    }

    statusFilter.addEventListener('change', function () {
        page = 1;
        load();
    });

    sizeSelect.addEventListener('change', function () {
        page = 1;
        load();
    });

    reloadBtn.addEventListener('click', load);

    prevBtn.addEventListener('click', function () {
        if (page > 1) {
            page -= 1;
            load();
        }
    });

    nextBtn.addEventListener('click', function () {
        page += 1;
        load();
    });

    load();
})();
