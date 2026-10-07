<%--
    footer.jsp - Dong khung trang dung chung. Owner: FE1
    Luon include NGAY SAU noi dung trang, truoc </body>.
--%>
        </main>
        <%-- /.app-content --%>

    </div>
    <%-- /.app-main --%>

</div>
<%-- /.app-shell --%>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="${ctx}/assets/js/app.js"></script>
<c:if test="${not empty pageScript}">
    <script src="${ctx}/assets/js/${pageScript}"></script>
</c:if>
</body>
</html>
