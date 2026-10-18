# Lộ trình triển khai kiểm thử và bàn giao

## 1. Cách chia công việc

Đề xuất nhịp 6 tuần cho nhóm 3–5 người; đây là ước lượng để phân việc, không phải deadline thầy đưa. Có thể co giãn theo lịch thực. Mỗi module phải đi từ DTO đến service/repository và màn hình/test; không để một người làm toàn bộ backend và một người chỉ ghép HTML đến cuối mới tích hợp.

Các việc đầu ra gồm: use case đúng actor, danh mục FR/NFR, ma trận quyền, ERD và data dictionary, khung MVC có DTO, các luồng đầy đủ, kiểm thử, deploy, tài liệu và demo cá nhân.

## 2. Các giai đoạn và cửa kiểm tra

| Giai đoạn | Thời gian đề xuất | Việc phải làm | Điều kiện hoàn thành / phụ thuộc |
|---|---|---|---|
| G0 — Chốt yêu cầu và mẫu kiến trúc | 2–3 ngày đầu | Chốt A01–A04/A06/A11; lập policy A05/A07/A08; cập nhật use case; ERD v1; thử baseline lab | Mọi thành viên chạy cùng JDK/Maven; mẫu MVC+DTO+Security+JPA render được; quyết định chưa chốt có người phụ trách |
| G1 — Nền tảng và danh mục | Phần còn lại tuần 1 | Repo, migration, account/role, profile GV/SV, bộ môn, layout, error handling, seed, CI đơn giản | CRUD cơ bản chạy; login đủ role; URL sai quyền bị chặn; restart giữ dữ liệu; thử Docker/host/mail sớm |
| G2 — Đợt và danh sách đề tài | Tuần 2 | CRUD đợt, 2 giai đoạn, đề xuất/duyệt/công bố, 1–2 GVHD, scope TBM, tìm kiếm | SV không đăng ký trước GĐ2; đề tài chưa công bố không lộ; đầu ra đủ cho nhóm SV đăng ký |
| G3 — Nhóm, đăng ký và báo cáo | Tuần 3 | Nhóm nháp/mời/nhận/chuyển trưởng; đăng ký/duyệt/rút; upload/download phiên bản | Đúng 3–5 theo policy, một nhóm hoạt động/SV, một đề tài/nhóm; race test; chỉ trưởng nhóm nộp |
| G4 — Phản biện và hội đồng | Tuần 4 | Phân công, hội đồng, chủ tịch/thư ký, lịch, phiếu điểm và tổng hợp | A05/A07 đã chốt; không GVHD tự chấm; 3–5 GV; đủ phiếu mới tổng hợp; tính điểm đúng |
| G5 — Công bố và hoàn thiện | Tuần 5 | Công bố, email từng SV, retry, tra cứu đúng scope, thông báo/dashboard, API, AOP, deploy | End-to-end đủ 4 loại đợt theo policy; mail có minh chứng; API/HTML cùng quyền; storage bền vững |
| G6 — Kiểm thử và bàn giao | Tuần 6 | Regression, sửa lỗi, chụp UI, cập nhật ERD/code/docs, diễn tập và đóng ZIP | Các test bắt buộc qua; mọi thành viên giải thích được code; hồ sơ đúng mẫu; chạy từ máy sạch |

Thông báo và CRUD danh mục có thể chạy song song về mặt phân công với G2–G3 sau khi khung chung ổn định. Email service/AOP có thể dựng sớm, chỉ tích hợp sự kiện công bố khi G4 có đầu ra. Không chờ tuần cuối mới thử host, mail hoặc storage.

## 3. Backlog theo đầu ra

### P0 — Phải hoàn thành nghiệp vụ

- [ ] B0. Use case tổng quan và chi tiết thể hiện đầy đủ Trưởng khoa, TBM, GVHD, GVPB, HĐ, SV/nhóm trưởng; bỏ tự chấm đề tài HD.
- [ ] B1. Ma trận quyền gắn scope dữ liệu, thời gian và trạng thái; ghi quyết định A01–A12.
- [ ] B2. ERD, data dictionary gồm tên bảng/cột, kiểu, NULL, PK/FK, unique/check, cardinality và mô tả.
- [ ] B3. Tài khoản, role, login/logout; BCrypt; profile GV/SV; CRUD bộ môn; chặn truy cập trực tiếp.
- [ ] B4. CRUD đợt, đủ 4 loại, validation thời gian, hai giai đoạn.
- [ ] B5. CRUD đề tài, đúng bộ môn, 1–2 GVHD, Trưởng khoa duyệt/công bố, TBM đúng scope.
- [ ] B6. Nhóm nháp, xác nhận tham gia, một trưởng nhóm, giới hạn số lượng và chống tham gia hai nhóm.
- [ ] B7. Đăng ký/chấp thuận/từ chối đề tài, một đăng ký hoạt động; xử lý xung đột đồng thời.
- [ ] B8. Nhóm trưởng nộp báo cáo; phiên bản và download có kiểm quyền.
- [ ] B9. Phân công GVPB; nhập/gửi điểm đúng người, đúng hạn, không GVHD tự chấm.
- [ ] B10. Hội đồng 3–5 GV, chủ tịch/thư ký, phân công và lịch; điểm cá nhân và tổng hợp chủ tịch.
- [ ] B11. Điểm cuối trung bình cộng đúng policy; thiếu phiếu không công bố.
- [ ] B12. Khoa công bố; mail tới từng SV; lỗi gửi có trạng thái/gửi lại; không gửi trước commit.
- [ ] B13. SV tra cứu đúng nhóm và đợt mình đăng ký, chỉ sau công bố.
- [ ] B14. CRUD/đọc thông báo chính thức; menu và dashboard theo vai trò.

### P1 — Hoàn thiện kiến trúc và chất lượng để nộp tốt

- [ ] B15. Áp dụng đủ 6 lab: API chọn lọc dùng chung service, AOP, mail async; DTO request/response rõ ràng.
- [ ] B16. HTML/API error handling thống nhất, validation, phân trang/tìm kiếm, trạng thái rỗng/không quyền.
- [ ] B17. Audit công bố/điểm/phân công, test concurrent/IDOR, backup/restore; quy trình chỉnh điểm nếu bật.
- [ ] B18. JAR/Docker/host chạy; env/SMTP/storage kiểm tra thật; README từ máy sạch.
- [ ] B19. Minh chứng file/route/test theo S3, tài liệu đủ chương theo S1, diễn tập từng thành viên.

### P2 — Chọn sau khi luồng chính ổn định

- [ ] Xuất kết quả Excel/PDF có cùng phân quyền với UI.
- [ ] Dashboard lọc nâng cao, thống kê nhiều đợt/bộ môn.
- [ ] Email nhắc hạn/tình trạng đăng ký; phân biệt với email công bố bắt buộc.
- [ ] Xếp lịch có cảnh báo trùng phòng/giờ tự động, chưa cần tối ưu toàn cục.
- [ ] Reset mật khẩu qua token email, import CSV GV/SV có validate/preview.

Tính năng mở rộng phải có mục riêng trong báo cáo và minh chứng thực tế để đáp ứng rubric. Không đổi tên chức năng bắt buộc thành sáng tạo để tính trùng.

## 4. Gợi ý phân công nhóm

| Thành viên phụ trách | Module chính | Trách nhiệm tích hợp |
|---|---|---|
| A | Khung MVC/DTO, account/security, GV/SV/bộ môn | Chốt convention, hỗ trợ policy và review quyền server |
| B | Đợt, đề tài, duyệt/công bố danh sách, GVHD, TBM | Đồng bộ trạng thái/thời gian với nhóm đăng ký |
| C | Nhóm, thành viên, đăng ký, báo cáo/file | Chống race, kiểm quyền theo nhóm và download |
| D | GVPB, hội đồng, lịch, phiếu, tổng hợp | Chốt công thức điểm và chống xung đột GVHD |
| E | Công bố, email, thông báo, dashboard, deploy | Theo dõi gửi mail, test end-to-end và hồ sơ minh chứng |

Nếu có 4 người: giữ A–D; A nhận thêm thông báo/dashboard và điều phối deploy, D nhận thêm công bố/email; B/C hỗ trợ kiểm thử tích hợp và hồ sơ. Nếu có 3 người: A làm nền tảng + đợt/đề tài; B làm nhóm/đăng ký/báo cáo + thông báo; C làm phản biện/hội đồng/kết quả/email; deploy và kiểm thử cùng làm. Với 3 người nên giảm P2 trước, không cắt bảo mật hay nghiệp vụ bắt buộc.

Mỗi người tự viết test phần mình, cập nhật file/route minh chứng, giải thích DTO và transaction liên quan. Tỷ lệ đóng góp ghi theo công việc thật, không tự điền 20% đồng loạt khi chưa có dữ liệu.

## 5. Quy ước làm nhóm và tích hợp

- Một nhánh chính chạy được; nhánh tính năng ngắn, PR nhỏ theo luồng nghiệp vụ. Không commit secrets, dữ liệu cá nhân thật hoặc file build.
- Trước code module, thống nhất Request/Response DTO, route, tên model attribute, status, quyền và lỗi trả về.
- Mỗi migration có số/tên duy nhất và một người review; không sửa migration đã dùng chung, viết migration mới.
- Mỗi PR có mô tả tình huống, thay đổi, test pass và ảnh màn hình nếu thay UI. Review chéo ít nhất một người cho nhóm/điểm/quyền.
- Tích hợp đều theo mốc; mỗi tuần chạy một kịch bản xuyên suốt bằng dữ liệu seed. Không chỉ test riêng màn hình của từng người.
- Giữ `main` build được với môi trường chuẩn; khóa dependency và schema v1 trước khi làm ồ ạt.

## 6. Bộ màn hình cần thiết kế

| Cụm màn hình | Trạng thái phải có | Vai trò chính |
|---|---|---|
| Login, profile, đổi mật khẩu, 403/404 | sai thông tin, khóa tài khoản, lỗi trường, thành công | Tất cả |
| Dashboard | chưa có dữ liệu, sắp hết hạn, thiếu phân công/phiếu, đã công bố | Theo actor |
| Tài khoản/GV/SV/bộ môn | list/filter/page, create/edit, xóa được/không được | Admin/Khoa |
| Đợt | draft, chưa mở, GĐ1, chờ công bố, GĐ2, đóng | Khoa; người khác xem |
| Đề tài | nháp/chờ duyệt/trả sửa/từ chối/công bố, đủ/chưa đủ GVHD | GV/Khoa/TBM/SV |
| Nhóm của tôi | chưa có nhóm, lời mời, thiếu thành viên, đủ, khóa | SV/nhóm trưởng |
| Đăng ký | còn chỗ/hết chỗ, ngoài hạn, pending/approved/rejected/withdrawn | Nhóm trưởng/Khoa |
| Báo cáo | chưa nộp, đang upload, lỗi loại/size, đã nộp, hết hạn, lịch sử | Nhóm trưởng và người xem được phép |
| Phân công phản biện/hội đồng | hợp lệ/xung đột GVHD, thiếu thành viên, trùng vai trò | Khoa |
| Chấm điểm | chưa nhập, nháp, đã gửi, khóa, ngoài hạn, lỗi điểm | GVPB/HĐ |
| Tổng hợp/công bố | thiếu phiếu, đủ phiếu, chủ tịch xác nhận, published, email lỗi | Chủ tịch/Khoa |
| Kết quả cá nhân | chưa công bố, đã công bố, đợt không thuộc SV | SV |
| Thông báo | nháp/đã đăng/lưu trữ, đúng/sai audience | Người quản lý/người đọc |

Mỗi form có nhãn, validation sát trường, nút quay lại và thông báo sau thao tác. Khi hết hạn hoặc không có quyền, giải thích lý do nếu được phép tiết lộ, không chỉ làm nút mất tích.

## 7. Test bắt buộc

Mỗi test dưới đây cần mã thực thi hoặc checklist/manual evidence; không ghi PASS khi chưa chạy. Ưu tiên unit test policy/công thức, integration test constraint/transaction với MySQL tương thích production, MockMvc/security test cho cả HTML/API và test end-to-end thủ công.

| ID | Kịch bản | Kết quả mong đợi |
|---|---|---|
| T01 | Chưa login vào trang và API nghiệp vụ | HTML chuyển login; API 401, không trả dữ liệu |
| T02 | Tài khoản bị khóa/thu hồi role khi đã có session | Quyền bị vô hiệu theo cơ chế đã thiết kế, không tiếp tục thao tác |
| T03 | SV gọi POST duyệt đề tài bằng URL/API | 403, DB không đổi |
| T04 | TBM đổi departmentId sang bộ môn khác | Không đọc/sửa dữ liệu ngoài phạm vi |
| T05 | Form/API sửa profile gửi thêm role/evaluatorId/status | Trường ngoài DTO không cấp quyền/đổi nghiệp vụ |
| T06 | POST phiên web thiếu/sai CSRF | Bị từ chối; API cùng session cũng được bảo vệ |
| T07 | Đổi mật khẩu và kiểm tra DB/log/JSON | Hash BCrypt, không lộ mật khẩu/hash/secret |
| T08 | CRUD đề tài/GV/SV/đợt/bộ môn/thông báo | Tạo/xem/sửa/xóa nháp hợp lệ; không xóa dây chuyền dữ liệu đã dùng |
| T09 | Đợt có start >= end, lịch chồng, loại sai trường | Lỗi validation rõ; không ghi dữ liệu sai |
| T10 | 4 loại đợt với hạn GVPB/ngày HĐ | TLCN/KLTN mới có hạn GVPB; chỉ KLTN có ngày HĐ ở cycle; quy trình khác theo policy |
| T11 | Gửi đề tài/đăng ký đúng trước, tại và sau biên giờ | Server dùng cùng quy tắc `[start,end)`; ngoài hạn bị chặn |
| T12 | SV xem đề tài nháp bằng id hoặc API | Không lộ nội dung chưa công bố |
| T13 | Công bố đề tài có 0, 1, 2, 3 GVHD hoặc GV trùng | Chỉ 1–2 GV khác nhau hợp lệ |
| T14 | Tạo nhóm 1 người, sau đó đăng ký khi 2/3/5/6 người | Cho nháp; chặn/cho theo min/max của đợt đã chốt |
| T15 | Một SV nhận hai lời mời đồng thời | Chỉ một nhóm ACTIVE được ghi |
| T16 | Thành viên rời nhóm/chuyển trưởng/đổi nhóm đã khóa | Đúng một trưởng nhóm; kiểm đúng trạng thái; không có nhóm mất trưởng |
| T17 | Cùng nhóm đăng ký hai đề tài đồng thời | Chỉ một đăng ký hoạt động |
| T18 | Hai nhóm tranh đề tài max_groups=1 | Không vượt sức chứa, bên thua nhận lỗi nghiệp vụ dễ hiểu |
| T19 | Đăng ký đề tài ở cycle khác nhóm | Bị từ chối |
| T20 | Từ chối/rút/duyệt lại request | Không nhân đôi đăng ký; giữ lịch sử; giải phóng slot theo policy |
| T21 | SV thường hoặc trưởng nhóm khác upload báo cáo | Bị từ chối kể cả sửa id/hidden input |
| T22 | Upload quá size, sai định dạng, path traversal | Bị từ chối; không ghi file ngoài storage |
| T23 | Người ngoài gọi thẳng URL download | Không tải được file; owner và GV được giao tải được |
| T24 | Đổi phiên bản báo cáo khi đã chấm | Bị khóa hoặc theo quy trình mở lại rõ ràng, không đổi nền phiếu âm thầm |
| T25 | Phân GVHD/đồng GVHD làm GVPB hoặc HĐ đề tài mình | Bị chặn cả khi phân công và khi gửi điểm |
| T26 | Đổi GVHD sang người đã chấm | Bị từ chối hoặc phải xử lý invalidation được ghi nhận trước |
| T27 | Hội đồng 2/3/5/6 GV, trùng GV, hai chủ tịch, thiếu thư ký | Chỉ bộ 3–5 với đúng vai trò hợp lệ được kích hoạt |
| T28 | GV không được giao hoặc sửa evaluatorId để chấm | 403/404 phù hợp, không tạo phiếu |
| T29 | Điểm âm, vượt thang, nhập text/null | Validation lỗi; 0 hợp lệ không bị hiểu là chưa nhập |
| T30 | Gửi phiếu hết hạn hoặc sửa phiếu người khác/đã khóa | Bị chặn |
| T31 | Chủ tịch hội đồng khác tổng hợp; thư ký xác nhận thay | Bị chặn |
| T32 | Thiếu một phiếu hoặc chưa xác nhận hội đồng | Không READY/PUBLISHED; không coi phiếu thiếu là 0 |
| T33 | Bộ điểm 7, 8, 9 và bộ có phân số | Điểm 8; bộ phân số làm tròn cuối theo policy; không trung bình chồng |
| T34 | Phiếu cũ bị thay assignment/cùng người trùng stage | Không tính hai lần; số phiếu theo policy hiện hành |
| T35 | Công bố thất bại/transaction rollback | Không gửi email công bố sai |
| T36 | Công bố nhóm 3 hoặc 5 SV | Mỗi SV có email đúng phiên bản; không chỉ trưởng nhóm |
| T37 | Bấm công bố hai lần, SMTP lỗi, worker restart | Không nhân bản kết quả; mail lỗi truy vết/retry; biết giới hạn khả năng gửi trùng |
| T38 | SV tra cứu điểm nhóm khác cùng đợt | Bị chặn |
| T39 | SV tra cứu đợt khác hoặc điểm chưa công bố qua API | Bị chặn, danh sách/bộ lọc cũng không lộ dữ liệu |
| T40 | CRUD thông báo và truy cập nháp/sai audience | Chỉ người có quyền xem; publish đúng người |
| T41 | CRUD qua API rồi xem HTML và ngược lại | Cùng DB, cùng quyền/nghiệp vụ; trả lỗi đúng định dạng |
| T42 | Restart app / redeploy rồi tải báo cáo và tra cứu điểm | Dữ liệu và file còn nguyên |
| T43 | AOP nhánh thành công/lỗi/async | Ghi actor/thao tác/thời gian phù hợp; không log secret hoặc tuyên bố commit sai |
| T44 | Chạy từ repo sạch theo README trên máy khác | Build, migration, seed, login, luồng chính hoạt động |

## 8. Kịch bản demo từ đầu đến cuối

Chuẩn bị seed giả: 1 Admin, 1 Trưởng khoa, 2 bộ môn với TBM tương ứng, ít nhất 6 GV để phân công hướng dẫn/phản biện/hội đồng không xung đột, 10–15 SV chia nhóm; các đợt ở nhiều giai đoạn. Không dùng thông tin cá nhân thật trong seed hoặc mail demo.

1. Admin tạo/xem tài khoản GV/SV, kiểm tra quyền và bộ môn.
2. Trưởng khoa tạo đợt, nhập lịch hợp lệ và cấu hình nhóm.
3. GV gửi đề tài; TBM xem đúng phạm vi; Khoa phân công GVHD, duyệt và công bố.
4. SV tạo/tham gia nhóm, chỉ một trưởng; demo chặn tham gia nhóm thứ hai.
5. Nhóm trưởng đăng ký; người duyệt chấp thuận; thành viên thường thử nộp bị chặn, trưởng nhóm nộp được.
6. Khoa phân GVPB/hội đồng; demo chặn GVHD tự chấm và hội đồng thiếu vai trò.
7. GVPB/thành viên HĐ nhập điểm; Chủ tịch tổng hợp; thiếu phiếu thì chưa công bố được.
8. Khoa công bố; kiểm tra mail từng SV; SV xem đúng kết quả và bị chặn khi đổi sang đợt/nhóm khác.
9. Thể hiện CRUD thông báo, API dùng chung service, AOP log và môi trường deploy.

Nếu cần chuyển pha trong demo, dùng các đợt seed khác nhau hoặc tính năng quản trị thời gian hợp lệ có audit. Không tạo backdoor bỏ security/time check trong môi trường công khai.

Demo chính 10–15 phút theo rubric, chuẩn bị phần mở rộng nếu thầy cho tối đa 30 phút. Không bắt buộc PowerPoint không có nghĩa được bỏ demo/thuyết trình/vấn đáp.

## 9. Đối chiếu rubric và hồ sơ nộp

| Tiêu chí | Điểm tối đa | Minh chứng cần chuẩn bị |
|---|---|---|
| Báo cáo | 1.0 | Đủ chương, phân công chi tiết, ảnh UI/flow, cài đặt và link chạy nếu có |
| Use case và database | 1.0 | Đủ actor, FR/NFR; ERD khớp schema/code; cột/kiểu/PK/FK/ràng buộc |
| Kiến trúc | 1.0 | MVC rõ; DTO; ít nhất 3 kỹ thuật Spring; mật khẩu hash; phân quyền server chi tiết |
| Chức năng | 2.0 | CRUD + 2 giai đoạn + nhóm + hội đồng + điểm + công bố/mail + tra cứu đúng phạm vi + deploy |
| Thuyết trình/demo | 1.0 | Kịch bản chạy ổn, dữ liệu chuẩn bị, mọi người có phần trình bày |
| Vấn đáp cá nhân | 3.0 | Mỗi người giải thích luồng MVC, annotation, file/hàm, sửa trực tiếp phần mình |
| Mở rộng/phản biện | 1.0 | Giải thích xung đột đăng ký/quyền/lỗi; mở rộng hữu ích có mục riêng và minh chứng |

Phần sản phẩm/báo cáo chiếm 5 điểm; demo, vấn đáp và mở rộng/phản biện chiếm 5 điểm. Cần dành thời gian hiểu và diễn tập, không chỉ tăng số màn hình. Mẫu S3 yêu cầu minh chứng chính xác và trách nhiệm cá nhân; code sinh từ công cụ cũng phải được thành viên hiểu và giải thích.

Hồ sơ cần chuẩn bị khi nộp thật, không tạo Word ở bước lập kế hoạch hiện tại:

- Báo cáo Word theo S1: Chương 1 tổng quan/phân công; Chương 2 MVC và ít nhất 3 kỹ thuật Spring; Chương 3 use case và FR/NFR; Chương 4 ERD/bảng/cột; Chương 5 ảnh giao diện/flow; Chương 6 kết luận; phụ lục cài đặt; tài liệu tham khảo.
- Toàn bộ source, Maven Wrapper, config mẫu, migration/seed, hướng dẫn build/run, test và sơ đồ DrawIO chỉnh sửa được.
- File báo cáo kết quả theo S3: thành viên, % thực tế, mức hoàn thành 8 hạng mục, file/controller/route đúng, tự chấm và khó khăn/bài học.
- Link deploy nếu có; không ghi đạt deploy/email thật nếu chỉ localhost/Mailpit.
- ZIP đúng `GroupXX_MSSV1_MSSV2_...zip`, liệt kê đúng số thành viên thực. Đề bài và rubric nhấn mạnh sai định dạng tên ZIP không được chấm.

## 10. Tiêu chí một chức năng được coi là xong

Chỉ đánh dấu hoàn thành khi: actor có quyền thao tác được; actor không có quyền bị server chặn; ràng buộc/time/state được kiểm; HTML và API nếu có cùng kết quả; dữ liệu persist; lỗi hiển thị rõ; test liên quan qua; file/route và ảnh minh chứng được cập nhật; người phụ trách giải thích được toàn bộ đường đi DTO → controller → service → repository → DB → view.

## 11. Việc team thực hiện ngay sau khi duyệt plan

1. Gửi thầy bảng A01–A12, ưu tiên nhóm, người duyệt đăng ký, TBM và thành phần điểm; ghi câu trả lời vào decision log.
2. Chốt thành viên/deadline và gán người phụ trách từng backlog; không đợi tất cả policy chấm điểm để dựng nền tảng.
3. Làm G0: một mẫu MVC+DTO+JPA+Security chạy trên baseline chung, thử API/AOP/mail để phát hiện lệch dependency.
4. Sửa use case cũ và dựng ERD v1 theo policy đã xác nhận; review chéo trước migration chung.
5. Chia các module sau khi đã có mẫu code, schema và hợp đồng DTO/route; triển khai theo G1–G6.
