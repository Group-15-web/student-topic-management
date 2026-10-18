# Kế hoạch đồ án quản lý đề tài sinh viên

Ngày lập: 17/10/2026. Cơ sở: toàn bộ 10 tài liệu trong `C:/Users/ADMIN/Downloads/WEB` và yêu cầu của người dùng rằng đồ án dùng Spring MVC, kiến trúc kế thừa 6 bài lab, có DTO.

Đây là kế hoạch để nhóm thống nhất và triển khai, chưa phải xác nhận đã xây dựng hay kiểm thử ứng dụng. Không tạo file Word trong lần lập kế hoạch này.

## Đọc theo thứ tự

1. [Chức năng, phân quyền và các quyết định nghiệp vụ](01-chuc-nang-va-phan-quyen.md): phạm vi, actor, ma trận quyền, danh mục FR, vòng đời và các câu hỏi cần chốt.
2. [Kiến trúc MVC, DTO và thiết kế dữ liệu](02-kien-truc-mvc-dto-va-du-lieu.md): cấu trúc theo lab, hợp đồng giữa các tầng, database, bảo mật, email và deployment.
3. [Lộ trình triển khai, kiểm thử và bàn giao](03-lo-trinh-kiem-thu-va-ban-giao.md): công việc theo giai đoạn, chia nhóm 3–5 người, tiêu chí hoàn thành, test và đối chiếu rubric.

## Kết luận để nhóm thống nhất

- Một ứng dụng Spring Boot monolith, giao diện Thymeleaf theo Spring MVC, dữ liệu MySQL theo Lab 3 và Deployment. DTO là đối tượng truyền dữ liệu, không phải một kiến trúc thay thế MVC.
- Package cấp đầu bám sát lab: `controller`, `dto`, `model`, `repository`, `service`, `config`, `exception`, `aspect`; bên trong có thể chia tiếp theo nghiệp vụ. Thay đề xuất PostgreSQL và package theo feature trước đây bằng phương án sát tài liệu môn học hơn.
- `@Controller` render HTML; `@RestController` ở `controller/api/v1` phục vụ JSON; cả hai dùng cùng service và quy tắc phân quyền.
- Đề xuất baseline Java 25 và Spring Boot 4.1.x theo Lab 1. Khóa phiên bản patch và dependency thực tế sau bài kiểm tra khởi động ở giai đoạn 0, vì các lab có chỗ pha ví dụ Boot 3/Security 6 và Boot 4. Không chép nguyên tất cả dependency giữa các phiên bản.
- Làm đủ nghiệp vụ đề bài/rubric trước; AOP, email bất đồng bộ và một phần REST được đưa vào kế hoạch để kế thừa đủ 6 lab. Không cần xây hai ứng dụng frontend/backend tách biệt.
- Cần thể hiện Trưởng khoa, Trưởng bộ môn, GVHD, GVPB, Chủ tịch/Thư ký/Thành viên hội đồng, Sinh viên và Nhóm trưởng trên use case chi tiết. Tài khoản hệ thống có thể giữ 5 role nền; các quyền theo phân công được suy ra từ dữ liệu nghiệp vụ.
- Không ép thiết kế vào đúng 20 bảng. Kế hoạch sử dụng 20 bảng nghiệp vụ nền và đề xuất 2 bảng kỹ thuật cho email/audit, có thể bổ sung khi một yêu cầu đã được xác nhận cần nó.

## Sửa các giả định trong trao đổi và sơ đồ cũ

| Nội dung cũ | Quyết định trong kế hoạch này | Cơ sở |
|---|---|---|
| Có chức năng chấm điểm hướng dẫn | Bỏ quyền GV chấm chính đề tài mình hướng dẫn, kể cả đồng hướng dẫn | Đề bài trang 1; rubric trang 3 |
| Chỉ có 4 actor là đủ | Bổ sung Trưởng bộ môn và tách các vai trò theo phân công trên use case chi tiết | Rubric trang 2 |
| GVHD duyệt đăng ký nhóm | Chưa được đề bài chỉ rõ; đề xuất Trưởng khoa duyệt, GVHD xem/đề xuất ý kiến; cần xác nhận | Đề bài chỉ nói đăng ký được chấp thuận |
| Quản lý khoa tổng hợp mọi điểm | Chủ tịch tổng hợp phần hội đồng; hệ thống tính trung bình; Khoa công bố kết quả | Đề bài trang 1 |
| Thông báo là phần có thể để sau | CRUD thông báo nằm trong chức năng cơ bản, phải làm | Rubric trang 3 |
| Gửi email chỉ là mở rộng | Email cho từng SV khi công bố kết quả là yêu cầu trong mức hoàn thành đầy đủ; các email nhắc hạn khác mới là mở rộng | Rubric trang 3 và mẫu báo cáo |
| Database mặc định PostgreSQL | MySQL để thống nhất với Lab 3, Lab 4 và tài liệu Deployment | Lab 3–4, Deployment |

File sơ đồ use case đã tạo trước đó là bản nháp trước khi đọc đề bài; không dùng nguyên bản đó để nộp. Giai đoạn 0 phải cập nhật sơ đồ theo tài liệu 01. Các file cũ chưa bị sửa hoặc ghi đè trong lần lập kế hoạch này.

## Quy ước mức độ yêu cầu

- **Nguồn**: yêu cầu được nói trực tiếp trong đề bài/rubric/mẫu báo cáo hoặc kiến thức cần áp dụng từ lab.
- **Đề xuất**: quyết định thiết kế để hiện thực yêu cầu; không khẳng định là quy định của thầy.
- **Cần xác nhận**: nguồn thiếu chi tiết hoặc mâu thuẫn. Kế hoạch có phương án tạm để nhóm tiếp tục thiết kế; không coi phương án tạm là yêu cầu đã được phê duyệt.
- Mức **P0**: đủ nghiệp vụ đề bài/rubric; **P1**: độ hoàn thiện kỹ thuật, deployment, minh chứng; **P2**: mở rộng sau khi P0 chạy tốt. Email công bố điểm thuộc P0, không phải P2.

## Danh mục tài liệu đã đọc và nội dung sử dụng

Đã trích đọc toàn bộ 78 trang của 9 PDF và toàn bộ đoạn/bảng của mẫu DOCX; kiểm tra hình ảnh các trang đề bài, rubric và các trang hướng dẫn deployment chỉ chứa ảnh. Mẫu DOCX đã đọc được nội dung/bảng; công cụ render thiếu LibreOffice nên chưa xác minh bố cục trang của mẫu, không ảnh hưởng đến việc trích yêu cầu kế hoạch.

| Mã | Tài liệu nguồn | Phần đưa vào kế hoạch |
|---|---|---|
| S1 | [Đồ án cuối kỳ](<C:/Users/ADMIN/Downloads/WEB/Đồ án cuối kỳ.pdf>) — 3 trang | Chu trình nghiệp vụ, thời gian theo loại đợt, 1–2 GVHD, nhóm, hội đồng, công bố, cấu trúc báo cáo và nộp bài |
| S2 | [Rubric](<C:/Users/ADMIN/Downloads/WEB/Rubric.pdf>) — 4 trang | Vai trò Trưởng bộ môn, đủ CRUD, giới hạn nhóm/hội đồng, email, phạm vi tra cứu điểm, điểm kiến trúc/demo/vấn đáp |
| S3 | [Mẫu báo cáo kết quả nhóm](<C:/Users/ADMIN/Downloads/WEB/File báo cáo kết quả dự án của mỗi nhóm (1).docx>) | 8 hạng mục tự đánh giá, vị trí file/route minh chứng, phân công, tỷ lệ đóng góp và trách nhiệm cá nhân |
| L1 | [Lab 1](<C:/Users/ADMIN/Downloads/WEB/Lab 1 - Controller and Thymeleaf (1).pdf>) — 11 trang | MVC, Thymeleaf, DTO, Bean Validation, constructor injection, form binding |
| L2 | [Lab 2](<C:/Users/ADMIN/Downloads/WEB/Lab 2 - RestController.pdf>) — 8 trang | REST `/api/v1`, dùng chung service với HTML, JSON, lỗi 404 |
| L3 | [Lab 3](<C:/Users/ADMIN/Downloads/WEB/Lab 3 - Spring Data JPA Basic.pdf>) — 10 trang | MySQL, JPA Entity, JpaRepository, transaction, dữ liệu tồn tại sau restart |
| L4 | [Lab 4](<C:/Users/ADMIN/Downloads/WEB/Lab 4 - Spring Security.pdf>) — 13 trang | BCrypt, UserDetailsService, FilterChain, method security, CSRF, `sec:authorize` |
| L5 | [Lab 5](<C:/Users/ADMIN/Downloads/WEB/Lab 5 - Aspect-Oriented Programming (1).pdf>) — 7 trang | Logging, audit, đo thời gian bằng AOP |
| L6 | [Lab 6](<C:/Users/ADMIN/Downloads/WEB/Lab 6 - Sending emails (1).pdf>) — 8 trang | JavaMailSender, Thymeleaf email, `@Async`, Mailpit local |
| D1 | [Deployment](<C:/Users/ADMIN/Downloads/WEB/Deployment.pdf>) — 14 trang | JAR, Docker nhiều giai đoạn, GitHub, biến môi trường, MySQL cloud, HTTPS |

## Thông tin nhóm chưa cung cấp

Chưa có deadline, tên và thế mạnh thành viên, repo bài lab đang chạy. Lộ trình dùng mốc tương đối cho 3–5 thành viên, không tự đặt hạn nộp hay gán phần trăm đóng góp. Những nội dung này không cản trở việc chốt kiến trúc và phân rã công việc.
