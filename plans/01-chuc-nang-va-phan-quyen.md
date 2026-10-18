# Chức năng và phân quyền hệ thống

## 1. Mục tiêu và phạm vi

Hệ thống phục vụ Khoa CNTT quản lý đề tài môn học, NCKH, TLCN và KLTN từ mở đợt, nhận đề tài, duyệt/công bố, đăng ký nhóm, phân công hướng dẫn/phản biện, nộp báo cáo, chấm điểm, tổng hợp đến công bố và gửi email. Có quản lý tài khoản, GV, SV, bộ môn và thông báo. Nguồn: S1 trang 1, S2 trang 2–3, S3 mục 2.

Một `registration_cycle` có `cycle_type` là `COURSE`, `RESEARCH`, `SPECIALIZED_PROJECT`, `GRADUATION_THESIS`. Bốn loại dùng chung bộ bảng và service; khác nhau ở cấu hình thời gian, quy tắc nhóm và quy trình đánh giá đã được duyệt.

Không đưa vào P0: chat thời gian thực, mobile app, thanh toán, chống đạo văn, microservice, tự động xếp lịch tối ưu, SSO, AI chấm điểm. Không tự bổ sung trường điểm cá nhân hay điểm có trọng số khi đề bài chỉ yêu cầu điểm đề tài và trung bình cộng.

## 2. Actor và role nền

| Actor | Cách xác định trong hệ thống | Phạm vi |
|---|---|---|
| Quản trị hệ thống | `SYSTEM_ADMIN` | Tài khoản, cấp role, cấu hình kỹ thuật; không mặc nhiên chấm/sửa điểm |
| Trưởng khoa | `FACULTY_MANAGER` | Đợt đăng ký, phê duyệt đề tài, phân công GVHD/GVPB, hội đồng, công bố kết quả trong khoa |
| Trưởng bộ môn | `DEPARTMENT_HEAD` + bộ môn phụ trách | Theo dõi, rà soát, đề xuất xử lý dữ liệu của bộ môn; quyền nghiệp vụ cụ thể là đề xuất cần xác nhận |
| Giảng viên | `LECTURER` + hồ sơ giảng viên | Đề xuất đề tài của mình; các quyền khác phụ thuộc phân công |
| GVHD / đồng GVHD | Có trong `topic_supervisor` | Xem nhóm/báo cáo đề tài được hướng dẫn; không chấm chính đề tài đó |
| GVPB | Có `reviewer_assignment` còn hiệu lực | Chỉ đánh giá nhóm/đề tài được phân công, đúng hạn |
| Thành viên hội đồng | Có `council_member`, đề tài được giao hội đồng | Xem tài liệu và nhập điểm của chính mình cho đề tài được phân công, nếu không xung đột GVHD |
| Chủ tịch hội đồng | `council_member.member_role=PRESIDENT` | Quyền thành viên + kiểm tra đủ phiếu, tổng hợp/xác nhận kết quả hội đồng mình phụ trách |
| Thư ký hội đồng | `council_member.member_role=SECRETARY` | Quyền thành viên + theo dõi hồ sơ, tình trạng nộp phiếu; không nhập thay điểm người khác |
| Sinh viên | `STUDENT` + hồ sơ sinh viên | Đề tài đã công bố, nhóm mình, báo cáo nhóm mình, kết quả đã công bố trong đợt mình đăng ký |
| Nhóm trưởng | Thành viên nhóm có vai trò trưởng nhóm | Thay nhóm đăng ký và nộp báo cáo; quyền đăng ký là đề xuất, quyền nộp báo cáo do đề bài quy định |
| Chưa đăng nhập | Không có phiên đăng nhập | Chỉ trang đăng nhập, tài nguyên công khai; thông báo công khai nếu Khoa bật |

Một tài khoản có thể đồng thời có `LECTURER` và `DEPARTMENT_HEAD` hoặc `FACULTY_MANAGER`. Không áp dụng cây quyền mặc định kiểu Admin > Trưởng khoa > GV > SV. Quyền quản lý tài khoản không đồng nghĩa quyền học thuật. Ràng buộc cấm tự chấm đề tài được áp dụng kể cả tài khoản có nhiều role.

Use case chi tiết phải thể hiện đủ actor theo rubric. GVHD, GVPB, Chủ tịch, Thư ký và Thành viên hội đồng có thể là chuyên biệt của Giảng viên; Nhóm trưởng là chuyên biệt của Sinh viên. Hội đồng là nhóm người trong miền nghiệp vụ, người đăng nhập và thao tác thực tế là từng giảng viên. Không tạo tài khoản dùng chung cho hội đồng.

## 3. Ma trận quyền quản lý

`—` nghĩa là không cấp quyền đó. Mọi quyền bên dưới còn phải kiểm tra trạng thái, thời hạn và phạm vi dữ liệu. Các quyền phân bổ chi tiết mà S1/S2 chưa chỉ định là thiết kế đề xuất.

| Thao tác | Admin | Trưởng khoa | Trưởng bộ môn | Giảng viên | Sinh viên |
|---|---|---|---|---|---|
| Đăng nhập, đăng xuất, đổi mật khẩu | Bản thân | Bản thân | Bản thân | Bản thân | Bản thân |
| Xem/sửa thông tin liên hệ cá nhân | Bản thân | Bản thân | Bản thân | Bản thân | Bản thân |
| Tạo/khóa/mở tài khoản, đặt lại mật khẩu | Có | — | — | — | — |
| Cấp/thu hồi role và phạm vi bộ môn | Có | Đề nghị | — | — | — |
| CRUD hồ sơ GV/SV | Có | Có, trong khoa | Xem GV thuộc bộ môn | Hồ sơ mình | Hồ sơ mình |
| CRUD bộ môn | Có | Có | Xem bộ môn mình | Xem danh mục | Xem danh mục |
| CRUD đợt đăng ký, cấu hình thời gian | — | Có | Xem | Xem đợt liên quan | Xem đợt công bố |
| Tạo/sửa/xóa bản nháp đề tài | — | Có | Rà soát bộ môn, không sửa thay tác giả | Bản nháp mình tạo, đúng hạn | — |
| Gửi đề tài xin duyệt | — | Có nếu tạo hộ có ghi nhận | Đề xuất ý kiến | Đề tài mình | — |
| Duyệt/từ chối/công bố đề tài | — | Có | Đề xuất, không phê duyệt cuối | Xem trạng thái đề tài mình | Chỉ xem đã công bố |
| Phân công/đổi 1–2 GVHD | — | Có | Đề xuất trong bộ môn | Xem phân công mình | Xem GVHD của đề tài |
| Xem nhóm và đăng ký | — | Trong khoa | Thuộc đề tài bộ môn | Nhóm mình hướng dẫn/được phân công | Nhóm mình |
| Duyệt/từ chối đăng ký nhóm | — | Có, tạm đề xuất A03 | Đề xuất ý kiến | Xem/ý kiến, chưa cấp quyền duyệt | — |
| Phân công GVPB | — | Có | Đề xuất | Xem phân công mình | Xem lịch/thông tin được công bố |
| Thành lập hội đồng, phân công, xếp lịch | — | Có | Đề xuất | Xem hội đồng mình | Xem lịch của nhóm |
| Công bố/thu hồi kết quả | — | Có; thu hồi phải ghi lý do | — | — | — |
| CRUD thông báo chính thức | Vận hành theo ủy quyền* | Có | Chỉ bản nháp bộ môn* | Đọc đúng đối tượng | Đọc đúng đối tượng |
| Xem tình trạng gửi mail công bố | Theo dõi kỹ thuật, che nội dung điểm | Trong khoa | — | — | Nhận email của mình |
| Gửi lại email lỗi | Thực hiện kỹ thuật với bản công bố hiện hành | Có | — | — | — |
| Nhật ký và thống kê | Nhật ký kỹ thuật | Nghiệp vụ khoa | Thống kê bộ môn | Thống kê được giao | Tổng quan cá nhân |

\* Đề xuất: Trưởng khoa chịu trách nhiệm nội dung thông báo; Admin không tự mang quyền duyệt học thuật. Nếu cần đăng thông báo nhà trường, cấp quyền đăng thông báo rõ ràng cho người phụ trách. Không xây một workflow phê duyệt thông báo nhiều tầng nếu thầy không yêu cầu.

Xóa là xóa thật đối với bản nháp/bản ghi chưa được tham chiếu. Tài khoản đã phát sinh dữ liệu được khóa; dữ liệu học thuật có liên kết được ngừng sử dụng/lưu trữ hoặc từ chối xóa kèm lý do. Cần demo CRUD cả nhánh xóa hợp lệ và nhánh không được xóa, không đặt nút xóa giả.

## 4. Quyền theo nhóm và phân công đánh giá

| Thao tác | Thành viên SV | Nhóm trưởng | GVHD | GVPB | Thành viên HĐ | Thư ký | Chủ tịch |
|---|---|---|---|---|---|---|---|
| Xem nhóm và thành viên | Nhóm mình | Nhóm mình | Nhóm HD | Nhóm được giao | Nhóm của HĐ | Nhóm của HĐ | Nhóm của HĐ |
| Chấp nhận lời mời vào nhóm | Bản thân, chưa có nhóm hoạt động | Bản thân khi phù hợp | — | — | — | — | — |
| Mời thành viên/chuyển trưởng nhóm | — | Nhóm mình trước khi khóa | — | — | — | — | — |
| Gửi đăng ký đề tài | — | Nhóm đủ điều kiện | — | — | — | — | — |
| Nộp phiên bản báo cáo | — | Nhóm được duyệt, trong hạn | — | — | — | — | — |
| Xem/tải báo cáo | Nhóm mình | Nhóm mình | Nhóm HD | Đúng phân công | Đúng HĐ/phân công | Đúng HĐ/phân công | Đúng HĐ/phân công |
| Nhập/sửa điểm nháp | — | — | Không cho đề tài mình HD | Phiếu mình | Phiếu mình | Phiếu mình | Phiếu mình |
| Gửi điểm chính thức | — | — | Không cho đề tài mình HD | Phiếu mình, đúng hạn | Phiếu mình, đúng hạn | Phiếu mình, đúng hạn | Phiếu mình, đúng hạn |
| Xem phiếu chưa công bố của người khác | — | — | — | — | — | Tình trạng nộp; nội dung nếu được ủy quyền rõ | Phiếu thuộc HĐ để tổng hợp |
| Tổng hợp/xác nhận điểm hội đồng | — | — | — | — | — | Chuẩn bị hồ sơ, không xác nhận thay | HĐ mình, đủ phiếu |
| Xem kết quả cuối đã công bố | Nhóm/đợt mình | Nhóm/đợt mình | Nhóm HD | Nhóm được giao | Nhóm của HĐ | Nhóm của HĐ | Nhóm của HĐ |

GVHD có thể đồng thời là GVPB của một đề tài khác. Nếu GVHD có mặt trong hội đồng, phải từ chối phân công đề tài của họ vào hội đồng đó hoặc thay thành viên để giữ đủ 3–5 người hợp lệ; không chỉ ẩn nút chấm rồi tính trung bình thiếu phiếu.

## 5. Danh mục chức năng cần triển khai

Mỗi mã FR phải được liên kết tới use case, màn hình/route, service, bảng, test và minh chứng thực tế trong file báo cáo nhóm.

| Mã | Chức năng và hành vi cần có | Quyền / điều kiện nghiệm thu |
|---|---|---|
| FR01 | Login/logout, chuyển dashboard theo quyền | Mọi tài khoản hoạt động; tài khoản khóa bị từ chối; logout hủy phiên |
| FR02 | Đổi mật khẩu, xem/sửa liên hệ cá nhân | Chủ tài khoản; không được tự sửa role, MSSV, bộ môn hay trạng thái |
| FR03 | CRUD tài khoản, khóa/mở, cấp role, reset mật khẩu | Admin; mật khẩu BCrypt; không trả hash ra UI/API; khóa phải vô hiệu hóa quyền đang dùng |
| FR04 | CRUD GV | Admin/Trưởng khoa; mã GV duy nhất, bộ môn hợp lệ; bảo toàn phân công đã có |
| FR05 | CRUD SV | Admin/Trưởng khoa; MSSV duy nhất, email dùng nhận kết quả; không xóa lịch sử tham gia |
| FR06 | CRUD bộ môn và chỉ định Trưởng bộ môn | Admin/Trưởng khoa; tách chỉ định chuyên môn với thao tác cấp role của Admin |
| FR07 | CRUD đợt: tên, loại, lịch GV/SV, hạn GVPB, ngày hội đồng | Trưởng khoa; đủ trường và validation chéo theo loại |
| FR08 | Hiển thị và kiểm soát 2 giai đoạn đăng ký | Không nhận SV khi chưa tới GĐ2 hoặc đề tài chưa công bố; chặn cả request gửi trực tiếp |
| FR09 | Tạo/sửa/xóa đề tài nháp; nội dung và bộ môn | GV tác giả/Trưởng khoa; không được xóa đề tài đã có đăng ký |
| FR10 | Gửi đề tài, trả sửa/từ chối, phê duyệt | Trưởng khoa duyệt; lưu người/thời gian/lý do; chưa duyệt chưa hiện cho SV |
| FR11 | Công bố danh sách và tìm kiếm/lọc đề tài | Trưởng khoa công bố; SV lọc theo đợt, loại, bộ môn, GVHD, từ khóa |
| FR12 | Phân công 1–2 GVHD, đổi phân công có kiểm tra xung đột | Trưởng khoa; không trùng GV; không tạo xung đột với chấm điểm đã có |
| FR13 | Trưởng bộ môn rà soát/theo dõi đề tài thuộc bộ môn | Vai trò rubric yêu cầu; quyền cụ thể A04 cần thầy xác nhận |
| FR14 | Tạo nhóm nháp, xem nhóm và tình trạng đủ thành viên | SV chưa có nhóm hoạt động; người tạo là trưởng nhóm ban đầu |
| FR15 | Mời/tham gia nhóm có xác nhận, rời nhóm trước khóa | SV tự chấp nhận; không tự thêm người vào nhóm đang hoạt động khác |
| FR16 | Chuyển nhóm trưởng, kiểm tra số lượng thành viên | Một trưởng nhóm; chuyển trong transaction; không cho trưởng nhóm rời mà chưa chuyển |
| FR17 | Nhóm trưởng đăng ký đề tài đã công bố | Đúng đợt, đúng thời gian, đủ SV, chưa có đăng ký hoạt động, đề tài còn chỗ |
| FR18 | Duyệt/từ chối đăng ký; xem trạng thái/lý do | Người duyệt tạm là Trưởng khoa A03; không cho hai đăng ký được duyệt vi phạm quy tắc |
| FR19 | Rút đăng ký chờ duyệt và đăng ký lại sau từ chối | Đề xuất UX; chỉ trong hạn, một đăng ký hoạt động; không tự hủy đăng ký đã duyệt |
| FR20 | Khóa nhóm sau được duyệt; theo dõi tiến độ tổng quan | Trưởng khoa/GVHD xem đúng nhóm; thay đổi ngoại lệ phải có lý do và kiểm tra lại |
| FR21 | Nộp báo cáo, lưu phiên bản, người nộp, thời gian | Chỉ nhóm trưởng; đăng ký đã duyệt; file hợp lệ; phiên bản cũ vẫn truy vết được |
| FR22 | Xem/tải đúng file báo cáo được phép | Chủ nhóm, GVHD, GVPB, thành viên HĐ hợp lệ, Trưởng khoa; không dùng URL file công khai |
| FR23 | Phân công/thay GVPB, danh sách cần chấm | Trưởng khoa; loại mọi GVHD của đề tài; kiểm tra hạn và phiếu đã nộp |
| FR24 | GVPB lưu nháp/gửi điểm, nhận xét | Chính GVPB được giao; không giả `evaluatorId`; hạn GVPB theo đợt |
| FR25 | Tạo hội đồng 3–5 GV và phân vai | Trưởng khoa; đúng một chủ tịch và một thư ký, hai người khác nhau, không trùng thành viên |
| FR26 | Giao đề tài/nhóm cho hội đồng, lịch/phòng báo cáo | Trưởng khoa; cùng đợt; không xung đột GVHD; kiểm tra trùng lịch nếu quản lý giờ/phòng |
| FR27 | Hiển thị lịch báo cáo | SV chỉ nhóm mình; GV chỉ phân công mình; quản lý theo phạm vi |
| FR28 | Thành viên hội đồng lưu/gửi điểm và nhận xét cá nhân | Mỗi người một phiếu cho lần chấm; không tự chấm đề tài HD; không ghi đè phiếu người khác |
| FR29 | Chủ tịch xem đủ phiếu, tổng hợp/xác nhận | Đúng HĐ; chỉ tổng hợp phiếu hợp lệ đã gửi; chưa đủ thì chưa xác nhận |
| FR30 | Tính điểm cuối bằng trung bình cộng | Hệ thống; tập điểm thành phần theo A05; không lấy trung bình của các trung bình một cách ngầm định |
| FR31 | Xem bảng kết quả chờ công bố và lỗi thiếu dữ liệu | Trưởng khoa; kiểm tra đã hoàn tất đánh giá, không công bố nhầm bản nháp |
| FR32 | Công bố kết quả theo đợt/danh sách đủ điều kiện | Trưởng khoa; cập nhật trạng thái, người/ngày công bố; thao tác lặp không sinh bản công bố trùng |
| FR33 | Gửi email riêng từng SV sau công bố | Hệ thống; dùng danh sách thành viên chính thức; không gửi duy nhất cho trưởng nhóm |
| FR34 | Theo dõi mail thành công/lỗi và gửi lại | Trưởng khoa/Admin đúng phạm vi; lỗi SMTP không mất kết quả đã công bố |
| FR35 | SV tra cứu điểm theo đợt đã đăng ký | Chỉ kết quả đã công bố của đề tài mình; đổi URL/query/id không thấy nhóm/đợt khác |
| FR36 | Xử lý sửa điểm/thu hồi/tái công bố có kiểm soát | Đề xuất vận hành P1; lưu lý do/phiên bản; người chấm sửa phiếu mình sau mở lại, không Admin sửa điểm hộ |
| FR37 | CRUD thông báo chính thức | Quyền theo ma trận; nháp/đã đăng/lưu trữ; tiêu đề, nội dung, người/ngày đăng |
| FR38 | Đọc danh sách/chi tiết thông báo và đối tượng nhận | Người dùng thuộc đối tượng; bản nháp không lộ qua link trực tiếp |
| FR39 | Dashboard đúng vai trò | Khoa: các đợt/thiếu phân công/thiếu điểm; TBM: bộ môn; GV: việc được giao; SV: nhóm/đề tài/hạn/kết quả |
| FR40 | Phân trang, tìm kiếm cơ bản, sắp xếp danh sách | Theo cùng scope bảo mật với trang chi tiết; không query toàn bộ rồi chỉ ẩn trên UI |
| FR41 | REST API chọn lọc dùng chung nghiệp vụ với MVC | Danh sách/chi tiết đề tài, CRUD đề tài và một luồng đăng ký; `/api/v1`; áp dụng cùng DTO/quyền/ràng buộc |
| FR42 | AOP ghi thao tác, lỗi, thời gian thực thi | Theo Lab 5; không ghi mật khẩu/token/nội dung file; phân biệt thành công trả về với transaction commit |
| FR43 | Triển khai và hướng dẫn chạy lại | JAR/Docker, config môi trường, DB, mail, lưu file bền vững; kiểm thử trên môi trường thực |

FR01–FR35, FR37–FR38 là phạm vi nghiệp vụ chính. FR39–FR40 giúp dùng được các chức năng đó; FR41–FR42 thực hiện kiến thức lab; FR36 là bổ sung có kiểm soát, không chặn hoàn thành luồng công bố lần đầu. FR43 là mục tiêu dự án để đáp ứng mức rubric tốt dù danh mục nộp nói hosting không bắt buộc.

## 6. Quy tắc và vòng đời

### Đợt và đề tài

Luồng tổng quan: Tạo đợt → GĐ1 GV gửi đề tài → Khoa duyệt/công bố → GĐ2 nhóm SV đăng ký → chấp thuận → thực hiện/nộp → phản biện/hội đồng → tổng hợp → Khoa công bố → email và SV tra cứu.

- Tên đợt không rỗng; thời gian bắt đầu nhỏ hơn kết thúc; cửa sổ GV không chồng sang cửa sổ SV theo thiết kế hai giai đoạn.
- Hạn nộp điểm GVPB chỉ thiết lập khi loại TLCN/KLTN; ngày báo cáo hội đồng chỉ thiết lập khi KLTN, đúng yêu cầu S1. Kiểm tra ngày báo cáo sau giai đoạn đăng ký; quan hệ hạn GVPB và ngày hội đồng cần xác nhận.
- Server quyết định có mở đăng ký dựa trên đồng hồ và trạng thái, không tin thời gian trình duyệt. Quy ước đề xuất cửa sổ `[start, end)` và hiển thị múi giờ Việt Nam.
- Đợt có trạng thái quản trị `DRAFT`, `ACTIVE`, `CLOSED`, `ARCHIVED`; pha hiện tại tính từ các mốc và điều kiện công bố. Không dùng duy nhất một status để bỏ qua các cửa sổ thời gian.
- Đề tài: `DRAFT → SUBMITTED → APPROVED → PUBLISHED`; nhánh `RETURNED` để sửa hoặc `REJECTED`. SV chỉ nhìn thấy `PUBLISHED`.
- Mỗi đề tài thuộc một bộ môn và một đợt; có 1–2 GVHD trước khi công bố. Bản nháp có thể chưa đủ GVHD, nhưng không được công bố thiếu người.
- Sửa nội dung quan trọng sau công bố phải đưa về quy trình kiểm tra, không đổi lặng lẽ đề tài đã có nhóm thực hiện.

### Nhóm và đăng ký

- Nhóm nháp được tạo với một người để có thể mời thêm. Chỉ nhóm đủ số lượng theo chính sách đã chốt mới được đăng ký. Không bắt đủ 3 người ngay ở thao tác tạo nhóm.
- Đúng một trưởng nhóm thuộc danh sách thành viên hoạt động. Các lời mời chưa chấp nhận không tính vào số lượng.
- Không cho SV ở hai nhóm đang thực hiện đồng thời theo mặc định bảo thủ A02; nhóm lịch sử không bị xóa. Không tự coi quy tắc chỉ có hiệu lực trong một đợt nếu chưa hỏi thầy.
- Một nhóm chỉ có một đăng ký hoạt động (`PENDING` hoặc `APPROVED`); các đăng ký bị từ chối/rút được giữ lịch sử. Sau khi đã duyệt, không cho đổi đề tài theo đường CRUD thông thường.
- Đăng ký: `PENDING → APPROVED / REJECTED / WITHDRAWN`. Chỉ `APPROVED` được nộp và chấm.
- Số nhóm được thực hiện cùng đề tài chưa được nguồn quy định; tạm dùng `max_groups=1`, có cấu hình và kiểm tra đồng thời.

### Hội đồng và điểm

- Hội đồng nháp có thể chưa đủ người; chỉ đưa vào hoạt động/giao lịch chính thức khi đủ 3–5 GV, đúng một chủ tịch và một thư ký khác nhau.
- Cấm GVHD/đồng GVHD chấm đề tài mình hướng dẫn ở mọi cổng vào, không tạo ngoại lệ `SUPERVISOR` như sơ đồ cũ.
- Phiếu: `DRAFT → SUBMITTED → LOCKED`; chỉ tác giả sửa nháp. Mở lại sau gửi/công bố là thao tác đặc biệt có lý do.
- Chủ tịch tổng hợp hội đồng sau khi đủ phiếu cần thiết. Không coi phiếu thiếu là 0; không chia theo số người đã nhập nếu còn thiếu người bắt buộc.
- `final_score = tổng các điểm thành phần hợp lệ / số điểm thành phần bắt buộc`. Tập thành phần (GVPB, từng thành viên HĐ, hoặc các vòng riêng) phải chốt A05. Tạm đề xuất mỗi phiếu hợp lệ của phân công hiện hành được tính đúng một lần; không cộng thêm kết quả tổng hợp hội đồng vào chính các phiếu đã tạo ra nó.
- Thang 0–10 và làm tròn hai chữ số là đề xuất, không có trong S1. Dùng số thập phân, không làm tròn sớm từng nhóm điểm.
- Kết quả: `DRAFT → READY → PUBLISHED`; có nhánh `WITHDRAWN` nếu bổ sung quy trình sửa. Công bố phải có đủ điểm/xác nhận theo loại đợt.
- Chỉ email sau commit công bố; email lỗi có thể thử lại. SV nhận email không đồng nghĩa được xem tài nguyên chưa công bố trên website.

## 7. Các quyết định phải chốt trước khi khóa ERD

| Mã | Điểm chưa rõ / khác nhau giữa nguồn | Phương án tạm để thiết kế | Người cần xác nhận |
|---|---|---|---|
| A01 | S1 nói tối đa 3 hoặc 5 SV; S2/S3 nói từ 3–5 SV | Nhóm nháp 1–5, khi đăng ký tối thiểu 3; `max_members` là 3 hoặc 5 cấu hình theo đợt. Chưa tự gán loại nào là 3 | Thầy/Khoa |
| A02 | Một SV duy nhất một nhóm trong suốt quá trình, không nói rõ phạm vi các đợt | Chặn hai nhóm đang thực hiện trên toàn hệ thống; cho giữ nhóm lịch sử sau hoàn tất | Thầy |
| A03 | Ai chấp thuận đăng ký của nhóm chưa được nêu rõ | Trưởng khoa duyệt cuối; GVHD xem/đề xuất; không giữ giả định GVHD duyệt như bản nháp cũ | Thầy |
| A04 | Rubric nêu Trưởng bộ môn nhưng không định nghĩa quyền cụ thể | Rà soát/theo dõi/đề xuất trong bộ môn; không tự có quyền duyệt cuối hoặc xem mọi điểm | Thầy |
| A05 | Chưa định nghĩa điểm thành phần, số GVPB và các loại đợt cần hội đồng; S1 nói hội đồng chung nhưng ngày HĐ chỉ KLTN | Engine hỗ trợ chấm GVPB và HĐ, số phiếu bắt buộc cấu hình; không mặc định bỏ hội đồng cho 3 loại còn lại. Phải chốt chính sách từng loại trước sprint chấm điểm | Thầy |
| A06 | Một đề tài có thể nhận mấy nhóm? | Một nhóm được duyệt/đề tài, cấu hình `max_groups=1` | Thầy |
| A07 | Thang điểm, cách làm tròn, ngưỡng đạt và điểm riêng từng SV | Điểm chung đề tài 0–10, 2 chữ số; chưa tự tạo ngưỡng đạt/rớt hay điểm cá nhân | Thầy |
| A08 | Ai đăng ký thay nhóm, hạn nộp báo cáo, định dạng/dung lượng file | Nhóm trưởng đăng ký; thêm hạn báo cáo cấu hình; PDF/DOCX/ZIP tối đa 20 MB là đề xuất cần kiểm tra | Nhóm/Thầy |
| A09 | Có được đổi nhóm/đề tài sau duyệt, sửa điểm đã công bố? | Khóa sau duyệt; ngoại lệ qua quản lý có lý do, kiểm tra và lưu lịch sử | Thầy |
| A10 | Hosting ghi không bắt buộc nhưng rubric dùng để đánh giá mức cao | Đưa deploy vào P1 và thử sớm, không đợi cuối; chuẩn bị demo local dự phòng | Nhóm |
| A11 | Baseline lab pha phiên bản Spring/JDK/dependency | Java 25, Boot 4.1.x theo Lab 1; kiểm tra chạy mẫu MVC+Security+DTO trước khi cả nhóm chia code | Nhóm/Thầy nếu có repo chuẩn |
| A12 | Trưởng bộ môn quản lý một hay nhiều bộ môn; có xét duyệt sơ bộ bắt buộc? | Một bộ môn/người ở bản đầu, rà soát không là cửa duyệt bắt buộc; mở rộng mapping nếu cần | Thầy |

Các module tài khoản, danh mục, khung MVC, DTO và CRUD đợt có thể làm trước khi mọi câu hỏi được trả lời. Không khóa migration ràng buộc nhóm, quyền duyệt hay thuật toán điểm dựa trên giả định chưa ghi nhận.

## 8. Yêu cầu phi chức năng

| Mã | Yêu cầu | Cách kiểm chứng |
|---|---|---|
| NFR01 | Phân quyền server theo role + phạm vi + trạng thái + hạn | Kiểm thử truy cập chéo bằng URL/API/file trực tiếp; 403/404 phù hợp |
| NFR02 | BCrypt; CSRF cho phiên web; không lộ bí mật | Kiểm tra DB, HTML/JSON/log, request giả thiếu CSRF |
| NFR03 | Dữ liệu bền vững và toàn vẹn | Restart vẫn còn dữ liệu; FK/unique/transaction; test hai request đăng ký cùng lúc |
| NFR04 | Lịch và số điểm nhất quán | Test biên thời gian, timezone, điểm thiếu, số thập phân/làm tròn |
| NFR05 | UI rõ trạng thái, lỗi trường, không có dữ liệu, không có quyền | Mỗi form có validation message và giữ input hợp lệ; menu đúng actor |
| NFR06 | Phản hồi không bị chờ gửi email | Mail bất đồng bộ, theo dõi lỗi; không gộp SMTP trong transaction HTTP |
| NFR07 | Tìm kiếm/phân trang sử dụng được | Đề xuất thử 1.000 đề tài, 200 nhóm; mục tiêu trang danh sách dưới 2 giây ở local đã khởi động, đo rồi ghi kết quả |
| NFR08 | Dễ tái lập và bảo trì | Một baseline JDK/Maven, migration/seed, README; máy khác chạy được |
| NFR09 | Truy vết hành động học thuật | Lưu ai duyệt, ai phân công, ai chấm, ai công bố; lý do điều chỉnh |
| NFR10 | Lưu file an toàn và không mất sau deploy | Download có kiểm quyền; đường dẫn không công khai; redeploy kiểm tra file cũ |

Các mốc hiệu năng/dung lượng là mục tiêu nhóm đề xuất, không phải số liệu thầy đã cung cấp hay kết quả đã đo.
