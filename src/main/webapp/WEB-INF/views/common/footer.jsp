<%--
    footer.jsp - Dong khung trang dung chung. Owner: FE1
    Luon include NGAY SAU noi dung trang, truoc </body>.

    ============================================================================
    DISCUSSION: vi sao file nay TU KHAI BAO taglib + ctx (khong dua vao trang cha)
    ----------------------------------------------------------------------------
    header.jsp dat ctx bang <c:set> -> mac dinh la PAGE SCOPE, chi song trong
    trang da include no. Neu trang cha include footer bang <jsp:include> (include
    DONG, chay luc request) thi footer duoc bien dich RIENG va KHONG thay bien
    page-scope cua trang cha. Hau qua tung gap tren 6 trang (hr|intern x
    allowances|attendance|support):
        ${ctx}        -> rong  => src="/assets/js/app.js" (THIEU context path)
        ${pageScript} -> rong  => <script src="/assets/js/">
    => app.js va notifications-bell.js deu KHONG nap duoc: mat het JS dung chung,
       chuong thong bao bam khong phan hoi (markup van con nen nhin nhu binh thuong).

    Vi vay footer TU set lai ctx. Gia tri luon giong header dat, nen voi 21 trang
    include TINH thi khong doi gi; voi trang include DONG thi sua duoc loi tren.
    Khai bao lai taglib cung prefix + cung URI la hop le theo spec JSP.

    DA KIEM CHUNG (10/10/2026, Chrome headless qua CDP):
        - /hr/support + /intern/support: truoc khi sua, bam chuong KHONG mo panel
          (aria-expanded giu "false", body ket o "Đang tải…") vi 2 file JS khong
          nap duoc; sau khi sua, panel mo that va hien dung danh sach.
        - /hr/catalog (include TINH): truoc va sau giau nguyen ket qua.
        - 10 trang INTERN + HR: chuong bam mo duoc, 0 loi JS, 0 request loi.
    --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

        </main>
        <%-- /.app-content --%>

    </div>
    <%-- /.app-main --%>

</div>
<%-- /.app-shell --%>

<script src="${ctx}/assets/js/app.js?v=20261018-password"></script>

<%-- Chuong thong bao nam trong header.jsp nen phai nap o MOI trang co khung chung,
     khong the de trong pageScript (chi co mot so trang set). Script tu thoat ngay
     neu khong tim thay #appBell, nen an toan voi ca trang khong co chuong. --%>
<script src="${ctx}/assets/js/notifications-bell.js?v=20261013-integration"></script>

<c:if test="${not empty pageScript}">
    <script src="${ctx}/assets/js/${pageScript}?v=20261013-integration"></script>
</c:if>
</body>
</html>
