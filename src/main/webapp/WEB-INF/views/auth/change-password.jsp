<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Đổi mật khẩu" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<section class="card p-4" style="max-width: 540px">
    <h1 class="h4">Đổi mật khẩu</h1>
    <p>Mật khẩu mới có ít nhất 8 ký tự, tối đa 72 byte UTF-8. Sau khi đổi thành công, bạn cần đăng nhập lại.</p>
    <form action="${ctx}/auth/change-password" method="post" data-check-password>
        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
        <div class="mb-3"><label class="form-label" for="currentPassword">Mật khẩu hiện tại</label>
            <input class="form-control" type="password" id="currentPassword" name="currentPassword" autocomplete="current-password" required></div>
        <div class="mb-3"><label class="form-label" for="newPassword">Mật khẩu mới</label>
            <input class="form-control" type="password" id="newPassword" name="newPassword" autocomplete="new-password" minlength="8" required></div>
        <div class="mb-3"><label class="form-label" for="confirmPassword">Nhập lại mật khẩu mới</label>
            <input class="form-control" type="password" id="confirmPassword" name="confirmPassword" autocomplete="new-password" minlength="8" required></div>
        <button type="submit" class="btn btn-brand">Lưu mật khẩu mới</button>
    </form>
</section>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
