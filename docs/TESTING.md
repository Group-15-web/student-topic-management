# Kết quả kiểm chứng — 17/10/2026

## Tự động

`mvn package` đã thành công trên Java 21 / Spring Boot 4.1.1.

| Nhóm test | Số ca | Kết quả |
|---|---:|---|
| ApplicationTest | 115 | 0 lỗi / 0 thất bại |
| ConcurrentRegistrationTest (H2) | 1 | Qua |
| EmailDeliveryTest | 1 | Qua |
| Tổng mặc định | **117** | **Qua toàn bộ** |

ApplicationTest có 99 lượt render trang/form theo 7 tài khoản, cùng các test login đúng/sai, thiếu CSRF, truy cập chéo, chặn Admin/SV công bố, đổi securityVersion hủy phiên, phiếu thiếu dữ liệu, luồng điểm/công bố/thu hồi, người hướng dẫn bị cấm chấm, file sai định dạng, nhóm trùng, đăng ký/duyệt, nhóm trưởng, lời mời và CRUD cơ bản.

EmailDeliveryTest dùng JavaMail tạo MIME thật ở chế độ preview, kiểm tra file `.eml`, trạng thái và hủy bản công bố không còn hiệu lực. Không dùng SMTP bên ngoài.

ConcurrentRegistrationTest được chạy thêm trên **MySQL 8.0.46 thật**, schema riêng `detaihub_concurrency_test`: hai transaction đồng thời đăng ký đề tài còn 1 chỗ, đúng một transaction thành công. Không chạy bài này trên schema ứng dụng.

## Trình duyệt thật

Edge headless qua Playwright, ứng dụng Spring Boot kết nối MySQL:

- `scripts/ui-smoke.cjs`: đăng nhập, dashboard, đề tài, hồ sơ, form đợt/đề tài, tài khoản; không có lỗi JavaScript/HTTP ở các trang đã kiểm. Desktop 1440×1000 và dashboard mobile 390×844 không tràn ngang.
- `scripts/ui-workflow.cjs`: chạy riêng cổng 8081, schema `detaihub_browser_test`, giữ nguyên dữ liệu demo cổng 8080.
- Nhóm trưởng upload PDF → download được bảo vệ → GVPB và 3 thành viên HĐ gửi 8, 9, 7, 10 → chủ tịch xác nhận → Khoa công bố danh sách → kết quả **8,50** → **3 bản email PREVIEW** → SV trong nhóm thấy điểm, SV ngoài nhóm không thấy điểm và nhận HTTP 403 khi truy cập hồ sơ trực tiếp.

Ảnh minh chứng sinh vào `target/screenshots/`: login, dashboard, mobile, topics, registration, forms, accounts, e2e-published, e2e-mail. Log runtime nằm trong `.local/`; các thư mục này không commit vào source.

## Giới hạn của bằng chứng

- Render trang không tương đương đã tự động kiểm tra mọi nhánh nghiệp vụ. Các giả định đề bài vẫn cần xác nhận theo `plans/`.
- Chưa benchmark tải 1.000 đề tài/200 nhóm, chưa kiểm thử khôi phục backup/SMTP nhà cung cấp thật/hosting.
- Browser E2E là bài ghi dữ liệu: chỉ chạy trên database test mới có seed và cổng 8081. Chạy lại trên dữ liệu đã công bố sẽ không đi lại được bước chấm do khóa phiếu — đó là hành vi bảo vệ dữ liệu, không nên xóa khóa để làm test qua.
- Docker Compose được cung cấp như phương án khác, chưa chạy Docker trên máy hiện tại (máy này dùng MySQL cài trực tiếp).

Các schema test và file test được giữ để đối chiếu; không tự xóa database/dữ liệu của người dùng.
