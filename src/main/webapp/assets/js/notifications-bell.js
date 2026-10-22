/*
 * notifications-bell.js - Chuong thong bao tren header dung chung (US36). Owner: FE1.
 *
 * Chay o MOI trang co khung chung (header.jsp + footer.jsp), cho ca 4 vai tro.
 * Goi API theo hop dong docs/BE1_US36_INAPP_CONTRACT.md:
 *
 *     GET  ${ctx}/notifications?page=1&size=8
 *          -> {items:[{id,kind,sourceType,sourceId,sourceRevision,
 *                      title,body,availableAt,readAt}],
 *              unreadCount, page, size, csrfToken}
 *     POST ${ctx}/notifications   (application/x-www-form-urlencoded)
 *          action=read&id=<id>&csrfToken=<token lay tu lan GET gan nhat>
 *          -> {"id":12,"read":true}
 *
 * Vi sao viet tay: ca du an khong dung thu vien JS ngoai (khong CDN), app.js cung
 * viet tay. Giu dung mot kieu.
 *
 * ============================ BA DIEM DE LAM SAI ============================
 * 1) PHIEN HET HAN KHONG TRA VE LOI HTTP. AuthenticationFilter redirect ve trang
 *    dang nhap, va trang do tra HTTP 200. Nen `if (!response.ok)` mot minh la
 *    KHONG DU: phai kiem tra Content-Type co phai JSON nua. Da lam o readJson().
 * 2) title/body la du lieu do nguoi dung nhap (tieu de cuoc hop, ten nhiem vu...).
 *    Toan bo phan render dung textContent, KHONG dung innerHTML -> khong co XSS.
 * 3) sourceType/sourceId chi la METADATA, khong phai su uy quyen. Trang thai hien
 *    tai chua co route trang cho tung nguon, nen chuong KHONG tu tao link toi
 *    nguon. Khong duoc dung sourceId de doan URL.
 * ============================================================================
 */
(function () {
    'use strict';

    var bell = document.getElementById('appBell');
    if (!bell) {
        return;
    }

    var ctx = bell.getAttribute('data-context') || '';
    var btn = document.getElementById('appBellBtn');
    var badge = document.getElementById('appBellBadge');
    var panel = document.getElementById('appBellPanel');
    var body = document.getElementById('appBellBody');
    var closeBtn = document.getElementById('appBellClose');
    var reloadBtn = document.getElementById('appBellReload');

    if (!btn || !panel || !body) {
        return;
    }

    /* So thong bao lay ve mot lan trong chuong. Danh sach day du thuoc trang
       /intern/notifications (chua co route - xem sidebar.jsp). */
    var PANEL_SIZE = 8;
    var POLL_MS = 60000;

    /* Token CSRF cua phien, chi lay duoc tu response cua GET /notifications.
       Luu lai de dung cho POST danh dau da doc. */
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
            return '';
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

    function setBadge(count) {
        var total = count > 0 ? count : 0;
        if (!badge) {
            return;
        }
        if (total === 0) {
            badge.hidden = true;
            badge.textContent = '';
            btn.setAttribute('aria-label', 'Thông báo');
            return;
        }
        /* Qua 99 thi hien "99+" - badge la vong tron nho, so dai se tran ra ngoai. */
        badge.textContent = total > 99 ? '99+' : String(total);
        badge.hidden = false;
        btn.setAttribute('aria-label', 'Thông báo, ' + total + ' chưa đọc');
    }

    function setHint(message) {
        body.textContent = '';
        var p = document.createElement('p');
        p.className = 'app-bell__hint';
        p.textContent = message;
        body.appendChild(p);
    }

    function renderItems(items) {
        body.textContent = '';

        if (!items.length) {
            setHint('Bạn chưa có thông báo nào.');
            return;
        }

        items.forEach(function (item) {
            var wrap = document.createElement('div');
            wrap.className = 'app-bell__item' + (item.readAt ? '' : ' is-unread');

            var head = document.createElement('div');
            head.className = 'app-bell__item-head';

            var kind = document.createElement('span');
            kind.className = 'app-bell__kind';
            kind.textContent = KIND_LABEL[item.kind] || (item.kind || 'Thông báo');
            head.appendChild(kind);

            var when = document.createElement('span');
            when.className = 'app-bell__when';
            when.textContent = formatInstant(item.availableAt);
            head.appendChild(when);

            wrap.appendChild(head);

            var title = document.createElement('p');
            title.className = 'app-bell__item-title';
            title.textContent = item.title || '(Không có tiêu đề)';
            wrap.appendChild(title);

            if (item.body) {
                var text = document.createElement('p');
                text.className = 'app-bell__item-body';
                text.textContent = item.body;
                wrap.appendChild(text);
            }

            if (!item.readAt) {
                var mark = document.createElement('button');
                mark.type = 'button';
                mark.className = 'app-bell__mark';
                mark.textContent = 'Đánh dấu đã đọc';
                mark.setAttribute('data-mark-read', String(item.id));
                wrap.appendChild(mark);
            }

            body.appendChild(wrap);
        });
    }

    /* Thong bao loi theo ma HTTP tra ve. 503 la truong hop rieng: no KHONG phai
       su co, ma la co inapp.notifications.enabled dang tat (mac dinh false). */
    function describeError(status) {
        switch (status) {
            case 400:
                return 'Yêu cầu thông báo không hợp lệ.';
            case 401:
                return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
            case 403:
                return 'Tài khoản của bạn không có quyền xem thông báo.';
            case 404:
                return 'Chức năng thông báo chưa được bật trên hệ thống.';
            case 503:
                return 'Chức năng thông báo chưa được bật trên hệ thống này.';
            default:
                return 'Không tải được thông báo (mã lỗi ' + status + ').';
        }
    }

    /*
     * Doc JSON an toan: tra null khi phien het han hoac may chu tra HTML.
     * Tra ve {status, data} de noi goi phan biet "loi HTTP" va "khong phai JSON".
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

    /*
     * @param renderList true khi can ve lai danh sach (mo chuong / bam Tai lai),
     *                   false khi chi can cap nhat so chua doc (vong poll dinh ky)
     *                   - luc do khong duoc dung vao danh sach nguoi dung dang doc.
     */
    function load(renderList) {
        if (loading) {
            return;
        }
        loading = true;

        if (renderList) {
            setHint('Đang tải…');
        }

        fetch(ctx + '/notifications?page=1&size=' + PANEL_SIZE, {
            method: 'GET',
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        }).then(readJson).then(function (result) {
            loading = false;

            if (result.data && typeof result.data === 'object' && !result.notJson) {
                if (typeof result.data.csrfToken === 'string') {
                    csrfToken = result.data.csrfToken;
                }
                if (!result.notJson && result.status >= 200 && result.status < 300) {
                    setBadge(Number(result.data.unreadCount) || 0);
                    if (renderList) {
                        renderItems(Array.isArray(result.data.items) ? result.data.items : []);
                    }
                    return;
                }
            }

            /* Toi day: loi HTTP, hoac 200 nhung than la HTML trang dang nhap. */
            if (result.notJson && (result.status === 200 || result.status === 302)) {
                setBadge(0);
                if (renderList) {
                    setHint('Phiên đăng nhập đã hết hạn. Vui lòng tải lại trang và đăng nhập lại.');
                }
                return;
            }

            setBadge(0);
            if (renderList) {
                setHint(describeError(result.status));
            }
        }).catch(function () {
            loading = false;
            setBadge(0);
            if (renderList) {
                setHint('Không kết nối được tới máy chủ. Kiểm tra kết nối mạng rồi thử lại.');
            }
        });
    }

    function markRead(id, button) {
        if (!csrfToken) {
            /* Chua co token (chua lan nao GET thanh cong) -> khong the POST dung
               hop dong; tai lai de lay token roi nguoi dung bam lai. */
            load(true);
            return;
        }

        button.disabled = true;

        var payload = 'action=read&id=' + encodeURIComponent(id)
            + '&csrfToken=' + encodeURIComponent(csrfToken);

        fetch(ctx + '/notifications', {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                'Accept': 'application/json'
            },
            body: payload
        }).then(readJson).then(function (result) {
            if (result.status >= 200 && result.status < 300 && result.data
                    && result.data.read === true) {
                /* Ve lai ca danh sach de so chua doc va trang thai tung dong khop
                   voi server, thay vi tu doan roi sua giao dien cho le. */
                load(true);
                return;
            }
            button.disabled = false;
            if (result.status === 403) {
                /* Token het hieu luc (phien doi / token cua phien khac). Lay token
                   moi roi de nguoi dung bam lai, khong tu gui lai. */
                setHint('Phiên làm việc đã thay đổi. Vui lòng bấm "Tải lại" rồi thử lại.');
                csrfToken = '';
                return;
            }
            setHint(describeError(result.status));
        }).catch(function () {
            button.disabled = false;
            setHint('Không kết nối được tới máy chủ. Vui lòng thử lại.');
        });
    }

    function open() {
        panel.hidden = false;
        btn.setAttribute('aria-expanded', 'true');
        load(true);
    }

    function close() {
        panel.hidden = true;
        btn.setAttribute('aria-expanded', 'false');
    }

    function toggle() {
        if (panel.hidden) {
            open();
        } else {
            close();
        }
    }

    btn.addEventListener('click', function (event) {
        event.stopPropagation();
        toggle();
    });

    if (closeBtn) {
        closeBtn.addEventListener('click', close);
    }

    if (reloadBtn) {
        reloadBtn.addEventListener('click', function () {
            /* Bam "Tai lai" cung la cach lay lai token khi token cu bi tu choi. */
            csrfToken = '';
            load(true);
        });
    }

    /* Danh dau da doc: uy quyen su kien o vung danh sach thay vi gan handler cho
       tung nut, vi danh sach duoc ve lai sau moi lan tai. */
    body.addEventListener('click', function (event) {
        var button = event.target.closest('[data-mark-read]');
        if (!button || button.disabled) {
            return;
        }
        markRead(button.getAttribute('data-mark-read'), button);
    });

    /* Bam ra ngoai hoac bam Esc thi dong chuong. */
    document.addEventListener('click', function (event) {
        if (!panel.hidden && !bell.contains(event.target)) {
            close();
        }
    });

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && !panel.hidden) {
            close();
            btn.focus();
        }
    });

    /* Lan dau chi lay so chua doc, KHONG mo danh sach. */
    load(false);

    /* Hop dong ghi ro: "FE polling/reloading is needed to refresh the bell; this is
       not a push delivery service." -> lam moi moi 60 giay. Chi cap nhat so chua doc
       khi chuong dang dong; neu chuong dang mo thi lan mo ke tiep da tai lai roi. */
    window.setInterval(function () {
        load(!panel.hidden);
    }, POLL_MS);
})();
