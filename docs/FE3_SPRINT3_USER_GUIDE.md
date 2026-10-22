# FE3 Sprint 3 – hướng dẫn sử dụng và bàn giao QA

## Chuẩn bị

Backend BE4 phải có nhánh Sprint 3 và đã chạy `sql/schema_be4_sprint3.sql`. Luồng lịch họp cần các migration meeting/notification/audit BE1. Không chạy reset schema, không đưa db.properties/mail.properties lên Git. FE3 không tự chèn dữ liệu giả để làm màn hình hết lỗi.

Thứ tự tích hợp: review BE1 cho dependency dùng chung, merge BE4 PR#69 vào develop, cập nhật database có backup, rồi cập nhật/review FE3 PR#70 và merge. FE3 không mang code BE4 vào branch của mình. Xuất chuyên cần US34 và nút ở màn hình chấm công là contract BE4/BE2/FE2, không phải báo cáo cuối kỳ FE3; migration riêng `schema_be4_attendance_export.sql` chỉ thêm bằng chứng xuất chuyên cần. Sửa SQL nguồn chuyên cần của BE2 phải được nhóm nhập trước khi QA nghiệm thu luồng đó.

Đăng nhập bằng tài khoản đúng role. Nếu trang báo lỗi dữ liệu, kiểm tra server/migration; đó không phải trạng thái “không có dữ liệu”. Nếu hết phiên đăng nhập, đăng nhập lại. Các biểu mẫu và API đều lấy người thao tác từ session, không có ô chọn tác giả.

## Mentor

1. Vào `/mentor/dashboard`, xem TTS được phân, công việc và báo cáo. Liên kết “Quản lý lịch họp” dẫn tới `/mentor/meetings`.
2. `/mentor/tasks`: chọn ghi danh đang ACTIVE; điền tiêu đề, mô tả và hạn trong thời gian chương trình; “Tạo và giao” lưu hai phần cùng lúc. Không cần tự thêm database. Task cũ có thể giao bằng mã nếu vẫn đủ thông tin/hạn hợp lệ.
3. Chọn “Lịch sử” để đọc từng cập nhật tiến độ. Backend giữ người cập nhật và thời gian. Không chỉ thay một con số hiện tại.
4. `/mentor/reports`: chỉ có báo cáo đã nộp/đã phản hồi. Chọn báo cáo, đọc nội dung và lịch sử phản hồi; nhận xét là bắt buộc, điểm1..5 là tùy chọn. Có thể phản hồi bổ sung; bản cũ không bị ghi đè.
5. `/mentor/evaluation`: chọn đợt ACTIVE, nhập kỹ năng và thái độ là số nguyên1..5, nhập nhận xét; lưu tạo một lần đánh giá mới. Lịch sử hiển thị tác giả/thời gian; bản mới nhất được đánh dấu. Điểm cũ /100 được tách riêng, không quy đổi.
6. `/mentor/meetings`: chọn chương trình, TTS hiện đang phụ trách, tiêu đề, địa điểm, ngày/giờ tương lai theo Việt Nam UTC+7. Kết thúc phải sau bắt đầu. Có thể sửa/hủy cuộc họp do mình tổ chức nếu vẫn đủ quyền; xung đột phiên bản cần tải lại. Hủy giữ lịch sử. Email/thông báo do BE1 phụ trách, không phải gửi trực tiếp từ FE3.

Khi bàn giao mentor, người cũ không được ghi thêm. Khi ghi danh kết thúc/hủy, mentor cuối cùng xem lịch sử nhưng không tạo task/feedback/đánh giá mới.

## Thực tập sinh

1. `/intern/tasks`: chọn đợt của mình, xem nhiệm vụ; vào chi tiết để cập nhật phần trăm0..100 và ghi chú. Tiến độ không giảm; task DONE không cập nhật thêm. Không được xem task TTS khác.
2. `/intern/reports`: tuần thứ1 bắt đầu từ ngày chương trình, không phải tuần ISO. Tạo nháp với ba mục: kết quả, khó khăn, kế hoạch tuần tới. Có thể để trống khi lưu nháp; trước nộp phải có đủ nội dung. Ghi “Không có” nếu không gặp khó khăn.
3. Chỉnh sửa rồi lưu trước khi nộp. Xác nhận nộp xong thì không sửa được. Mỗi đợt một báo cáo/tuần; chọn báo cáo đã có thay vì tạo trùng.
4. Đọc phản hồi với tác giả/thời gian. `/intern/evaluation` hiển thị lịch sử hai tiêu chí và tách riêng điểm cũ.
5. Ghi danh đã kết thúc vẫn xem lịch sử nhưng các thao tác ghi bị khóa. Lịch họp của TTS dùng trang BE1/FE1 `/intern/meetings`, không có form tạo cuộc họp.

## HR / Admin

1. `/hr/evaluations` giữ các kết quả thang100 trước đây; chọn “Báo cáo cuối kỳ và xuất file” hoặc mở `/hr/end-term-report`.
2. Lọc theo mã chương trình, mã ghi danh, trạng thái. Mã được hiển thị trong dòng kết quả; để trống là tất cả. Bấm “Áp dụng bộ lọc”, kiểm tra chú thích bộ lọc và thời điểm dữ liệu.
3. Mỗi dòng là một đợt thực tập. Task hoàn thành/tổng task; báo cáo đã nộp bao gồm đã phản hồi. Đây không phải điểm trung bình hay tự kết luận đạt/trượt.
4. “Xem lịch sử” mở toàn bộ đánh giá hai tiêu chí với người/time/comment. TTS trùng tên được phân biệt bằng mã hồ sơ/ghi danh, không gộp theo tên.
5. Xuất Excel/PDF lấy toàn bộ dữ liệu cùng bộ lọc đã áp dụng, không chỉ trang đang xem. Nội dung lấy ở thời điểm xuất mới; không cam kết giống thời điểm xem nếu dữ liệu vừa đổi. Hơn1000 ghi danh phải lọc hẹp hơn; không xuất một phần mà không báo.
6. PDF cần font tiếng Việt trên server. Excel ghi nhận xét đầy đủ ở trang “Lịch sử đánh giá”; nhận xét dài có dòng tiếp nối cùng mã. Không thay .csv bằng đuôi .xlsx.

FE3 không sửa menu chung/đăng nhập/đăng ký của FE1, chương trình/ghi danh/lịch/nghỉ phép của FE1/FE2 hay cấu hình bảo mật BE1. Chức năng admin khác vẫn do FE1/BE1 hướng dẫn.

## QA cần chạy trên bản tích hợp

- Mentor A/B và TTS X/Y; một ghi danh ACTIVE, một COMPLETED/CANCELLED; dữ liệu cũ score100 và dữ liệu mới hai tiêu chí.
- Mentor không thấy nháp; X không xem/sửa Y; mentor cũ mất quyền sau bàn giao, tác giả cũ vẫn đúng.
- Task hạn sai/thiếu mô tả bị từ chối; tạo/giao lỗi không còn task mồ côi; tiến độ/history/actor cùng lưu hoặc cùng rollback.
- Báo cáo tuần sai/trùng, nháp thiếu mục, lưu rồi nộp; không sửa báo cáo đã nộp; phản hồi bổ sung giữ đủ lịch sử.
- Hai tiêu chí ngoài1..5/số lẻ/thiếu nhận xét bị chặn; không chuyển điểm100 thành tiêu chí5.
- Bộ lọc/pagination/không dữ liệu/lỗi server/hết phiên; export dùng bộ lọc đã áp dụng, không ô đang sửa chưa gửi. Kiểm tra tiếng Việt, file rỗng, nhận xét dài, công thức Excel nguy hiểm giữ dạng chữ.
- Họp tương lai, kết thúc sai, không TTS, TTS ngoài phạm vi, stale revision, quyền sau bàn giao, hủy giữ lịch sử. SMTP nhận thật cần BE1 và địa chỉ nhận đã thống nhất.
- Desktop và màn hình nhỏ; tab/label bàn phím; thao tác nhanh giữa nhiều bộ lọc không hiện phản hồi cũ đè phản hồi mới.

Trạng thái: code/build/test sẽ được ghi trong PR. Các tình huống QA chưa chạy phải để chưa kiểm thử; tài liệu này là hướng dẫn, không phải chứng nhận toàn bộ42 US đã pass.

## Rà tích hợp 10/10/2026

- Đã merge develop2038801 vào branch FE3, không conflict; build trên máy FE3 đạt354 test,68 test opt-in bỏ qua, không failure/error. Hai bộ frontend test Node chạy từ bản source đã archive:10/10 đạt. Máy FE3 chưa có Node trong PATH của SSH; không ghi nhầm đây là kết quả Node chạy trực tiếp trên máy FE3.
- Bản preview riêng kết hợp FE3 mới với BE4 mới và database dữ liệu tổng hợp: HR đăng nhập vào `/hr/dashboard`, dữ liệu và tiếng Việt đúng; mở kết quả cũ, báo cáo cuối kỳ, lịch sử hai tiêu chí có đúng tên mentor/time/comment. Tải thật Excel/PDF từ nút giao diện thành công. Dữ liệu giả chỉ phục vụ kiểm tra, không chèn vào database ứng dụng.
- Menu chung từ develop còn đánh dấu Chấm công/Phụ cấp “Sắp có” dù BE2 đã có route. Bàn giao FE1 bật liên kết sau khi BE2/FE2 xác nhận, không sửa header/sidebar chung trong PR FE3.
- SMTP thật, US38 thiết bị QR/thẻ, test chéo Mentor A/B/TTS X/Y trên môi trường nhóm và QA/UAT đầy đủ vẫn cần xác nhận riêng. Không coi kết quả build/preview là chứng nhận100% Sprint3.
