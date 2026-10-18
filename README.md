# Đề Tài Hub — quản lý đề tài sinh viên

Ứng dụng web chạy local theo **Spring MVC + Thymeleaf**, tách Controller → DTO → Service → Repository → Entity. Không cần React, Node.js hay hosting để chạy ứng dụng. MySQL lưu dữ liệu; Flyway quản lý cấu trúc database; Spring Security quản lý phiên đăng nhập và phân quyền.

## 1. Chạy trên máy này

Yêu cầu: **JDK 21**, MySQL Server 8.x. Maven đã có trên máy; repo cũng có Maven Wrapper để dùng khi chưa cài Maven.

Trong PowerShell, mở thư mục dự án và chạy:

```powershell
.\scripts\start.ps1
```

Mở **http://localhost:8080**. Dừng web bằng `Ctrl+C` trong cửa sổ chạy lệnh.

Nếu muốn chạy nền: `scripts/start-background.ps1` (dùng JAR đã build; khi sửa code cần `mvnw.cmd package` lại). Dừng đúng tiến trình chạy nền bằng `scripts/stop-web.ps1`.

- Script tạo một MySQL **riêng tại `127.0.0.1:3307`**, dữ liệu nằm trong `.local/mysql-data`.
- **Không thay đổi dịch vụ MySQL80 đang có ở cổng 3306.**
- MySQL mặc định được tìm ở `C:\Program Files\MySQL\MySQL Server 8.0`. Nếu máy khác cài chỗ khác, chạy `scripts/start-local-db.ps1 -MySqlHome 'đường dẫn MySQL'` trước.
- Database tự được migrate và tạo dữ liệu demo **một lần khi chưa có tài khoản**. Khởi động lại không xóa dữ liệu.
- Nếu PowerShell chặn script, chạy `powershell -ExecutionPolicy Bypass -File .\scripts\start.ps1` cho riêng lần chạy này; không cần đổi chính sách toàn máy.
- Để dừng MySQL riêng sau khi dừng web: `.\scripts\stop-local-db.ps1`. Dữ liệu được giữ nguyên.
- Không chạy `package` trong khi JAR đang được Java sử dụng trên Windows; dừng web trước khi đóng gói lại.

### Dùng database đã có / Docker

Tạo database riêng và cấp quyền cho tài khoản ứng dụng, rồi đặt cấu hình trong phiên terminal:

```powershell
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/ten_database_rieng?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8'
$env:DB_USER='tai_khoan_ung_dung'
$env:DB_PASSWORD='mat_khau_cua_ban'
.\mvnw.cmd spring-boot:run
```

Không trỏ vào database của đồ án khác. File `compose.yaml` là lựa chọn thay thế nếu đã cài Docker: `docker compose up -d`, rồi chạy web với `scripts/start.ps1 -SkipDatabase`. Không dùng đồng thời Docker và MySQL local ở cổng 3307.

## 2. Tài khoản demo

Tất cả dùng mật khẩu **`Demo@2026!`**. Trang đăng nhập có nút điền nhanh vai trò.

| Tài khoản | Vai trò / tình huống demo |
|---|---|
| `admin@demo.local` | Tạo/khóa tài khoản, cấp role, reset mật khẩu, danh mục |
| `khoa@demo.local` | Trưởng khoa: đợt, duyệt đề tài/nhóm, phân công, công bố điểm |
| `tbm@demo.local` | Trưởng bộ môn CNPM: theo dõi/rà soát dữ liệu bộ môn |
| `gv1@demo.local` | Giảng viên đề xuất; GVHD nhóm 02, không được chấm nhóm này |
| `gv2@demo.local` | Giảng viên phản biện nhóm 02 |
| `gv3@demo.local` | Chủ tịch hội đồng của nhóm 02 |
| `gv4@demo.local` | Thư ký hội đồng của nhóm 02 |
| `gv5@demo.local` | Thành viên hội đồng của nhóm 02 |
| `gv6@demo.local` | Giảng viên bộ môn HTTT |
| `sv1@demo.local` | Nhóm trưởng nhóm 01, có 3 người, sẵn sàng đăng ký |
| `sv2@demo.local`, `sv3@demo.local` | Thành viên nhóm 01 |
| `sv4@demo.local` | Nhóm trưởng nhóm 02 đã được duyệt, có thể nộp báo cáo |
| `sv5@demo.local`, `sv6@demo.local` | Thành viên nhóm 02 |
| `sv7@demo.local` … `sv12@demo.local` | Chưa có nhóm; dùng để thử tạo nhóm/mời tham gia |

MSSV tương ứng `SV001` … `SV012`. Các mốc thời gian demo được tính từ **lần khởi tạo dữ liệu đầu tiên**. Khi dùng lại lâu ngày, Khoa cần điều chỉnh thời gian đợt, không xóa database.

Đây là tài khoản/mật khẩu **chỉ dành cho demo local**. Không mở cổng ra Internet hoặc dùng cho dữ liệu thật khi chưa thay thông tin đăng nhập, tắt seed và hoàn tất rà soát triển khai.

## 3. Kịch bản demo từ đầu đến cuối

### Đề xuất và đăng ký

1. `gv1`: tạo đề tài trong đợt TLCN đang nhận đề xuất, lưu nháp rồi gửi duyệt.
2. `khoa`: mở đề tài, phân công 1–2 GVHD, duyệt rồi công bố.
3. Để đăng ký ngay, `sv1` chọn một đề tài còn chỗ trong đợt KLTN đang mở giai đoạn SV; nhóm 01 đã có đủ 3 người.
4. `khoa`: Hồ sơ đề tài → mở đăng ký → chấp thuận hoặc từ chối có lý do.
5. Muốn thử tạo nhóm: dùng `sv7`, mời `SV008`/`SV009`; đăng nhập từng SV để chấp nhận. Chưa đủ 3 người thì không được đăng ký.

### Nộp báo cáo, chấm và công bố

1. `sv4`: Hồ sơ đề tài → nhóm 02 → nộp PDF/DOCX/ZIP thật, tối đa 20 MB.
2. `gv2`, `gv3`, `gv4`, `gv5`: Đánh giá & chấm điểm → phiếu được giao → nhập điểm 0–10, nhận xét, chọn **gửi phiếu chính thức**. Lưu nháp không được tính là đã gửi.
3. `gv3`: mở hồ sơ nhóm 02 → xác nhận tổng hợp hội đồng khi tất cả thành viên đã gửi phiếu.
4. `khoa`: Kết quả → bảng hồ sơ chờ công bố → xem lý do nếu còn thiếu, chọn hồ sơ đủ điều kiện và công bố; hoặc công bố ngay trong trang hồ sơ.
5. `sv4`, `sv5`, `sv6`: xem điểm đã công bố. `sv1` không thấy điểm của nhóm 02 dù sửa URL.
6. `khoa`: Email công bố → sau khoảng 10 giây có bản `.eml` cho **từng** SV; tải và mở bằng phần mềm đọc email. Admin chỉ xem trạng thái kỹ thuật, không tải nội dung điểm.
7. Nếu cần sửa điểm, Khoa thu hồi/mở lại với lý do; GV sửa phiếu của chính mình, chủ tịch xác nhận lại, Khoa tái công bố phiên bản mới.
8. Khi hoàn tất, Khoa lưu trữ nhóm để SV có thể tham gia nhóm mới. Lịch sử nhóm/điểm vẫn được giữ.

## 4. Chức năng đã triển khai

- Đăng nhập/đăng xuất, đổi mật khẩu, cập nhật tên; BCrypt, CSRF, làm mới quyền mỗi request, hủy phiên cũ khi mật khẩu/phiên bản bảo mật thay đổi.
- Quản lý tài khoản/hồ sơ SV/GV, role, khóa/mở, đặt lại mật khẩu; bộ môn và Trưởng bộ môn. Admin tạo tài khoản; Khoa cập nhật hồ sơ học thuật đã có.
- Bốn loại đợt, lịch GV/SV, hạn nộp báo cáo/phản biện, ngày hội đồng; kiểm tra thời gian ở server.
- CRUD đề tài nháp, gửi duyệt, trả sửa/từ chối, duyệt, công bố, phân công 1–2 GVHD; rà soát bộ môn; tìm kiếm/lọc/phân trang.
- Nhóm nháp, lời mời có xác nhận, rời nhóm/chuyển trưởng nhóm; đăng ký, rút đăng ký chờ duyệt, duyệt/từ chối và khóa nhóm.
- Upload báo cáo nhiều phiên bản, download có kiểm quyền, không lưu file trong thư mục public.
- Phân công GVPB; hội đồng 3–5 người, chủ tịch/thư ký; sửa/xóa hội đồng chưa được giao nhóm; kiểm tra trùng đúng giờ/phòng/GV; giao nhóm cùng đợt.
- Chấm nháp/gửi chính thức, cấm GVHD/đồng GVHD tự chấm, kiểm tra quyền sở hữu phiếu; thư ký xem tiến độ nộp, chủ tịch xác nhận.
- Bảng chờ công bố với lỗi thiếu dữ liệu; trung bình cộng từng phiếu; công bố từng hồ sơ hoặc danh sách đã chọn; chống tạo email lặp khi bấm lại.
- Tra cứu kết quả đúng nhóm/đợt; thu hồi/tái công bố có lịch sử; hoàn tất nhóm.
- Thông báo theo đối tượng/bộ môn, dashboard theo phạm vi, nhật ký nghiệp vụ, AOP đo thời gian/lỗi service.
- REST API chọn lọc `/api/v1`, dùng chung service và DTO với MVC.

Ma trận quyền chi tiết ở `plans/01-chuc-nang-va-phan-quyen.md`; quyết định triển khai và giới hạn ở `docs/IMPLEMENTATION.md`.

## 5. Email và cấu hình

Mặc định `MAIL_MODE=preview`: **không gửi email thật**, tạo MIME `.eml` tại `.local/mail`. Không cần Gmail hay dịch vụ ngoài để chạy demo.

Muốn gửi qua máy chủ SMTP do bạn quản lý, đặt các biến `MAIL_MODE=smtp`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_TLS`, `MAIL_FROM`. Không ghi mật khẩu thật vào Git. `compose.yaml` có Mailpit để hứng email local, xem ở `http://localhost:8025`; với Mailpit dùng port 1025, không auth/TLS. Việc gửi SMTP ngoài chưa được kiểm chứng với tài khoản thật trong bản bàn giao.

Biến khác: `PORT`, `APP_ADDRESS` (mặc định chỉ loopback), `APP_STORAGE`, `MAIL_DIR`, `BASE_URL`, `DEMO_SEED`. Khi `DEMO_SEED=false` trên database mới, cần quy trình cấp tài khoản ban đầu; không tự mở trang đăng ký công khai.

## 6. Cấu trúc source

```text
src/main/java/vn/edu/detaihub/
  config/        Security, dữ liệu demo, mail worker
  controller/    MVC controllers và api/v1 REST
  dto/           Forms (input validation), Views (output an toàn)
  service/       Nghiệp vụ, transaction, quyền theo đối tượng
  repository/    JPA repository và truy cập persistence dùng chung
  model/         22 entity JPA, không bind trực tiếp từ request
  security/      Người dùng hiện tại, role, phạm vi bộ môn
  exception/     Xử lý lỗi HTTP/form/API
  aspect/        AOP logging không ghi mật khẩu
src/main/resources/
  templates/     HTML Thymeleaf và template email
  static/        CSS/JavaScript giao diện responsive
  db/migration/  Flyway SQL: PK/FK/unique/check
src/test/        Integration, quyền, rendering, email, concurrency
scripts/        Chạy/dừng local, smoke test trình duyệt
plans/          Tài liệu phân tích từ đề bài và 6 lab
docs/           Quyết định triển khai, kiểm thử và bàn giao
.local/         Database/file/mail/log runtime, không đưa vào Git
```

## 7. Kiểm thử và đóng gói

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
java -jar target/de-tai-hub-1.0.0.jar
```

Test mặc định dùng H2 chế độ MySQL, database riêng trong RAM, không xóa dữ liệu demo MySQL. Migration đã chạy trên MySQL 8.0 thật. Bài kiểm thử tranh chỗ có thể trỏ tới **database test riêng mới** qua `TEST_DB_URL`, `TEST_DB_DRIVER=com.mysql.cj.jdbc.Driver`, `TEST_DB_USER`, `TEST_DB_PASSWORD`; tuyệt đối không trỏ test này vào database ứng dụng.

`scripts/ui-smoke.cjs` là smoke test tùy chọn với Node + Playwright + Edge; không phải dependency để chạy web. Báo cáo test ở `target/surefire-reports`; ảnh UI ở `target/screenshots`.

Không có tên miền hay triển khai hosting. Sao lưu cả database **và** `.local/uploads` khi chuyển máy; chỉ copy source không mang theo dữ liệu báo cáo đã nộp.
