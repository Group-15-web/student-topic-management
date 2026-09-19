# Hệ thống Quản lý Đề tài Sinh viên

Ứng dụng web hỗ trợ quản lý đề tài, nhóm sinh viên, đăng ký đề tài, nộp bài và đánh giá.

## Công nghệ sử dụng

- Java 21
- Spring Boot 4.1.1
- Maven Wrapper
- Spring MVC và Thymeleaf
- Spring Data JPA và Spring Security
- MySQL cho môi trường phát triển cục bộ
- Flyway để quản lý thay đổi cấu trúc cơ sở dữ liệu
- H2 cho kiểm thử tự động

## Yêu cầu môi trường

- JDK 21
- Git
- MySQL 8+ nếu chạy ứng dụng với cơ sở dữ liệu local riêng

## Chạy ứng dụng trên máy cá nhân

1. Sao chép file cấu hình local mẫu:

   ```powershell
   Copy-Item src\main\resources\application-local.example.yml src\main\resources\application-local.yml
   ```

2. Mở `application-local.yml` và điền thông tin MySQL của riêng bạn. File này đã được Git bỏ qua.

3. Chạy ứng dụng:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

4. Kiểm tra ứng dụng đang hoạt động:

   [http://localhost:8080/health](http://localhost:8080/health)

## Kiểm tra dự án

Chạy đúng lệnh mà GitHub Actions sử dụng:

```powershell
.\mvnw.cmd verify
```

## Migration cơ sở dữ liệu

Mọi thay đổi cấu trúc cơ sở dữ liệu dùng chung phải nằm trong `src/main/resources/db/migration/` dưới dạng file SQL Flyway có phiên bản:

```text
V<version>__<description>.sql
```

Không chỉnh sửa migration sau khi nó đã được chia sẻ hoặc chạy trên database. Mỗi thay đổi cấu trúc phải tạo một migration mới.

## Quy tắc cấu hình local

- Commit `application-local.example.yml`, không commit `application-local.yml`.
- Không commit mật khẩu, API key, file `.env` hoặc dữ liệu người dùng tải lên.
- Hibernate chỉ kiểm tra cấu trúc schema; Flyway là cơ chế duy nhất thay đổi schema dùng chung.
