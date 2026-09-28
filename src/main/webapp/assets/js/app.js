/* ==========================================================================
   Internship Management System - Global script
   Owner: FE1
   Khong dung jQuery. Khong dung Bootstrap JS (tranh phu thuoc CDN khi demo).
   ========================================================================== */

(function () {
    'use strict';

    /* --------------------------------------------- Sidebar (chi mobile) --- */

    function initSidebar() {
        var sidebar = document.querySelector('.app-sidebar');
        var toggle = document.querySelector('.app-menu-toggle');
        var backdrop = document.querySelector('.app-backdrop');

        if (!sidebar || !toggle || !backdrop) {
            return;
        }

        function open() {
            sidebar.classList.add('is-open');
            backdrop.hidden = false;
            toggle.setAttribute('aria-expanded', 'true');
        }

        function close() {
            sidebar.classList.remove('is-open');
            backdrop.hidden = true;
            toggle.setAttribute('aria-expanded', 'false');
        }

        toggle.addEventListener('click', function () {
            if (sidebar.classList.contains('is-open')) {
                close();
            } else {
                open();
            }
        });

        backdrop.addEventListener('click', close);

        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape') {
                close();
            }
        });
    }

    /* ------------------------------------------------ Danh dau menu active -
       Neu trang khong tu set activePage thi suy ra tu URL hien tai.         */

    function initActiveNav() {
        var links = document.querySelectorAll('.app-nav__link[href]');

        if (!links.length) {
            return;
        }

        if (document.querySelector('.app-nav__link.is-active')) {
            return;
        }

        var path = window.location.pathname;

        Array.prototype.forEach.call(links, function (link) {
            var href;
            try {
                href = new URL(link.getAttribute('href'), window.location.origin).pathname;
            } catch (err) {
                return;
            }
            if (href !== '/' && path.indexOf(href) === 0) {
                link.classList.add('is-active');
            }
        });
    }

    /* ------------------------------------------------------ Dong alert --- */

    function initAlerts() {
        document.addEventListener('click', function (event) {
            var button = event.target.closest('[data-dismiss="alert"]');
            if (!button) {
                return;
            }
            var alert = button.closest('.alert');
            if (alert && alert.parentNode) {
                alert.parentNode.removeChild(alert);
            }
        });
    }

    /* --------------------------------------------------- Confirm action ---
       Dung cho cac thao tac khong the hoan tac: xac nhan hop dong,
       duyet/tu choi ho so, xoa tai lieu.                                   */

    function initConfirm() {
        document.addEventListener('submit', function (event) {
            var form = event.target;
            var message = form.getAttribute('data-confirm');

            if (message && !window.confirm(message)) {
                event.preventDefault();
            }
        });

        document.addEventListener('click', function (event) {
            var el = event.target.closest('[data-confirm]');
            if (el && el.tagName === 'A' && !window.confirm(el.getAttribute('data-confirm'))) {
                event.preventDefault();
            }
        });
    }

    /* ------------------------------------------------- Upload validation -
       Dong bo voi DocumentService cua BE2:
       chi nhan .pdf .doc .docx, toi da 5MB.                                */

    var ALLOWED_EXT = ['pdf', 'doc', 'docx'];
    var MAX_BYTES = 5 * 1024 * 1024;

    function formatSize(bytes) {
        if (bytes >= 1024 * 1024) {
            return (bytes / 1024 / 1024).toFixed(1) + ' MB';
        }
        return Math.max(1, Math.round(bytes / 1024)) + ' KB';
    }

    function validateFile(file) {
        var ext = (file.name.split('.').pop() || '').toLowerCase();

        if (ALLOWED_EXT.indexOf(ext) === -1) {
            return 'File "' + file.name + '" không đúng định dạng. Chỉ nhận PDF, DOC, DOCX.';
        }
        if (file.size > MAX_BYTES) {
            return 'File "' + file.name + '" là ' + formatSize(file.size)
                + ', vượt quá giới hạn 5 MB.';
        }
        return null;
    }

    function showFieldError(input, message) {
        var holder = input.closest('[data-file-field]') || input.parentNode;
        var box = holder.querySelector('.field-error');

        if (!box) {
            box = document.createElement('span');
            box.className = 'field-error';
            holder.appendChild(box);
        }

        if (message) {
            box.textContent = message;
            input.classList.add('is-invalid');
        } else {
            box.textContent = '';
            input.classList.remove('is-invalid');
        }
    }

    function initFileInputs() {
        var inputs = document.querySelectorAll('input[type="file"][data-validate-file]');

        Array.prototype.forEach.call(inputs, function (input) {
            input.addEventListener('change', function () {
                var file = input.files && input.files[0];

                if (!file) {
                    showFieldError(input, null);
                    return;
                }

                var error = validateFile(file);
                showFieldError(input, error);

                var nameBox = input.closest('form').querySelector('[data-file-name]');
                if (nameBox) {
                    nameBox.textContent = error ? '' : file.name + ' (' + formatSize(file.size) + ')';
                }
            });
        });
    }

    function initUploadForms() {
        var forms = document.querySelectorAll('form[data-validate-upload]');

        Array.prototype.forEach.call(forms, function (form) {
            form.addEventListener('submit', function (event) {
                var input = form.querySelector('input[type="file"][data-validate-file]');
                if (!input || !input.files || !input.files[0]) {
                    return;
                }

                var error = validateFile(input.files[0]);
                if (error) {
                    event.preventDefault();
                    showFieldError(input, error);
                }
            });
        });

        var zones = document.querySelectorAll('.dropzone');

        Array.prototype.forEach.call(zones, function (zone) {
            var input = zone.querySelector('input[type="file"]');
            if (!input) {
                return;
            }

            zone.addEventListener('click', function () {
                input.click();
            });

            ['dragenter', 'dragover'].forEach(function (type) {
                zone.addEventListener(type, function (event) {
                    event.preventDefault();
                    zone.classList.add('is-dragover');
                });
            });

            ['dragleave', 'drop'].forEach(function (type) {
                zone.addEventListener(type, function (event) {
                    event.preventDefault();
                    zone.classList.remove('is-dragover');
                });
            });

            zone.addEventListener('drop', function (event) {
                if (event.dataTransfer && event.dataTransfer.files.length) {
                    input.files = event.dataTransfer.files;
                    input.dispatchEvent(new Event('change'));
                }
            });
        });
    }

    /* ------------------------------------------------ Mat khau nhap lai ---
       Dung cho form dang ky: chan submit khi 2 o mat khau khac nhau.
       Server VAN PHAI kiem tra lai, day chi la lop chan som o client.      */

    function initPasswordMatch() {
        var forms = document.querySelectorAll('form[data-check-password]');

        Array.prototype.forEach.call(forms, function (form) {
            var pass = form.querySelector('input[name="password"]');
            var confirm = form.querySelector('input[name="confirmPassword"]');

            if (!pass || !confirm) {
                return;
            }

            function check() {
                if (!confirm.value) {
                    showFieldError(confirm, null);
                    return;
                }
                showFieldError(confirm, pass.value === confirm.value
                    ? null
                    : 'Mật khẩu nhập lại không khớp.');
            }

            pass.addEventListener('input', check);
            confirm.addEventListener('input', check);

            form.addEventListener('submit', function (event) {
                if (pass.value !== confirm.value) {
                    event.preventDefault();
                    showFieldError(confirm, 'Mật khẩu nhập lại không khớp.');
                    confirm.focus();
                }
            });
        });
    }

    /* ------------------------------------------------------------- Init -- */

    document.addEventListener('DOMContentLoaded', function () {
        initSidebar();
        initActiveNav();
        initAlerts();
        initConfirm();
        initFileInputs();
        initUploadForms();
        initPasswordMatch();
    });
})();
