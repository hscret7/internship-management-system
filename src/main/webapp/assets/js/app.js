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
        var wrapper = input.closest('.input-group') || input.closest('.position-relative') || input.closest('[data-file-field]');
        var holder = wrapper ? wrapper.parentNode : input.parentNode;
        var box = holder.querySelector('.field-error');

        if (!box) {
            box = document.createElement('span');
            box.className = 'field-error text-danger small d-block mt-1';
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
                showFieldError(pass, new TextEncoder().encode(pass.value).length > 72 ? 'Mật khẩu tối đa 72 byte UTF-8.' : null);
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
                if (new TextEncoder().encode(pass.value).length > 72) {
                    event.preventDefault(); showFieldError(pass, 'Mật khẩu tối đa 72 byte UTF-8.'); pass.focus(); return;
                }
                if (pass.value !== confirm.value) {
                    event.preventDefault();
                    showFieldError(confirm, 'Mật khẩu nhập lại không khớp.');
                    confirm.focus();
                }
            });
        });
    }

    /* ------------------------------------------------ Hiện/Ẩn mật khẩu --- */

    var passwordTogglesInitialized=false;
    function initPasswordToggle() {
        if(passwordTogglesInitialized)return;passwordTogglesInitialized=true;
        document.addEventListener('click',function(event){
            var button=event.target.closest('.btn-toggle-password');
            if(button && !button.getAttribute('onclick')) window.togglePasswordVisibility(button,event);
        });
        function conceal(){
            document.querySelectorAll('.btn-toggle-password').forEach(function(button){
                var wrapper=button.closest('.position-relative')||button.closest('.input-group')||button.parentElement;
                var input=wrapper&&wrapper.querySelector('input');
                if(!input)return;
                if(input.type==='text')window.togglePasswordVisibility(button);
                button.setAttribute('aria-pressed','false');
                if(input.id)button.setAttribute('aria-controls',input.id);
            });
        }
        document.addEventListener('submit',conceal,true);
        window.addEventListener('pageshow',conceal);
        conceal();
    }

    /* ------------------------------------------------ Hỗ trợ Modal chuẩn Bootstrap (offline & online) --- */

    function initModals() {
        function ensureModalsInBody() {
            var modals = document.querySelectorAll('.modal');
            for (var i = 0; i < modals.length; i++) {
                if (modals[i].parentElement !== document.body) {
                    document.body.appendChild(modals[i]);
                }
            }
        }
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', ensureModalsInBody);
        } else {
            ensureModalsInBody();
        }

        var hasBootstrap = (typeof window.bootstrap !== 'undefined' && typeof window.bootstrap.Modal !== 'undefined');

        if (hasBootstrap) {
            window.closeAppModal = function(modal) {
                if (!modal) return;
                var inst = bootstrap.Modal.getInstance(modal);
                if (inst) {
                    inst.hide();
                } else {
                    var newInst = new bootstrap.Modal(modal);
                    newInst.hide();
                }
                setTimeout(function() {
                    document.querySelectorAll('.modal-backdrop').forEach(function(b) { b.remove(); });
                    document.body.classList.remove('modal-open');
                }, 350);
            };
            window.openAppModal = function(modal) {
                if (!modal) return;
                if (modal.parentElement !== document.body) document.body.appendChild(modal);
                var inst = bootstrap.Modal.getInstance(modal) || new bootstrap.Modal(modal);
                inst.show();
            };
            return;
        }

        document.addEventListener('click', function(e) {
            var trigger = e.target.closest('[data-bs-toggle="modal"]');
            if (trigger) {
                var targetSelector = trigger.getAttribute('data-bs-target') || trigger.getAttribute('href');
                var modal = targetSelector ? document.querySelector(targetSelector) : null;
                if (modal) {
                    e.preventDefault();
                    openModal(modal);
                }
                return;
            }

            var dismiss = e.target.closest('[data-bs-dismiss="modal"]');
            if (dismiss) {
                var modal = dismiss.closest('.modal');
                if (modal) {
                    e.preventDefault();
                    closeModal(modal);
                }
                return;
            }

            if (e.target.classList.contains('modal') && e.target.classList.contains('show')) {
                closeModal(e.target);
            }
        });

        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                var openModalEl = document.querySelector('.modal.show');
                if (openModalEl) closeModal(openModalEl);
            }
        });

        function openModal(modal) {
            if (modal.parentElement !== document.body) {
                document.body.appendChild(modal);
            }
            modal.style.display = 'block';
            modal.classList.add('show');
            modal.removeAttribute('aria-hidden');
            modal.setAttribute('aria-modal', 'true');
            document.body.classList.add('modal-open');

            var backdrop = document.querySelector('.modal-backdrop');
            if (!backdrop) {
                backdrop = document.createElement('div');
                backdrop.className = 'modal-backdrop fade show';
                document.body.appendChild(backdrop);
            }
        }

        function closeModal(modal) {
            modal.classList.remove('show');
            modal.style.display = 'none';
            modal.setAttribute('aria-hidden', 'true');
            modal.removeAttribute('aria-modal');
            document.body.classList.remove('modal-open');

            var backdrops = document.querySelectorAll('.modal-backdrop');
            for (var i = 0; i < backdrops.length; i++) {
                backdrops[i].remove();
            }
        }

        window.closeAppModal = closeModal;
        window.openAppModal = openModal;
    }

    window.togglePasswordVisibility = function (btn, event) {
        if (event) {
            try {
                event.preventDefault();
                event.stopPropagation();
            } catch (e) {}
        }

        var wrapper = btn.closest('.position-relative') || btn.closest('.input-group') || btn.parentElement;
        var input = wrapper ? wrapper.querySelector('input') : null;
        if (!input) return;
        if(input.id)btn.setAttribute('aria-controls',input.id);
        btn.setAttribute('aria-pressed',input.type==='password'?'true':'false');

        var eyeSvg = '<svg class="bi-eye" xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 16 16"><path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8M1.173 8a13 13 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5s3.879 1.168 5.168 2.457A13 13 0 0 1 14.828 8q-.086.13-.195.288c-.335.48-.83 1.12-1.373 1.67C11.88 11.332 10.119 12.5 8 12.5s-3.879-1.168-5.168-2.457A13 13 0 0 1 1.172 8z"/><path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5M4.5 8a3.5 3.5 0 1 1 7 0 3.5 3.5 0 0 1-7 0"/></svg>';
        var slashSvg = '<svg class="bi-eye-slash" xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 16 16"><path d="M13.359 11.238C15.06 9.72 16 8 16 8s-3-5.5-8-5.5a7 7 0 0 0-2.79.588l.77.771A6 6 0 0 1 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13 13 0 0 1 14.828 8q-.086.13-.195.288c-.335.48-.83 1.12-1.373 1.67l.77.771zM11.297 9.176l3.854 3.854a.5.5 0 0 0 .708-.708l-13-13a.5.5 0 0 0-.708.708l2.68 2.68A7 7 0 0 0 0 8s3 5.5 8 5.5a7 7 0 0 0 4.297-1.472M8 12.5c-2.12 0-3.879-1.168-5.168-2.457A13 13 0 0 1 1.172 8q.086-.13.195-.288c.335-.48.83-1.12 1.373-1.67l1.096 1.096A3.5 3.5 0 0 0 8 11.5a3.5 3.5 0 0 0 2.47-.976l.827.827zm-1.854-4.854 2.146 2.146A2.5 2.5 0 0 1 6.146 7.646"/></svg>';

        var eyeEl = btn.querySelector('.bi-eye');
        var slashEl = btn.querySelector('.bi-eye-slash');
        var iconHolder = btn.querySelector('.pwd-toggle-icon');
        var iconI = btn.querySelector('i');

        if (input.type === 'password') {
            input.type = 'text';
            btn.setAttribute('title', 'Ẩn mật khẩu');
            btn.setAttribute('aria-label', 'Ẩn mật khẩu');
            btn.classList.add('text-primary');
            btn.classList.remove('text-muted', 'text-secondary');
            if (iconI) {
                iconI.classList.remove('bi-eye');
                iconI.classList.add('bi-eye-slash');
            } else if (eyeEl && slashEl) {
                eyeEl.classList.add('d-none');
                slashEl.classList.remove('d-none');
            } else if (iconHolder) {
                iconHolder.innerHTML = slashSvg;
            } else {
                btn.innerHTML = slashSvg;
            }
        } else {
            input.type = 'password';
            btn.setAttribute('title', 'Hiện mật khẩu');
            btn.setAttribute('aria-label', 'Hiện mật khẩu');
            btn.classList.remove('text-primary');
            btn.classList.add('text-secondary');
            if (iconI) {
                iconI.classList.remove('bi-eye-slash');
                iconI.classList.add('bi-eye');
            } else if (eyeEl && slashEl) {
                eyeEl.classList.remove('d-none');
                slashEl.classList.add('d-none');
            } else if (iconHolder) {
                iconHolder.innerHTML = eyeSvg;
            } else {
                btn.innerHTML = eyeSvg;
            }
        }
    };

    /* ------------------------------------------------------------- Init -- */

    document.addEventListener('DOMContentLoaded', function () {
        initSidebar();
        initActiveNav();
        initAlerts();
        initConfirm();
        initFileInputs();
        initUploadForms();
        initPasswordMatch();
        initPasswordToggle();
        initModals();
    });
})();
