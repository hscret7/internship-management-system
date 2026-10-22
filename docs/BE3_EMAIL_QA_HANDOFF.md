# BE3 / BE1 — phối hợp kiểm thử EmailService chung

## Trách nhiệm

- BE3 chuẩn bị đơn `PENDING`, thao tác duyệt/từ chối và đối chiếu quyết định đã lưu.
- BE1 cấu hình SMTP/EmailService, kiểm tra gửi/nhận và chủ động mô phỏng lỗi gửi trong môi trường QA.
- Trước khi gửi thật, hai bên thống nhất hộp thư nhận, quyền sử dụng hộp thư và thời điểm thử.
  Không sử dụng email của người thật trong dữ liệu cũ. Không đưa SMTP secret/token vào Git/chat/log evidence.

## Chuẩn bị dữ liệu và chạy luồng

1. Dùng tài khoản HR QA và một TTS QA có hồ sơ riêng do BE1/BE2 cung cấp hoặc đã tạo qua UI.
2. Đặt `intern_profiles.email` của riêng hồ sơ QA thành hộp thư đã được hai bên đồng ý.
   `ApplicationDAO` lấy người nhận từ email của hồ sơ, không phải email HR/reviewer.
   Không đổi email hồ sơ đang dùng của thành viên/khách hàng.
3. TTS đăng nhập, nộp đơn tại `/intern/applications`. Ghi mã application vừa tạo và xác nhận `PENDING`.
4. HR đăng nhập, mở `/hr/applications?action=detail&id=<applicationId>`, duyệt hoặc từ chối qua form.
   Form dùng CSRF cùng session; từ chối nhập lý do riêng dễ đối chiếu.
5. Sau mỗi case: refresh cả HR và TTS, đối chiếu status/reviewer/reviewed_at/reject_reason trong DB.
   Chụp evidence đã che email/config nhạy cảm; BE1 đối chiếu hộp thư nhận và log SMTP.
6. Tạo đơn QA mới cho case tiếp theo sau khi đơn trước được quyết định; không cần tạo enrollment.
   Không reset đơn đã xử lý về PENDING hoặc sửa lịch sử để thử lại.

Không ghi sẵn mã đơn hay email nhận vì chúng chỉ có sau khi thống nhất fixture/hộp thư với BE1.
Các bước trên không tự gửi email cho tới khi người kiểm thử thao tác và SMTP QA được cấu hình.

## Ma trận bốn case

| Case | Thao tác và EmailService | Kết quả phải có |
| --- | --- | --- |
| EMAIL-01 | Duyệt PENDING, SMTP hoạt động | APPROVED; đúng người nhận/tên/mã đơn, tiêu đề duyệt; 1 email |
| EMAIL-02 | Từ chối PENDING, lý do `QA_EMAIL_REJECT`, SMTP hoạt động | REJECTED; đúng người nhận/mã đơn/lý do; 1 email |
| EMAIL-03 | Duyệt PENDING, BE1 làm SMTP QA lỗi | APPROVED vẫn giữ sau refresh/kết nối mới; reviewer/time vẫn có; log lỗi gửi |
| EMAIL-04 | Từ chối PENDING, lý do `QA_EMAIL_FAILURE`, BE1 làm SMTP QA lỗi | REJECTED và lý do/reviewer/time vẫn giữ; log lỗi gửi |

Kiểm tra thêm: gửi lại cùng quyết định hoặc đổi approve/reject trên đơn đã xử lý phải bị chặn,
không có email thứ hai. Lỗi gửi hiện không tự retry; nếu BE1 thêm retry sau này cần contract
idempotency riêng, không gọi lại quyết định trên đơn đã xử lý để resend.

## Evidence tự động hiện có

`ApplicationServiceTest` dùng stub EmailService, không mở SMTP:
- đúng người nhận, tên/mã đơn và nội dung duyệt;
- nội dung từ chối có lý do;
- email chỉ được gọi sau commit;
- lỗi email duyệt/từ chối không rollback quyết định;
- quyết định lặp không gửi email trùng.

Chạy: `mvn -Dtest=ApplicationServiceTest test`.
Thông báo log “Đã gửi email” trong bộ test là từ stub, không phải bằng chứng đã gửi thư thật.
Trạng thái phối hợp SMTP thật: CHỜ BE1 xác nhận cấu hình và hộp thư thử.
