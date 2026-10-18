# Kiến trúc MVC DTO và dữ liệu

## 1. Kiến trúc được đề xuất

Một codebase, một ứng dụng Spring Boot, một MySQL. Giao diện HTML render phía server bằng Thymeleaf. Kế thừa cấu trúc các lab, mở rộng số module và quy tắc nghiệp vụ, không giữ dữ liệu chính trong ArrayList như bước học đầu tiên.

```text
Trình duyệt ── HTTP + session + CSRF ── Spring Security
                                            │
                         ┌──────────────────┴──────────────────┐
                         │                                     │
                 @Controller                         @RestController
                 Form Request DTO                    JSON Request DTO
                         │                                     │
                         └──────────────────┬──────────────────┘
                                            │
                                     Service dùng chung
                             quyền đối tượng + nghiệp vụ + transaction
                                            │
                                       Repository
                                            │
                                    JPA Entity / MySQL

Service → Response/View DTO → Model → Thymeleaf → HTML
Service → Response DTO → JSON (đối với /api/v1)

AOP theo dõi lời gọi service; sự kiện công bố sau commit → EmailService @Async.
```

MVC gồm Controller điều phối request, Model chứa dữ liệu cung cấp cho view và miền xử lý phía sau, View là Thymeleaf. `model/` trong lab chứa entity/domain object; `org.springframework.ui.Model` là đối tượng đưa dữ liệu vào template, không phải một bảng. DTO là hợp đồng dữ liệu vào/ra, không là entity và không tự lưu vào database.

HTML controller gọi service trực tiếp trong cùng ứng dụng; không gọi HTTP đến REST controller của chính mình. Cả hai loại controller đều không gọi repository trực tiếp. Service không phụ thuộc `HttpServletRequest`, `Model`, tên template hoặc mã HTML.

## 2. Ánh xạ 6 lab vào đồ án

| Nguồn | Kỹ thuật kế thừa | Vị trí / áp dụng | Minh chứng khi demo |
|---|---|---|---|
| Lab 1 | `@Controller`, Thymeleaf, DTO, Validation, DI | CRUD đề tài/đợt và form đăng ký, `@Valid` + `BindingResult` | Gửi form thiếu tiêu đề có lỗi ở đúng trường, nhập lại không mất dữ liệu |
| Lab 2 | `@RestController`, `/api/v1`, JSON, xử lý 404 | API đề tài và đăng ký qua cùng service | Tạo đề tài hợp lệ qua API, xuất hiện trên trang HTML với đúng quyền |
| Lab 3 | JPA, MySQL, Repository, `@Transactional` | Entity toàn hệ thống; transaction nhóm/đăng ký/công bố | Restart không mất dữ liệu; lỗi giữa chừng không tạo bản ghi nửa vời |
| Lab 4 | UserDetailsService, BCrypt, FilterChain, method security, CSRF, `sec:authorize` | Đăng nhập và policy theo đối tượng | Đổi URL hoặc gọi API trực tiếp vẫn bị chặn; menu đúng role |
| Lab 5 | `@Aspect`, advice trước/sau/lỗi, `@Around` | `AuditAspect`, `PerformanceAspect` | Thấy actor, mã đối tượng, thao tác và thời gian; không thấy mật khẩu |
| Lab 6 | JavaMailSender, Thymeleaf mail, `@EnableAsync`, `@Async` | Công bố điểm gửi từng SV, dev qua Mailpit | Nhóm 3 SV có 3 email đúng người; thử lỗi gửi và gửi lại |

Rubric yêu cầu ít nhất 3 kỹ thuật Spring; kế hoạch bao phủ nhiều hơn, nhưng chỉ ghi đã áp dụng trong báo cáo khi có code và minh chứng chạy thực tế.

## 3. Công nghệ và quyết định phiên bản

- Java 25, Maven Wrapper, Spring Boot 4.1.x là baseline đề xuất theo Lab 1. Chọn và ghi một patch cụ thể trong `pom.xml` sau khi kiểm tra build/login/render/API/AOP/mail ở G0; không mỗi người tự dùng một phiên bản.
- MySQL cho local và deploy, Spring Data JPA/Hibernate. Không đổi sang PostgreSQL chỉ vì dịch vụ hosting thuận tiện khi chưa thống nhất cả nhóm.
- Spring MVC, Thymeleaf, HTML/CSS/JavaScript. Có thể dùng một bộ CSS thống nhất; không đưa framework SPA vào phạm vi.
- Spring Security với form login/session; chưa cần JWT, OAuth hay microservice.
- Bean Validation cho DTO; ràng buộc DB và kiểm tra business trong service bổ sung cho nhau.
- AOP và Spring Mail kế thừa lab; thêm Spring Boot Test/Security Test theo baseline tương thích.
- Đề xuất Flyway hoặc SQL migration đánh số để schema của cả nhóm thống nhất. Flyway là bổ sung kỹ thuật, không được lab bắt buộc. Chọn một cách quản lý schema, không vừa migration vừa để `ddl-auto=update` tự sửa schema ở môi trường nghiệm thu.

Tài liệu có điểm pha phiên bản: Lab 1 chọn Boot 4.1.x/Java 25 hoặc 26, Lab 4 nhắc Boot 3 và security dialect 6; Lab 5 mở đầu nói starter AOP nhưng bước dependency dùng starter AspectJ. Khi scaffold, chọn dependency theo BOM/Initializr của baseline rồi chạy kiểm tra, không tự gán suffix `7` hoặc trộn starter chỉ từ suy luận tên.

Tài liệu Spring hiện hành được kiểm tra ngày 17/10/2026 xác nhận dòng Boot 4.1 trong [system requirements](https://docs.spring.io/spring-boot/system-requirements.html); hướng dẫn MVC dùng [starter Web MVC](https://docs.spring.io/spring-boot/reference/web/index.html). Tình trạng hỗ trợ Thymeleaf/Spring 7 cần đối chiếu [tracking chính thức của Thymeleaf](https://github.com/thymeleaf/thymeleaf/issues/1065) và dependency thực tế. Gate G0 phải thử `sec:authorize`; không coi việc compile thành công là đủ.

## 4. Cấu trúc thư mục bám lab

```text
project-root/
├── pom.xml
├── mvnw / mvnw.cmd / .mvn/
├── Dockerfile
├── compose.yaml                    # đề xuất MySQL + Mailpit cho local
├── .env.example                    # tên biến, không chứa secret thật
├── README.md
├── docs/                           # use case, ERD, data dictionary, test/demo
├── src/main/java/vn/edu/project/
│   ├── ProjectApplication.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── TopicController.java
│   │   ├── RegistrationCycleController.java
│   │   ├── StudentGroupController.java
│   │   ├── TopicRegistrationController.java
│   │   ├── SubmissionController.java
│   │   ├── CouncilController.java
│   │   ├── EvaluationController.java
│   │   ├── ResultController.java
│   │   ├── AnnouncementController.java
│   │   ├── admin/                  # tài khoản, hồ sơ, bộ môn
│   │   └── api/v1/                 # REST, không trả tên template
│   ├── dto/
│   │   ├── topic/                  # TopicCreateRequest, TopicEditRequest, TopicView
│   │   ├── cycle/
│   │   ├── group/
│   │   ├── registration/
│   │   ├── submission/
│   │   ├── evaluation/
│   │   ├── result/
│   │   ├── account/
│   │   └── common/                 # PageResponse, ApiError
│   ├── model/                      # các JPA entity như Lab 3
│   ├── repository/                 # interface JpaRepository
│   ├── service/                    # nghiệp vụ, transaction, EmailService
│   ├── config/                     # security, async, clock, storage
│   ├── security/                   # policy kiểm quyền theo đối tượng
│   ├── exception/                  # exception + xử lý HTML/JSON riêng
│   ├── aspect/                     # audit, thời gian
│   ├── mapper/                     # mapping DTO tường minh nếu cần
│   └── event/                      # ResultPublishedEvent và listener
├── src/main/resources/
│   ├── application.properties
│   ├── application-dev.properties
│   ├── application-test.properties
│   ├── application-prod.properties
│   ├── db/migration/
│   ├── templates/
│   │   ├── fragments/              # header, sidebar, messages, pagination
│   │   ├── auth/ dashboard/ admin/
│   │   ├── topics/ cycles/ groups/ registrations/
│   │   ├── submissions/ councils/ evaluations/ results/
│   │   ├── announcements/ error/
│   │   └── mail/
│   └── static/css/ js/ images/
└── src/test/java/vn/edu/project/    # service, repository, MVC/security
```

`security`, `mapper`, `event`, các profile và migration là phần mở rộng phục vụ đồ án; các tầng gốc giữ nguyên tinh thần lab. Không bắt buộc có interface + Impl cho mọi service khi chỉ có một implementation. Dùng constructor injection.

## 5. Hợp đồng DTO và trách nhiệm tầng

| DTO minh họa | Trường client được gửi | Trường hệ thống tự xác định |
|---|---|---|
| `TopicCreateRequest` | tiêu đề, nội dung, đợt, bộ môn; đề xuất GVHD nếu workflow cho phép | tác giả từ principal, trạng thái nháp; danh sách GVHD chính thức do chức năng phân công quyết định |
| `TopicApprovalRequest` | quyết định, lý do | người duyệt/thời gian; kiểm tra role Trưởng khoa |
| `GroupCreateRequest` | tên nhóm, đợt | người tạo/trưởng nhóm từ principal |
| `GroupJoinRequest` | lời mời hoặc nhóm muốn tham gia | SV hành động từ principal; không nhận một `studentId` tùy ý để giả người khác |
| `TopicRegistrationRequest` | topicId; groupId có thể là route | người thao tác, group ownership, status PENDING |
| `SubmissionRequest` | file, ghi chú | người nộp, registration hợp lệ, version, timestamp, storage key |
| `EvaluationRequest` | assignmentId, điểm, nhận xét, thao tác nháp/gửi | người chấm từ principal; loại phiếu suy ra từ assignment |
| `PublishResultRequest` | danh sách kết quả đủ điều kiện hoặc cycleId, xác nhận | final score từ service, người/ngày công bố, phiên bản |
| `AccountCreateRequest` | email, mã/hồ sơ, role do Admin chọn hợp lệ | password hash do encoder; không chấp nhận hash từ UI |

Response DTO/View DTO chỉ chứa dữ liệu đúng phạm vi. `TopicSummaryView`, `GroupDetailView`, `EvaluationFormView`, `StudentResultView`, `PublicationSummaryView` có thể khác nhau dù đều dựa trên cùng entity. Không trả `passwordHash`, cấu hình SMTP, storage path hay điểm chưa được phép xem.

Quy ước xử lý form:

1. `@Controller` bind Request DTO bằng `@ModelAttribute`, `@Valid`; `BindingResult` đặt ngay sau DTO.
2. Lỗi trường trả lại form và dữ liệu lựa chọn cần thiết; không mất các giá trị hợp lệ đã nhập.
3. Service lấy actor đã xác thực, kiểm quyền đối tượng, trạng thái, hạn và ràng buộc liên bảng trong transaction.
4. Thành công dùng Post/Redirect/Get; thông báo kết quả bằng flash message.
5. REST dùng `@RequestBody` + `@Valid`, trả DTO và status phù hợp; chung service nên không có đường API bỏ qua điều kiện của form.

`@NotBlank`, `@Size`, `@Email`, `@DecimalMin`, `@DecimalMax` kiểm tra dữ liệu đơn lẻ. Quy tắc một nhóm, 1–2 GVHD, 3–5 hội đồng, không tự chấm và đủ phiếu thuộc service/DB; không chỉ đặt annotation lên DTO rồi coi là hoàn tất.

## 6. Danh mục database nền

Đây là thiết kế sơ bộ để lập ERD/data dictionary, chưa phải migration cuối. Dùng ID `BIGINT`, text `VARCHAR/TEXT`, tiền tố khóa ngoại rõ ràng; điểm `DECIMAL(5,2)`, Java `BigDecimal`; timestamp thống nhất lưu UTC và hiển thị Việt Nam. Có `created_at`, `updated_at`, trạng thái khi cần và `version` cho dữ liệu dễ cập nhật đồng thời.

| # | Bảng | Nội dung chính và ràng buộc |
|---|---|---|
| 1 | `user_account` | email/username unique, password_hash, full_name, status, timestamps |
| 2 | `role` | code unique: SYSTEM_ADMIN, FACULTY_MANAGER, DEPARTMENT_HEAD, LECTURER, STUDENT |
| 3 | `user_role` | user_id, role_id; unique cặp; không dùng role toàn hệ thống để giả quyền GVPB/HĐ |
| 4 | `student` | user_id unique, student_code unique, intake_year, trạng thái |
| 5 | `lecturer` | user_id unique, lecturer_code unique, department_id, trạng thái |
| 6 | `department` | code unique, name, head_lecturer_id nullable; trưởng bộ môn phải thuộc đúng phạm vi theo A12 |
| 7 | `registration_cycle` | name, cycle_type, cửa sổ GV/SV, reviewer_deadline, council_report_date, group_min/max, cấu hình chính sách, status |
| 8 | `topic` | cycle_id, department_id, author_id, title, description, status, max_groups, approval metadata |
| 9 | `topic_supervisor` | topic_id, lecturer_id, vai trò chính/phụ; unique cặp; 1–2 GV khi công bố |
| 10 | `student_group` | cycle_id, name, leader_student_id, status; leader phải là thành viên ACTIVE |
| 11 | `group_member` | group_id, student_id, status INVITED/ACTIVE/LEFT/REJECTED, joined_at/left_at; kiểm tra một nhóm hoạt động của SV |
| 12 | `topic_registration` | group_id, topic_id, status, người/ngày gửi/duyệt, lý do; group và topic cùng cycle |
| 13 | `submission` | registration_id, version_no, storage_key, file_name, content_type, size, submitted_by, submitted_at; unique registration/version |
| 14 | `reviewer_assignment` | registration_id, lecturer_id, status, assigned_by/at; số người và vòng theo A05 |
| 15 | `review_council` | cycle_id, name, status, lịch/phòng nếu cần; không suy ra mọi loại đều có cycle.council_report_date |
| 16 | `council_member` | council_id, lecturer_id, member_role; unique cặp; một PRESIDENT, một SECRETARY khi hoạt động |
| 17 | `council_assignment` | council_id, registration_id, report_at/location nếu theo từng nhóm, status, điểm tổng hợp HĐ, confirmed_by/at |
| 18 | `evaluation` | registration_id, evaluator_id, type REVIEWER/COUNCIL, score, comment, status, submitted_at; FK tới reviewer_assignment hoặc cặp council_assignment/council_member |
| 19 | `evaluation_result` | registration_id unique, final_score, expected/submitted score count, status, version, published_by/at; metadata xác nhận |
| 20 | `announcement` | title, content, author_id, audience_scope, department_id/cycle_id nếu cần, status, published_at |
| 21 | `email_delivery` (kỹ thuật đề xuất) | result_id, publication_version, recipient_user_id/email, status, attempts, last_error, sent_at; unique theo phiên bản công bố/người nhận |
| 22 | `audit_log` (kỹ thuật đề xuất) | actor_id, action, entity_type/id, timestamp, outcome, lý do/metadata được lọc |

Không có bảng DTO, không tạo bảng riêng cho từng loại đợt, không tạo bảng Trưởng khoa hay Nhóm trưởng. Chọn `leader_student_id` làm nguồn duy nhất cho trưởng nhóm; không đồng thời giữ `group_member.is_leader` như một nguồn độc lập gây lệch dữ liệu. Ràng buộc leader thuộc ACTIVE membership kiểm tra trong transaction (và FK bổ sung nếu mô hình cho phép).

Các bảng chỉ thêm khi yêu cầu xác nhận cần: `announcement_audience` khi một thông báo chọn nhiều role tùy ý; `file_attachment` khi có nhiều file trong một lần nộp/thông báo; bảng lịch sử sửa điểm khi bật FR36; bảng policy/criteria nếu A05 có tiêu chí động. Không tự thêm bảng chỉ để đạt số entity.

Liên kết cốt lõi:

```text
user_account 1--N user_role N--1 role
user_account 1--0..1 student / lecturer
department 1--N lecturer, topic
registration_cycle 1--N topic, student_group, review_council
topic 1--N topic_supervisor N--1 lecturer
student_group 1--N group_member N--1 student
student_group 1--N topic_registration N--1 topic
topic_registration 1--N submission, reviewer_assignment, evaluation
review_council 1--N council_member N--1 lecturer
review_council 1--N council_assignment N--1 topic_registration
topic_registration 1--0..1 evaluation_result
evaluation_result 1--N email_delivery
```

Số lượng `1--N` trong lịch sử không có nghĩa được nhiều đăng ký đang hoạt động đồng thời. DB và service phải giữ ràng buộc trạng thái.

### Ràng buộc và xử lý cạnh tranh

- Unique code/email và FK ở DB; kiểm tra trong service để trả thông báo dễ hiểu, nhưng không bỏ DB constraint.
- Khi nhận thành viên, khóa bản ghi SV và nhóm theo thứ tự nhất quán; kiểm tra mọi membership ACTIVE thuộc nhóm chưa hoàn tất; tránh hai lời mời được nhận cùng lúc.
- Khi đăng ký, khóa nhóm và đề tài, kiểm tra đăng ký hoạt động và sức chứa. Có thể giữ slot từ PENDING để tránh hứa trùng; đây là đề xuất phải ghi vào policy. Từ chối/rút thì giải phóng slot.
- Không dùng partial unique index kiểu PostgreSQL trên MySQL. Nếu cần uniqueness có điều kiện, thiết kế generated nullable key phù hợp MySQL hoặc phương án slot/transaction và kiểm thử thật trước khi chốt SQL.
- Khóa/optimistic version khi chuyển trưởng nhóm, đổi GVHD, chấm, tổng hợp và công bố. Không để thêm GVHD sau khi người đó đã có điểm hợp lệ cho đề tài mà không xử lý xung đột.
- Phiếu REVIEWER phải trỏ assignment đúng người/registration; phiếu COUNCIL phải trỏ thành viên và assignment cùng một hội đồng. Một evaluator không tạo nhiều phiếu trùng trong cùng stage/lần đánh giá.
- Kiểm tra chính sách nếu một GV vừa là GVPB vừa thành viên HĐ của cùng đề tài: chưa rõ thì không phân công chồng cho đến khi A05 xác nhận cách tính.
- Xóa bản nháp không xóa dây chuyền điểm, báo cáo hay lịch sử đã công bố. Phân công thay thế đã có điểm phải được xử lý/invalidate rõ ràng, không dùng phiếu cũ cùng phiếu mới để tăng mẫu số.

## 7. Quy ước URL và quyền

| Nhóm route | Ví dụ HTML | Service dự kiến | Phạm vi |
|---|---|---|---|
| Auth | `/login`, POST `/logout`, `/me/password` | AccountService / UserDetailsService | Bản thân |
| Quản trị | `/admin/accounts`, `/admin/roles` | AccountService | Admin |
| Hồ sơ/danh mục | `/management/students`, `/management/lecturers`, `/management/departments` | Student/Lecturer/DepartmentService | Ma trận quản lý |
| Đợt | `/cycles`, `/cycles/{id}/edit` | RegistrationCycleService | Quản lý hoặc xem đúng scope |
| Đề tài | `/topics`, `/topics/{id}`, `/topics/{id}/submit`, `/topics/{id}/approve` | TopicService | Tác giả/quản lý/công bố |
| Nhóm | `/groups/mine`, `/groups/{id}/members`, `/groups/{id}/leader` | StudentGroupService | Thành viên/trưởng nhóm |
| Đăng ký | `/groups/{id}/registrations`, `/registrations/{id}/approve` | TopicRegistrationService | Trưởng nhóm/người duyệt |
| Báo cáo | `/registrations/{id}/submissions`, `/submissions/{id}/download` | SubmissionService | Kiểm quyền đối tượng |
| Phân công | `/reviewer-assignments`, `/councils`, `/councils/{id}/assignments` | Assignment/CouncilService | Khoa và người được giao |
| Chấm | `/evaluations/mine`, `/evaluations/{id}/submit` | EvaluationService | Đúng người chấm |
| Tổng hợp | `/council-assignments/{id}/confirm` | CouncilResultService | Chủ tịch đúng HĐ |
| Công bố | `/cycles/{id}/results`, `/results/publish`, `/email-deliveries` | ResultPublicationService | Trưởng khoa |
| Tra cứu | `/me/results?cycleId=...` | ResultQueryService | Membership + registration + published |
| Thông báo | `/announcements`, `/announcements/{id}/publish` | AnnouncementService | Đối tượng nhận/người quản lý |
| REST | `/api/v1/topics`, `/api/v1/registrations` | Cùng service như HTML | Cùng quyền và ràng buộc |

Tất cả hành động thay đổi dùng POST/PUT/PATCH/DELETE phù hợp; GET chỉ đọc. Route trong plan là hợp đồng đề xuất, báo cáo cuối phải ghi route/file thực tế chứ không sao chép các tên dự kiến nếu code đã đổi.

## 8. Security và lỗi

- `SecurityFilterChain`: chỉ login/tài nguyên tĩnh và trang công khai được permit; route nghiệp vụ cần xác thực; mặc định không cho route mới vô tình công khai.
- `@EnableMethodSecurity`, `@PreAuthorize` cho vai trò và policy bean; đặt kiểm tra đối tượng/nghiệp vụ trong service dùng chung để form/API không lệch.
- Quyền hiệu lực = role/phân công hợp lệ + đúng đối tượng/phạm vi + đúng trạng thái + đúng thời gian + không có xung đột bị cấm. Role cao không bỏ qua điều kiện cấm tự chấm.
- Giữ CSRF cho form, logout và API dùng session/cookie từ trình duyệt. Lab 4 miễn `/api/v1/**` để test Postman; không sao chép ngoại lệ đó cho mọi API cùng session. Nếu cần HTTP Basic demo thì tách cấu hình/test rõ ràng; không coi Basic tự động miễn rủi ro CSRF.
- `sec:authorize` giúp ẩn nút; không thay thế kiểm tra phía server. Với quyền động, service cấp `canEdit`, `canSubmit`, `canGrade` trong View DTO, backend vẫn kiểm lại khi POST.
- Tài khoản bị khóa hoặc đổi quyền phải có hiệu lực với phiên đang tồn tại, không chỉ ở lần đăng nhập sau.
- HTML có trang 403/404 và thông báo lỗi nghiệp vụ; API trả 400 validation, 401 chưa xác thực, 403 không quyền, 404 không tồn tại/không được tiết lộ, 409 xung đột trạng thái/đồng thời. Không trả stack trace hoặc SQL ra client.
- DTO cho sửa profile không chứa role; điểm không lấy evaluator từ hidden input; download file không tin đường dẫn client gửi.
- Hash mật khẩu bằng BCrypt; secret qua biến môi trường; HTTPS môi trường công khai. Log không ghi password/token/cookie, hash, nội dung báo cáo hay toàn bộ DTO một cách mặc định.

## 9. Email, file và audit

### Công bố và email

Trong transaction công bố: xác minh đủ phiếu, lưu kết quả/bản công bố, tạo các bản ghi email cần gửi cho từng SV. Sau commit, listener/worker gọi `EmailService` qua bean khác có `@Async`; không tự gọi method async trong cùng bean và tưởng đã chạy nền.

Payload email là DTO chứa kết quả đã chốt và recipient, không mang entity lazy sang thread khác. Dùng Thymeleaf template mail với tên SV, đợt, đề tài, điểm/nhận xét được phép xem và link đăng nhập. Gửi riêng từng SV, không lộ danh sách email trong To/CC chung.

`email_delivery` lưu `PENDING/SENDING/SENT/FAILED`, số lần thử, thời điểm và lỗi đã làm sạch. Unique theo phiên bản công bố/recipient ngăn bấm công bố lặp tạo mail lặp thông thường. Nếu SMTP đã nhận nhưng ứng dụng mất kết nối trước khi ghi SENT, vẫn có khả năng gửi trùng khi retry; không tuyên bố exactly-once. Nếu dùng nhà cung cấp có idempotency key thì tận dụng.

Local dùng Mailpit SMTP 1025/UI 8025 theo các bước thực tế Lab 6. Lab có chỗ gọi tên Mailtrap nhưng phần cấu hình là Mailpit. Mailpit là hộp thư kiểm thử, không phải bằng chứng SV nhận email Internet thật.

### File báo cáo

File lưu ở vùng storage riêng, database lưu metadata và storage key. Local dùng thư mục cấu hình ngoài `static/`; production dùng storage bền vững phù hợp host. Kiểm tra extension/MIME/size, tạo tên lưu ngẫu nhiên, không thực thi file hoặc giải nén ZIP trên server. Mọi tải file đi qua kiểm quyền hoặc URL ký có hạn sau kiểm quyền. Phiên bản báo cáo đang chấm cần được chốt để người nộp không đổi nội dung dưới phiếu đã gửi.

### Audit và AOP

Theo Lab 5, aspect ghi thao tác service, actor, object id và thời gian; log lỗi không nuốt exception. `@Around` đo cả nhánh lỗi bằng finally. Audit học thuật thành công phải phản ánh transaction đã commit; `@AfterReturning` đơn thuần chưa bảo đảm điều đó nếu commit còn có thể thất bại. Có thể dùng event sau commit hoặc audit cùng transaction; ghi thất bại ở kênh riêng. Không dùng AOP logging để thay thế logic phân quyền hay tính điểm.

## 10. Deployment thực tế

Hướng đề xuất từ D1: Maven build JAR → Docker build/runtime cùng JDK → GitHub → web host → MySQL cloud → HTTPS. Đây là kế hoạch, chưa tạo tài khoản cloud hay deploy.

Checklist triển khai:

1. Chạy test trước build bàn giao. `-DskipTests` trong ví dụ lab không phải lý do bỏ test cả dự án.
2. Giữ Maven Wrapper và Dockerfile trong repo, không commit `.env`, password, SMTP credentials, file upload hay dữ liệu cá nhân thật.
3. Config datasource, `PORT`, SMTP/provider, storage và timezone qua môi trường; tách dev/test/prod.
4. Kiểm tra JDK compile và runtime tương ứng; pin image/dependency đã kiểm thử.
5. Sửa ví dụ JDBC TLS: Connector/J dùng `sslMode=REQUIRED` hoặc tốt hơn `VERIFY_IDENTITY` với chứng chỉ phù hợp; không sao chép `sslmode=require` từ D1. Cơ sở: [MySQL Connector/J TLS](https://dev.mysql.com/doc/connector-j/en/connector-j-reference-using-ssl.html).
6. Kiểm tra gói hosting thực tế. Ảnh trang 6 D1 là gói dùng trial credit dù văn bản nói free; không giả định tài khoản nào cũng có cùng gói miễn phí. Không tạo tài nguyên trả phí theo kế hoạch này.
7. Render free hiện chặn outbound SMTP cổng 25/465/587 và filesystem không bền vững; kiểm tra [giới hạn chính thức](https://render.com/docs/free). Thử mail/storage ngay G1/G5, không chờ trước hôm nộp.
8. Nếu host không gửi SMTP được: chọn host phù hợp với JavaMailSender, hoặc dùng `EmailSender` abstraction với SMTP adapter cho Lab 6 và HTTP provider adapter cho cloud, ghi rõ đây là phần mở rộng. Không báo gửi thành công khi chỉ log ra console.
9. Xác minh migration, seed demo, upload/download sau redeploy, login từng role, công bố/email và request sai quyền trên URL thật.
10. Chuẩn bị demo local dự phòng, xuất backup DB/danh sách file và hướng dẫn khôi phục. Chỉ dọn tài nguyên cloud sau khi nhóm xác nhận đã chấm xong và đã giữ dữ liệu cần thiết.

Gói free, port SMTP, dung lượng và hỗ trợ dịch vụ thay đổi theo thời điểm; kiểm lại khi triển khai. Kế hoạch không cam kết một dịch vụ cụ thể miễn phí vĩnh viễn.
