# Quyết định triển khai

## Baseline đã chạy

- Java **21**, Spring Boot **4.1.1**, Maven Wrapper **3.9.16**, MySQL **8.x**.
- Plan ban đầu ghi Java 25 dựa trên baseline lab. Bản chạy này dùng Java 21 đã có trên máy; kiến trúc MVC/DTO/Service/JPA/Security/AOP/Async giữ nguyên. Khi team thống nhất Java 25, đổi `java.version`, JDK chạy Maven và chạy lại toàn bộ test trước khi merge.
- Frontend là Thymeleaf server-rendered HTML + CSS/JS thuần. Backend không nằm trong template: controller nhận HTTP, service xử lý nghiệp vụ, repository truy cập MySQL, DTO là hợp đồng vào/ra.
- 22 bảng là các entity riêng, **không phải 4 heading = 4 bảng**. Các nhóm chủ đề tài liệu không tạo ranh giới database. SQL migration chứa khóa ngoại liên kết xuyên nhóm.
- Số lượng bảng 22 bao gồm `email_delivery` và `audit_log`; `announcement.audience` chứa một đối tượng hoặc ALL; chưa cần bảng audience/file_attachment riêng vì mỗi lần nộp chứa một file (ZIP có thể gom nhiều tài liệu).

## Quyền và chính sách áp dụng

- Admin không mặc nhiên xem/sửa điểm. Trưởng khoa có quyền học thuật. Trưởng bộ môn xem phạm vi bộ môn, không mặc nhiên đọc điểm/báo cáo nếu không có phân công khác.
- Hồ sơ SV/GV gắn 1–1 với tài khoản. Admin tạo tài khoản/hồ sơ và cấp role; Khoa chỉnh hồ sơ GV/SV đã được cấp tài khoản. Không cho Khoa tự nâng quyền hoặc đặt lại mật khẩu.
- Nhóm có 3–5 thành viên khi đăng ký, được tạo nháp từ 1 người. Một SV chỉ có một nhóm đang thực hiện trên toàn hệ thống; nhóm hoàn tất vẫn giữ lịch sử.
- Khoa duyệt đăng ký nhóm; trưởng nhóm đăng ký/nộp file. Chưa có workflow GVHD phê duyệt thay Khoa.
- Mỗi đề tài mặc định 1 nhóm, cấu hình được số chỗ. GVPB/hội đồng không trùng GVHD, đồng GVHD; cùng một GV không đồng thời là GVPB và thành viên hội đồng cho cùng hồ sơ.
- Từng loại đợt có số GVPB và yêu cầu hội đồng cấu hình được. Dữ liệu demo KLTN dùng 1 GVPB + hội đồng 3 người; loại khác demo dùng 1 GVPB. Đây là **chính sách demo có thể chỉnh trước khi phát sinh nhóm**, không khẳng định là quy định của thầy.
- Điểm cuối = trung bình cộng **từng phiếu** phản biện + từng thành viên HĐ hợp lệ, 0–10, làm tròn 2 số thập phân; không lấy trung bình của các trung bình. Không tự đặt ngưỡng đậu/rớt.
- Phiếu DRAFT được sửa; SUBMITTED khóa sửa. Công bố cũng khóa nghiệp vụ bằng kết quả PUBLISHED, không cần thêm trạng thái LOCKED riêng cho phiếu. Bảng sẵn sàng công bố được tính từ dữ liệu hiện tại, không lưu READY dư thừa.
- Thu hồi yêu cầu lý do, lưu snapshot điểm, mở lại phiếu chính chủ và hủy email chưa gửi của bản cũ. Email đã gửi thật không thể thu hồi khỏi hộp thư người nhận; website luôn kiểm trạng thái hiện hành.
- Lưu trữ nhóm là bước cuối sau công bố. Không có nút tùy tiện mở lại nhóm đã lưu trữ và thay lịch sử.

## Toàn vẹn và bảo mật

- Migration có PK/FK/unique, `@Version`; transaction cho các luồng ghi.
- Khóa bi quan nhóm/đề tài khi đăng ký; isolation READ_COMMITTED để kiểm tra chỗ trống sau khi nhận khóa không dùng snapshot cũ của MySQL.
- Các DTO output không chứa password hash. Không bind request lên entity, không nhận evaluatorId để chấm thay người khác.
- CSRF bật cho POST/PUT/DELETE dùng phiên. Cần đăng nhập, lấy token từ `/api/v1/csrf` nếu gọi REST bằng client.
- File đổi sang tên UUID, ngoài static. Chỉ nhận PDF/DOCX/ZIP tối đa 20 MB; kiểm phần đầu file và download có quyền. **Đây không phải quét virus**; không giải nén hay thực thi upload.
- Mail outbox được ghi cùng transaction công bố; worker chỉ thấy dữ liệu đã commit; xử lý async, có trạng thái PENDING/PREVIEW/SENT/FAILED/CANCELLED. Khóa cùng hồ sơ để tuần tự hóa với thu hồi. SMTP không bảo đảm exactly-once khi tiến trình chết sau khi server mail nhận nhưng trước khi DB commit; có thể cần idempotency/dedup nếu triển khai thật.
- Account bị khóa hoặc securityVersion đổi sẽ không giữ quyền ở request tiếp theo. Phiên cũ mất hiệu lực khi đổi/reset mật khẩu. Quyền được nạp lại từ database mỗi request.
- Log AOP chỉ có tên method/trạng thái return/thời gian. Audit nghiệp vụ lưu cùng transaction; không coi dòng log return là bằng chứng commit.

## Phạm vi cần xác nhận / nâng cấp trước triển khai thật

- Các giả định A01–A12 trong plan vẫn cần thầy xác nhận (đặc biệt ai duyệt nhóm, tập điểm thành phần, giới hạn nhóm, quyền TBM).
- UI có tìm kiếm/lọc/phân trang tại service sau khi lọc quyền, không phải chỉ ẩn ở HTML. Repository hiện tối ưu cho đồ án nhỏ; chưa benchmark 1.000 đề tài/200 nhóm, chưa chuyển tất cả tìm kiếm sang truy vấn phân trang SQL. Không tuyên bố đạt mục tiêu hiệu năng NFR07.
- Lịch hội đồng kiểm tra **cùng thời điểm bắt đầu**, chưa có độ dài buổi để phát hiện chồng lấn khoảng thời gian. Chưa có thuật toán tự xếp lịch.
- Sửa hội đồng đã có nhóm, sửa nội dung đề tài đã công bố hoặc thay GVHD khi đã duyệt nhóm bị chặn. Các ngoại lệ phải thiết kế riêng, không sửa trực tiếp database khi demo.
- Chưa có chat, SSO, chống đạo văn, điểm cá nhân, quét virus, import Excel hàng loạt hay tự đăng ký tài khoản vì không nằm trong luồng lõi đã chọn.
- Chưa gửi SMTP qua nhà cung cấp thật; mặc định chỉ preview local. Chưa hosting/tên miền theo yêu cầu người dùng. HTTPS, bí mật môi trường, backup tự động, phục hồi, rate limit login và kiểm thử tải cần làm trước public Internet.

Không chỉnh migration V1 khi database đã dùng. Mọi thay đổi cấu trúc mới phải thêm V2/V3…; `ddl-auto=validate` để phát hiện entity/schema lệch nhau.
