# Hệ thống quản lý đề tài sinh viên

Đồ án môn Lập trình Web, quản lý việc đề xuất đề tài, lập nhóm sinh viên, đăng ký, nộp báo cáo và chấm điểm.

Website: [hcinnamon.world](https://hcinnamon.world). Ứng dụng đang chạy trên AWS EC2.

## Công nghệ

- Java 21, Spring Boot 4.1.1.
- Spring MVC và Thymeleaf cho giao diện, Spring Security cho đăng nhập và phân quyền.
- MySQL, Spring Data JPA và Flyway. Test dùng H2 và Mockito.

## Chức năng

- Admin quản lý tài khoản và vai trò. Quản lý khoa cập nhật hồ sơ giảng viên, sinh viên, quản lý đợt đăng ký và công bố kết quả.
- Giảng viên đề xuất đề tài, gửi duyệt và theo dõi nhóm được hướng dẫn. Đợt đăng ký có thời gian riêng cho giảng viên và sinh viên.
- Sinh viên tạo nhóm, mời thành viên và đăng ký đề tài. Nhóm phải đủ 3–5 người khi đăng ký, có một trưởng nhóm. Một sinh viên không được tham gia hai nhóm đang hoạt động trong cùng đợt.
- Trưởng nhóm nộp báo cáo PDF, DOCX hoặc ZIP, tối đa 20 MB. Hệ thống giữ các phiên bản đã nộp và kiểm tra quyền khi tải file.
- Phân công phản biện, lập hội đồng 3–5 giảng viên, chấm điểm và xác nhận kết quả. Giảng viên không được chấm đề tài mình hướng dẫn.
- Sinh viên xem kết quả đã công bố của nhóm mình. Có thông báo, tìm kiếm và lọc đề tài, dashboard, nhật ký thao tác, nhập danh sách sinh viên đủ điều kiện đăng ký.

Chi tiết quyền của từng vai trò nằm trong [tài liệu phân quyền](plans/01-chuc-nang-va-phan-quyen.md).

## Chạy local

Cần JDK 21 và MySQL Server 8.x. Mở PowerShell tại thư mục chứa `pom.xml`, chạy:

```powershell
.\scripts\start.ps1
```

Mở [localhost:8080](http://localhost:8080). Dừng web bằng `Ctrl+C`.

Script khởi động một MySQL riêng ở `127.0.0.1:3307`, lưu dữ liệu trong `.local/mysql-data`. Nó không dùng dịch vụ MySQL ở cổng 3306. Đường dẫn MySQL mặc định là `C:\Program Files\MySQL\MySQL Server 8.0`.

Nếu MySQL được cài ở chỗ khác, chạy lệnh này trước, thay đường dẫn bằng thư mục cài của bạn:

```powershell
.\scripts\start-local-db.ps1 -MySqlHome "D:\MySQL\MySQL Server 8.0"
.\scripts\start.ps1 -SkipDatabase
```

Nếu PowerShell chặn script:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start.ps1
```

Sau khi dừng web, có thể dừng MySQL riêng bằng lệnh dưới. Dữ liệu vẫn được giữ lại.

```powershell
.\scripts\stop-local-db.ps1
```

### Dùng MySQL có sẵn

Tạo database riêng cho project rồi đặt biến môi trường trong terminal đang dùng:

```powershell
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/detaihub?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8'
$env:DB_USER='tai_khoan_database'
$env:DB_PASSWORD='mat_khau_database'
.\mvnw.cmd spring-boot:run
```

Flyway tự chạy migration khi ứng dụng khởi động. Không trỏ ứng dụng hoặc test vào database của dự án khác.

Nếu dùng Docker, có thể thay bước khởi động MySQL local bằng:

```powershell
docker compose up -d
.\scripts\start.ps1 -SkipDatabase
```

Docker cũng dùng cổng 3307, vì vậy không chạy đồng thời với MySQL riêng của script.

## Cấu trúc code

```text
src/main/java/vn/edu/detaihub/
  controller/
    mvc/         Controller theo chức năng, trả về trang Thymeleaf
    api/v1/      Các REST API
    support/     Xử lý form dùng chung
  service/       Nghiệp vụ và transaction
  repository/    Truy vấn database
  model/         Entity JPA
  dto/           Dữ liệu form và dữ liệu trả về
  security/      Đọc tài khoản, xử lý đăng nhập và kiểm tra phiên
  config/        Cấu hình Spring Security, dữ liệu mẫu, mail worker
  exception/     Xử lý lỗi
  aspect/        Ghi log service
src/main/resources/
  templates/     HTML Thymeleaf và mẫu email
  static/        CSS, JavaScript, hình ảnh
  db/migration/  Các file SQL của Flyway
src/test/        Test nghiệp vụ, phân quyền và controller
scripts/         Script chạy local và kiểm tra giao diện
plans/           Tài liệu phân tích và thiết kế
```

Sửa giao diện trong `templates` và `static`. Nghiệp vụ nằm ở `service`, controller chỉ nhận dữ liệu và gọi service. Cấu hình đăng nhập nằm trong `SecurityConfig.java`, các thành phần xử lý được tách vào `security`.

## Kiểm thử và đóng gói

```powershell
.\mvnw.cmd clean verify
java -jar target/de-tai-hub-1.0.0.jar
```

Test mặc định dùng H2 trong RAM, không dùng database MySQL đang chạy. Kết quả test nằm trong `target/surefire-reports`.

Khi sửa source, cần đóng gói lại JAR trước khi chạy bản mới. Trên Windows, dừng tiến trình đang dùng JAR trước khi build lại.

## Cấu hình và dữ liệu

Cấu hình nằm trong [application.properties](src/main/resources/application.properties), lấy giá trị từ biến môi trường. Chạy trực tiếp bằng Maven hoặc JAR không tự đọc file `.env`.

- Database dùng `DB_URL`, `DB_USER`, `DB_PASSWORD`.
- Địa chỉ và cổng ứng dụng dùng `APP_ADDRESS`, `PORT`. Mặc định là `127.0.0.1:8080`.
- Báo cáo nộp lên lưu tại `APP_STORAGE`, mặc định `.local/uploads`. Khi sao lưu cần giữ cả database và thư mục này.
- `DEMO_SEED=true` tạo dữ liệu mẫu khi chưa có tài khoản. Danh sách tài khoản mẫu nằm trong [DemoData.java](src/main/java/vn/edu/detaihub/config/DemoData.java). Tắt seed không xóa các tài khoản đã tạo trước đó.

Nếu tắt seed ngay trên database trống, cần chuẩn bị tài khoản ban đầu vì web không có trang đăng ký công khai.

Email mặc định chạy ở chế độ `MAIL_MODE=preview`, chỉ tạo file `.eml` trong `.local/mail`, không gửi đến hộp thư thật. Chức năng SMTP đã có trong code nhưng chưa kiểm tra gửi thư thật trên bản deploy.

Để gửi qua SMTP, cấu hình `MAIL_MODE=smtp`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_TLS` và `MAIL_FROM`. `BASE_URL` là địa chỉ website dùng trong liên kết của email. Nếu dùng Mailpit trong Docker, xem thư tại [localhost:8025](http://localhost:8025).

Không đưa mật khẩu database, SMTP, file `.env` hoặc khóa SSH lên Git. Trên bản deploy, tắt seed và đổi mật khẩu hoặc khóa các tài khoản demo không sử dụng.
