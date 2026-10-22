/* Progressive enhancement only. BE3 owns permissions, capacity, CSRF and writes. */
(function () {
    'use strict';
    const root = document.querySelector('[data-mentor-page]');
    if (!root) return;
    const normalize = value => String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '')
        .replace(/đ/gi, 'd').toLocaleLowerCase('vi').trim();
    const rows = Array.from(root.querySelectorAll('[data-filter-item]'));
    const search = root.querySelector('[data-search]');
    const capacity = root.querySelector('[data-capacity-filter]');
    const assignment = root.querySelector('[data-assignment-filter]');
    const count = root.querySelector('[data-visible-count]');
    const empty = root.querySelector('[data-no-results]');
    const feedback = root.querySelector('[data-filter-feedback]');
    const filterNote = root.querySelector('[data-mentor-filter-note]');
    const queryId = new URLSearchParams(window.location.search).get('mentorId');
    let mentorId = root.dataset.mentorPage === 'assignments' && /^[1-9]\d{0,9}$/.test(queryId || '')
        && Number(queryId) <= 2147483647 ? queryId : null;
    function clearMentorFilter() {
        mentorId = null;
        const url = new URL(window.location.href);
        url.searchParams.delete('mentorId');
        window.history.replaceState(null, '', url);
    }
    function filter() {
        if (!rows.length) return;
        const keyword = normalize(search && search.value);
        let visible = 0;
        rows.forEach(row => {
            const matches = (!keyword || normalize(row.dataset.searchText).includes(keyword))
                && (!capacity || capacity.value === 'all' || row.dataset.capacity === capacity.value)
                && (!assignment || assignment.value === 'all' || row.dataset.assignmentState === assignment.value)
                && (!mentorId || row.dataset.mentorId === mentorId);
            row.hidden = !matches;
            if (matches) visible++;
        });
        if (count) count.textContent = visible + (root.dataset.mentorPage === 'directory' ? ' mentor' : ' ghi danh');
        if (empty) empty.hidden = visible !== 0;
        if (filterNote) filterNote.hidden = !mentorId;
        if (feedback) feedback.textContent = 'Đang hiển thị ' + visible + '/' + rows.length
            + (root.dataset.mentorPage === 'directory' ? ' mentor trong phòng ban đang chọn.' : ' ghi danh.');
    }
    if (search) search.addEventListener('input', filter);
    if (capacity) capacity.addEventListener('change', filter);
    if (assignment) assignment.addEventListener('change', filter);
    root.querySelectorAll('[data-clear-filters]').forEach(button => button.addEventListener('click', () => {
        if (search) search.value = '';
        if (capacity) capacity.value = 'all';
        if (assignment) assignment.value = 'all';
        clearMentorFilter();
        filter();
        if (search) search.focus();
    }));
    root.querySelectorAll('[data-clear-mentor]').forEach(button => button.addEventListener('click', () => {
        clearMentorFilter();
        filter();
    }));
    root.querySelectorAll('[data-open-create]').forEach(link => link.addEventListener('click', () => {
        const details = root.querySelector('#mentor-create');
        if (!details) return;
        details.open = true;
        window.requestAnimationFrame(() => {
            const field = details.querySelector('select,input');
            if (field) field.focus({preventScroll:true});
        });
    }));
    const choice = root.querySelector('[data-mentor-choice]');
    const preview = root.querySelector('[data-choice-preview]');
    function updateChoice() {
        if (!choice || !preview) return;
        const option = choice.selectedOptions[0];
        preview.hidden = !option || !option.value;
        if (preview.hidden) return;
        preview.querySelector('[data-choice-initial]').textContent = (option.dataset.name || '').slice(0, 1).toLocaleUpperCase('vi');
        preview.querySelector('[data-choice-name]').textContent = option.dataset.name || '';
        preview.querySelector('[data-choice-specialization]').textContent = [option.dataset.department,
            option.dataset.specialization || 'Chưa cập nhật chuyên môn'].filter(Boolean).join(' · ');
        const available = Math.max(0, Number(option.dataset.max) - Number(option.dataset.current));
        preview.querySelector('[data-choice-capacity]').textContent = 'Còn ' + available + ' chỗ · đang hướng dẫn '
            + option.dataset.current + '/' + option.dataset.max + ' TTS';
    }
    if (choice) choice.addEventListener('change', updateChoice);
    const reason = root.querySelector('[data-reason]');
    function updateReason() {
        if (!reason) return;
        reason.setCustomValidity(reason.value && !reason.value.trim() ? 'Vui lòng nhập lý do có nội dung.' : '');
        const counter = root.querySelector('[data-reason-count]');
        if (counter) counter.textContent = reason.value.length + '/200';
    }
    if (reason) reason.addEventListener('input', updateReason);
    root.querySelectorAll('[data-mentor-form]').forEach(form => {
        form.addEventListener('submit', () => {
            form.querySelectorAll('button[type="submit"]').forEach(button => {
                button.disabled = true;
                button.textContent = 'Đang lưu…';
            });
        });
    });
    window.addEventListener('pageshow', () => {
        root.querySelectorAll('button[data-submit-label]').forEach(button => {
            button.disabled = false;
            button.textContent = button.dataset.submitLabel;
        });
        filter(); updateChoice(); updateReason();
    });
    filter(); updateChoice(); updateReason();
}());
