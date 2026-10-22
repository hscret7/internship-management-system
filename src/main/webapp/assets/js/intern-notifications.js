/*
 * intern-notifications.js - Man hinh "Thong bao cua toi" (US36). Owner: FE1.
 *
 * Goi API theo hop dong docs/BE1_US36_INAPP_CONTRACT.md:
 *     GET  ${ctx}/notifications?page&size&unreadOnly
 *          -> {items:[{id,kind,sourceType,sourceId,sourceRevision,
 *                      title,body,availableAt,readAt}],
 *              unreadCount,page,size,csrfToken}
 *     POST ${ctx}/notifications   (application/x-www-form-urlencoded)
 *          action=read&id=<id>&csrfToken=<token lay tu lan GET gan nhat>
 *
 * Vi sao viet tay: ca du an khong dung thu vien JS ngoai (khong CDN), app.js cung
 * viet tay. Giu dung mot kieu.
 *
 * ============================ BON DIEM DE LAM SAI ============================
 * 1) PHIEN HET HAN KHONG TRA VE LOI HTTP. AuthenticationFilter redirect ve trang
 *    dang nhap, trang do tra HTTP 200. Nen `if (!response.ok)` mot minh la KHONG
 *    DU: phai kiem tra Content-Type co phai JSON nua. Da lam o readJson().
 * 2) title/body la du lieu do nguoi dung nhap (tieu de cuoc hop, ten nhiem vu...).
 *    Toan bo phan render dung textContent, KHONG dung innerHTML -> khong co XSS.
 * 3) sourceType/sourceId chi la METADATA, khong phai su uy quyen. Trang thai hien
 *    tai chua co route trang cho tung nguon, nen trang KHONG tu tao link toi nguon.
 *    Khong duoc dung sourceId de doan URL.
 * 4) Hop dong KHONG co endpoint "danh dau tat ca da doc". Vi vay trang nay chi co
 *    nut danh dau TUNG thong bao. Da bao BE1; khong tu goi nhieu POST de gia lap.
 * ============================================================================
 */
(function () {
    'use strict';

    var app = document.getElementById('notificationsApp');
    if (!app) {
        return;
    }

    var ctx = app.getAttribute('data-context') || '';
    var listBox = document.getElementById('notificationList');
    var notice = document.getElementById('notificationsNotice');
    var unreadFilter = document.getElementById('notificationUnreadOnly');
    var sizeSelect = document.getElementById('notificationSize');
    var reloadBtn = document.getElementById('notificationReload');
    var prevBtn = document.getElementById('notificationPrev');
    var nextBtn = document.getElementById('notificationNext');
    var pageLabel = document.getElementById('notificationPageLabel');
    var unreadLabel = document.getElementById('notificationUnreadCount');

    var page = 1;
    var lastLoadedCount = 0;

    /* Token CSRF cua phien, chi lay duoc tu response cua GET /notifications. */
    var csrfToken = '';
    var loading = false;

    var TIME_ZONE = 'Asia/Ho_Chi_Minh';

    /* Tach rieng ngay va gio roi ghep theo dung thu tu dd/MM/yyyy HH:mm.
       KHONG dung mot Intl.DateTimeFormat gop ca ngay lan gio: voi locale vi-VN,
       Intl xep gio TRUOC ngay ("16:00 10/10/2026") nen moi phep cat chuoi theo
       vi tri deu sai. Ghep tay thi khong phu thuoc vao ICU cua trinh duyet. */
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

    function parseInstant(value) {
        if (!value) {
            return null;
        }
        var text = String(value);
        var hasZone = /(Z|[+-]\d{2}:?\d{2})$/.test(text);
        var date = new Date(hasZone ? text : text + 'Z');
        return isNaN(date.getTime()) ? null : date;
    }

    function formatInstant(value) {
        var date = parseInstant(value);
        if (!date) {
            return '—';
        }
        buildFormatters();
        try {
            return DATE_FORMATTER.format(date) + ' ' + TIME_FORMATTER.format(date);
        } catch (e) {
            return date.toISOString().slice(0, 16).replace('T', ' ');
        }
    }

    var KIND_LABEL = {
        CREATE: 'Mới',
        CHANGE: 'Thay đổi',
        CANCEL: 'Đã huỷ',
        REMINDER: 'Nhắc nhở',
        TASK: 'Nhiệm vụ',
        DEADLINE: 'Hạn chót',
        FEEDBACK: 'Phản hồi'
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

    function setListMessage(message) {
        listBox.textContent = '';
        var p = document.createElement('p');
        p.className = 'text-center text-muted py-4 mb-0';
        p.textContent = message;
        listBox.appendChild(p);
    }

    function setUnreadLabel(count) {
        if (!unreadLabel) {
            return;
        }
        var total = count > 0 ? count : 0;
        unreadLabel.textContent = total === 0
            ? 'Không có thông báo chưa đọc'
            : total + ' thông báo chưa đọc';
    }

    function renderItems(items) {
        listBox.textContent = '';

        if (!items.length) {
            setListMessage(unreadFilter && unreadFilter.value === 'true'
                ? 'Bạn đã đọc hết thông báo.'
                : 'Bạn chưa có thông báo nào.');
            return;
        }

        items.forEach(function (item) {
            var row = document.createElement('div');
            row.className = 'notify-item' + (item.readAt ? '' : ' is-unread');

            var head = document.createElement('div');
            head.className = 'notify-item__head';

            var kind = document.createElement('span');
            kind.className = 'badge-status badge-status--pending';
            kind.textContent = KIND_LABEL[item.kind] || (item.kind || 'Thông báo');
            head.appendChild(kind);

            var when = document.createElement('span');
            when.className = 'notify-item__when';
            when.textContent = formatInstant(item.availableAt);
            head.appendChild(when);

            row.appendChild(head);

            var title = document.createElement('p');
            title.className = 'notify-item__title';
            title.textContent = item.title || '(Không có tiêu đề)';
            row.appendChild(title);

            if (item.body) {
                var text = document.createElement('p');
                text.className = 'notify-item__body';
                text.textContent = item.body;
                row.appendChild(text);
            }

            var foot = document.createElement('div');
            foot.className = 'notify-item__foot';

            /* Nguon duoc hien thi o dang METADATA de doi chieu khi kiem thu.
               Khong phai link, khong suy ra URL tu sourceId. */
            var source = document.createElement('span');
            source.className = 'notify-item__source';
            source.textContent = (item.sourceType || '?')
                + ' #' + (item.sourceId === null || item.sourceId === undefined ? '?' : item.sourceId)
                + ' · rev ' + (item.sourceRevision === null || item.sourceRevision === undefined
                    ? '?' : item.sourceRevision);
            foot.appendChild(source);

            var state = document.createElement('span');
            state.className = 'notify-item__state';
            if (item.readAt) {
                state.textContent = 'Đã đọc lúc ' + formatInstant(item.readAt);
            } else {
                var mark = document.createElement('button');
                mark.type = 'button';
                mark.className = 'btn btn-outline-brand btn-sm';
                mark.textContent = 'Đánh dấu đã đọc';
                mark.setAttribute('data-mark-read', String(item.id));
                state.appendChild(mark);
            }
            foot.appendChild(state);

            row.appendChild(foot);
            listBox.appendChild(row);
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
                return 'Tài khoản của bạn không có quyền xem thông báo.';
            case 404:
                return 'Chức năng thông báo chưa được bật trên hệ thống.';
            case 415:
                return 'Máy chủ từ chối định dạng yêu cầu. Vui lòng báo lại cho nhóm phát triển.';
            case 503:
                return 'Chức năng thông báo chưa được bật trên hệ thống này '
                    + '(cờ inapp.notifications.enabled đang tắt).';
            default:
                return 'Không tải được thông báo (mã lỗi ' + status + '). Vui lòng thử lại sau.';
        }
    }

    /*
     * Doc JSON an toan: tra ve {status, data, notJson}. notJson = true khi than
     * response khong phai JSON - truong hop nay xay ra khi phien het han va
     * AuthenticationFilter tra ve trang HTML dang nhap voi HTTP 200.
     */
    function readJson(response) {
        var type = response.headers.get('Content-Type') || '';
        if (type.indexOf('json') === -1) {
            return Promise.resolve({ status: response.status, data: null, notJson: true });
        }
        return response.json().then(
            function (data) {
                return { status: response.status, data: data, notJson: false };
            },
            function () {
                return { status: response.status, data: null, notJson: true };
            }
        );
    }

    function load() {
        if (loading) {
            return;
        }
        loading = true;

        setListMessage('Đang tải…');
        clearNotice();
        prevBtn.disabled = true;
        nextBtn.disabled = true;

        var url = ctx + '/notifications?page=' + page
            + '&size=' + encodeURIComponent(sizeSelect.value);
        if (unreadFilter && unreadFilter.value) {
            url += '&unreadOnly=' + encodeURIComponent(unreadFilter.value);
        }

        fetch(url, {
            method: 'GET',
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        }).then(readJson).then(function (result) {
            loading = false;

            if (!result.notJson && result.status >= 200 && result.status < 300 && result.data) {
                if (typeof result.data.csrfToken === 'string') {
                    csrfToken = result.data.csrfToken;
                }
                var items = Array.isArray(result.data.items) ? result.data.items : [];
                lastLoadedCount = items.length;
                setUnreadLabel(Number(result.data.unreadCount) || 0);
                renderItems(items);
                updatePager();
                return;
            }

            if (result.notJson && (result.status === 200 || result.status === 302)) {
                setListMessage('Không tải được danh sách thông báo.');
                showNotice('warning', 'Phiên đăng nhập đã hết hạn hoặc máy chủ trả về dữ liệu '
                    + 'không hợp lệ. Vui lòng đăng nhập lại.');
                return;
            }

            setListMessage('Không tải được danh sách thông báo.');
            showNotice(result.status === 503 ? 'warning' : 'danger', describeError(result.status));
        }).catch(function () {
            loading = false;
            setListMessage('Không tải được danh sách thông báo.');
            showNotice('danger', 'Không kết nối được tới máy chủ. Kiểm tra kết nối mạng rồi thử lại.');
        });
    }

    function markRead(id, button) {
        if (!csrfToken) {
            /* Chua co token (chua lan nao GET thanh cong) -> tai lai de lay token,
               nguoi dung bam lai. Khong tu doan token. */
            load();
            return;
        }

        button.disabled = true;

        fetch(ctx + '/notifications', {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                'Accept': 'application/json'
            },
            body: 'action=read&id=' + encodeURIComponent(id)
                + '&csrfToken=' + encodeURIComponent(csrfToken)
        }).then(readJson).then(function (result) {
            if (result.status >= 200 && result.status < 300 && result.data
                    && result.data.read === true) {
                /* Tai lai ca danh sach de so chua doc va trang thai tung dong khop
                   voi server, thay vi tu doan roi sua giao dien cho le. */
                load();
                return;
            }
            button.disabled = false;
            if (result.status === 403) {
                /* Token het hieu luc. Lay token moi roi de nguoi dung bam lai. */
                csrfToken = '';
                showNotice('warning', 'Phiên làm việc đã thay đổi. Đang tải lại danh sách, '
                    + 'vui lòng bấm lại.');
                load();
                return;
            }
            showNotice('danger', describeError(result.status));
        }).catch(function () {
            button.disabled = false;
            showNotice('danger', 'Không kết nối được tới máy chủ. Vui lòng thử lại.');
        });
    }

    /* Uy quyen su kien o vung danh sach thay vi gan handler cho tung nut, vi danh
       sach duoc ve lai sau moi lan tai. */
    listBox.addEventListener('click', function (event) {
        var button = event.target.closest('[data-mark-read]');
        if (!button || button.disabled) {
            return;
        }
        markRead(button.getAttribute('data-mark-read'), button);
    });

    if (unreadFilter) {
        unreadFilter.addEventListener('change', function () {
            page = 1;
            load();
        });
    }

    sizeSelect.addEventListener('change', function () {
        page = 1;
        load();
    });

    reloadBtn.addEventListener('click', function () {
        csrfToken = '';
        load();
    });

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
