# Tách controller MVC theo chức năng

## Phạm vi

Bản cập nhật ngày 25/10/2026 thay hai lớp `PageController` và `FormController`
bằng các controller theo chức năng. Mỗi controller quản lý các trang, form và
thao tác liên quan cùng một miền nghiệp vụ. Không thay REST API, giao diện,
Service, Repository, cơ sở dữ liệu hoặc cấu hình phân quyền.

Thư mục gốc của package là `src/main/java/vn/edu/detaihub/controller/`.

```text
controller/
├── api/                           Giữ nguyên REST API
├── PageAdvice.java                Giữ dữ liệu chung cho giao diện
├── mvc/
│   ├── HomeController.java        Đăng nhập và trang tổng quan
│   ├── TopicController.java       Đề tài và form đề tài
│   ├── RegistrationCycleController.java  Đợt và nhập danh sách đủ điều kiện
│   ├── GroupController.java       Nhóm và lời mời
│   ├── TopicRegistrationController.java  Đăng ký và xét duyệt
│   ├── SubmissionController.java  Nộp và tải báo cáo
│   ├── AssessmentController.java  Phản biện, hội đồng và phiếu chấm
│   ├── ResultController.java      Công bố, mở lại và kết quả
│   ├── AnnouncementController.java  Thông báo
│   ├── AccountController.java     Tài khoản và vai trò
│   ├── DepartmentController.java  Bộ môn
│   ├── MailController.java        Theo dõi và xem trước email
│   ├── AuditController.java       Nhật ký
│   ├── ProfileController.java     Hồ sơ cá nhân và đổi mật khẩu
│   └── FormFallbackController.java  Form không tồn tại trả 404
└── support/
    └── FormViewSupport.java       Metadata form, lỗi và redirect dùng chung
```

## Những gì được giữ nguyên

- Các URL đang dùng, tham số, HTTP method, view Thymeleaf và model attributes.
- Các kiểm tra quyền ở Service và các điều kiện riêng của form nhóm/hội đồng.
- BindingResult và Bean Validation của form. Khi lỗi vẫn giữ dữ liệu đã nhập,
  danh sách trường và thông báo như trước.
- POST form thành công vẫn dùng thông báo “Đã lưu dữ liệu thành công.”.
  Các thao tác trang vẫn dùng “Đã lưu thay đổi thành công.”.
- CSRF, form login, session, filter và cấu hình bảo mật.
- Tải file vẫn dùng tên UTF-8 và kiểm quyền thông qua SubmissionService.
- PageAdvice vẫn cung cấp `me`, `path`, `mailMode`, `demoSeed` cho các controller.

## Cách đọc và sửa tiếp

Ví dụ, các trang và thao tác đề tài nằm trong `mvc/TopicController.java`.
Hai phương thức `form` và `save` phụ trách GET/POST `/forms/topics`.
Phương thức `fields` của chính lớp này mô tả nhãn và loại trường hiển thị.
Các quy tắc nghiệp vụ vẫn nằm ở `TopicService`, không chuyển vào controller.

`FormViewSupport` chỉ xử lý trình bày chung. Lớp này không quyết định quyền,
trạng thái học thuật hoặc điều kiện đăng ký. Không cần sửa template `form.html`
vì cấu trúc các thuộc tính `form`, `fields`, `title`, `type` được giữ nguyên.

GET `/forms/{type}` trước đây được thay bằng các GET cụ thể theo chức năng.
Fallback vẫn nhận `id` kiểu Long và trả thông báo 404 cũ với type không hợp lệ.
Việc thêm các mapping cụ thể không đổi URL mà người dùng đang truy cập.

## Kiểm chứng

```powershell
mvn --batch-mode --no-transfer-progress clean verify
```

Kết quả ngày 25/10/2026: **BUILD SUCCESS**, 178 test, 0 failures, 0 errors,
0 skipped. Thêm 22 trường hợp so với 156 test trước khi tách. Test sử dụng H2
trong bộ nhớ hoặc Mockito, không chạy trên database MySQL thực tế.

- Giữ toàn bộ bộ test nghiệp vụ và bảo mật đã có.
- `MvcControllerStructureTest` đối chiếu toàn bộ mapping cũ và các GET form cụ thể.
- `FormViewSupportTest` kiểm tra metadata, lỗi validation/nghiệp vụ, flash và redirect.
- `ApplicationTest` kiểm tra các form và PageAdvice qua MockMvc.

Chỉ xóa hai file controller cũ sau khi đã lưu bản gốc trong thư mục staging của
phiên làm việc. Không xóa route hoặc chức năng, không tạo commit hoặc push.
