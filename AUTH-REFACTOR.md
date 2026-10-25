# Tách chức năng đăng nhập và kiểm tra phiên

## Phạm vi

Tách các trách nhiệm đang nằm trong `SecurityConfig` thành các lớp riêng, giữ nguyên
Spring Security form login, BCrypt, session và các quy tắc phân quyền hiện có.
Không chuyển sang JWT, không thêm controller tự kiểm tra mật khẩu.

## Các file sau khi tách

Các đường dẫn dưới đây tính từ thư mục gốc của project.

| File | Trách nhiệm |
| --- | --- |
| `src/main/java/vn/edu/detaihub/config/SecurityConfig.java` | Khai báo BCrypt, đường dẫn public, form login, logout và kết nối các thành phần bảo mật. |
| `src/main/java/vn/edu/detaihub/security/DatabaseUserDetailsService.java` | Đọc email, password hash, trạng thái tài khoản và roles từ database; chuyển thành `UserDetails`. |
| `src/main/java/vn/edu/detaihub/security/LoginSuccessHandler.java` | Lưu `securityVersion` vào session sau khi đăng nhập thành công và chuyển về `/`. |
| `src/main/java/vn/edu/detaihub/security/SessionVersionFilter.java` | Kiểm tra tài khoản và phiên trên request đã đăng nhập; thu hồi phiên khi tài khoản không còn hợp lệ. |
| `src/main/java/vn/edu/detaihub/security/AppAuthenticationEntryPoint.java` | Khi chưa đăng nhập: API `/api/**` trả JSON HTTP 401, trang web chuyển về `/login`. |
| `src/main/java/vn/edu/detaihub/security/ActorService.java` | Giữ nguyên: xác định người dùng, kiểm tra role và quyền trên từng dữ liệu. |

`SessionVersionFilter` được khởi tạo trong `SecurityConfig` và thêm đúng một lần,
sau `UsernamePasswordAuthenticationFilter`. Không đặt `@Component` hoặc khai báo
filter này thành `@Bean`, để tránh bị đăng ký thêm như một servlet filter.

## Luồng xử lý

1. GET `/login`: controller hiển thị form Thymeleaf.
2. POST `/login`: Spring Security xử lý username/password và CSRF.
3. `DatabaseUserDetailsService` đọc tài khoản và roles; Spring Security kiểm tra
   mật khẩu bằng BCrypt.
4. Thành công: `LoginSuccessHandler` lưu phiên bản bảo mật, chuyển về `/`.
5. Request tiếp theo: `SessionVersionFilter` kiểm tra tài khoản và session.
6. Các kiểm tra role/quyền sở hữu ở controller, service và `ActorService` tiếp tục
   áp dụng như trước.
7. POST `/logout` có CSRF: hủy session, xóa cookie `JSESSIONID`, về `/login?logout`.

Hai thuộc tính session có mục đích khác nhau:

- `securityVersion`: phiên bản khi đăng nhập, dùng phát hiện phiên đã bị thu hồi.
- `loadedSecurityVersion`: phiên bản authorities đã nạp, tránh nạp lại roles khi
  không cần thiết. Filter vẫn kiểm tra tài khoản trong DB trên mỗi request đã
  đăng nhập.

Giữ tương thích với logic cũ: session thiếu `securityVersion` không bị tự động
hủy; filter không tạo session mới. Đây là bản tách lớp, không phải thay đổi chính
sách xác thực hoặc thu hồi quyền. Việc thay đổi role và thời điểm cập nhật
authorities vẫn theo cơ chế hiện có của project.

## Kiểm thử

```powershell
mvn --batch-mode --no-transfer-progress clean verify
```

Kết quả chạy ngày 25/10/2026: **BUILD SUCCESS**, 156 test, 0 failures,
0 errors, 0 skipped. Trước khi tách lớp: 132 test đều thành công. Test dùng H2
trong bộ nhớ, không sử dụng database MySQL thực tế.

- `src/test/java/vn/edu/detaihub/security/SecurityComponentsTest.java`: 19 unit
  test cho đọc tài khoản, login handler, authentication entry point và filter.
- `src/test/java/vn/edu/detaihub/ApplicationTest.java`: bổ sung 5 integration test
  về wiring, session sau login, tài khoản bị khóa, phiên API bị thu hồi và logout;
  siết kiểm tra redirect, JSON 401 và CSRF đang có.

## Không thay đổi

- Schema và dữ liệu database thực tế, demo seeder và cấu hình môi trường.
- Giao diện login, HTML/CSS/JavaScript: phần tài khoản demo trên login đã được bỏ
  trong bản gốc của checkout này.
- Nghiệp vụ, kiểm tra quyền sở hữu và các annotation phân quyền hiện có.
- Không tạo commit hoặc push. Việc tách lớp không đồng nghĩa project đã được kiểm
  tra đầy đủ điều kiện production/hosting.
