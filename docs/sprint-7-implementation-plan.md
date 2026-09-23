# Sprint 7 — News Management: implementation plan

Ngày lập: 23/09/2026. Căn cứ: `News Management System Internship Assignment.md`, Sprint 7, REQ-NEWS-001..005, REQ-SEARCH-001, REQ-PAGING-001..002, REQ-SORT-001..002 và REQ-TEST-003. Stack hiện tại: Java 8, Servlet 4 (`javax.servlet`), JSP/JSTL, JDBC, MySQL, Maven WAR.

## 0. Điểm xuất phát và phạm vi

Sprint 6 đã có `GenericDAO` quản lý `Connection`/`PreparedStatement`/`ResultSet`, `CategoryService`/`CategoryDAO`, `PageResult<T>`, `AuthorizationFilter` cho `/admin/*`, `AuthSession` và `CsrfTokenManager`. Đợt refactor gần nhất đã sửa fixture Search, giới hạn cửa sổ pagination và nhận diện riêng mã lỗi trùng `1062`. Sau refactor, `mvn test` và 4 test `CategorySearchIntegrationTest` trên MySQL đã pass; chưa có kết quả chạy lại toàn bộ DB suite hoặc smoke test Tomcat. Đây là phần bằng chứng cần bổ sung trước khi tuyên bố hệ thống chạy hoàn chỉnh trên container, không cản trở việc bắt đầu thiết kế Sprint 7.

Sprint 7 triển khai trang quản trị `/admin/news`: List, Add, Edit, Delete, View, Search theo Title/Short Description, Filter Category, Sort theo Title/Created Date và Pagination. Danh sách hiển thị ID, Title, Thumbnail, Category, Created By, Created Date, Actions. News bắt buộc liên kết một Category và một User tạo bài; khi xoá News, Comment liên quan được xử lý theo FK `ON DELETE CASCADE` hiện có.

Ranh giới với các sprint sau:

- Sprint 8 triển khai upload ảnh, xác thực image, lưu file, thumbnail và rich text editor. Sprint 7 tạo News với `thumbnail = NULL`, không nhận URL hoặc file từ form; danh sách hiển thị placeholder khi chưa có ảnh. Khi sửa News, giữ nguyên thumbnail hiện có. Content Sprint 7 là plain text trong `textarea`, được escape khi hiển thị; không render HTML do người dùng nhập.
- Trang News công khai, tìm kiếm cho USER/anonymous và Comment UI thuộc các sprint website/comment sau. Sprint 7 chỉ triển khai admin News. Các case upload thumbnail/invalid image trong REQ-TEST-003 chuyển sang Sprint 8 theo lịch của assignment.
- Không thêm `slug`, `status`, `published_at`, `modified_by` hay trường khác vì schema hiện không lưu. Không đổi `GenericDAO` hoặc schema nếu không xác định được lỗi cụ thể.

## 1. Hợp đồng dữ liệu và URL

Schema `news`: `id`, `title VARCHAR(255)`, `short_description VARCHAR(500)`, `content LONGTEXT`, `thumbnail VARCHAR(500) NULL`, `category_id BIGINT NOT NULL`, `created_by BIGINT NOT NULL`, `created_date`, `modified_date`. `news.category_id` và `news.created_by` là FK `ON DELETE RESTRICT`; `comment.news_id` là FK `ON DELETE CASCADE`.

Tạo `NewsModel` cho các cột lưu trong DB. Dùng `Long createdById` cho FK; `AbstractModel.createdBy` hiện là `String` nên không dùng thuộc tính này làm author ID. Tạo projection `NewsListItem` chứa các cột cần hiển thị ở list (`categoryName`, `authorName` lấy từ JOIN) và `NewsDetail` cho trang View; các projection không được dùng làm đầu vào của `INSERT`/`UPDATE`. Tạo `NewsListCriteria` gồm `search`, `categoryId`, `sortName`, `sortBy`, `page`; tái sử dụng `PageResult<NewsListItem>` và cửa sổ tối đa 5 số trang.

| URL/method | Mục đích | Kết quả |
|---|---|---|
| `GET /admin/news` | List, Search, Filter, Sort, Pagination | Forward `admin/news/list.jsp` |
| `GET /admin/news?action=create` | Form Add | Forward `admin/news/form.jsp` |
| `GET /admin/news?action=edit&id=...` | Form Edit | Forward `admin/news/form.jsp`; ID không hợp lệ/không tồn tại trả lỗi có kiểm soát |
| `GET /admin/news?action=view&id=...` | Xem chi tiết | Forward `admin/news/detail.jsp`; ID không tồn tại trả 404 |
| `POST /admin/news?action=create` | Tạo News | Redirect về list sau thành công |
| `POST /admin/news?action=update` | Sửa News | Redirect về list sau thành công |
| `POST /admin/news?action=delete` | Xoá News | Redirect về list sau thành công |

Các GET chỉ đọc dữ liệu. POST phải đi qua `AuthorizationFilter`, xác thực CSRF trước khi gọi Service; redirect sau ghi dữ liệu để refresh không gửi lại form. Dùng ID của `AuthenticatedUser` từ `AuthSession` cho `created_by`, không nhận `createdBy` từ request.

## 2. Quy tắc List, Search, Filter, Sort và Pagination

| Parameter | Hợp lệ | Mặc định/xử lý sai |
|---|---|---|
| `search` | Chuỗi trim tối đa 100 ký tự; tìm contains trong `title` hoặc `short_description` | Rỗng = không lọc; quá dài báo validation có kiểm soát |
| `categoryId` | ID dương của Category; thiếu = tất cả | Sai định dạng/không dương: báo lỗi và về bộ lọc mặc định |
| `sortName` | `title`, `createdDate` | `createdDate` |
| `sortBy` | `asc`, `desc` | `desc` |
| `page` | Số nguyên dương | `1`; vượt số trang thì clamp về trang cuối |

Page size cố định 10; không nhận `limit` tuỳ ý từ client. Tập rỗng trả `page=1`, `totalPages=0`, không có Previous/Next. Khi thay search/filter/sort từ form, reset `page=1`. Link pagination giữ đầy đủ `search`, `categoryId`, `sortName`, `sortBy` bằng `c:url` và `c:param`. Thứ tự luôn ổn định: sort theo cột đã whitelist và thêm `n.id` cùng chiều làm tie-breaker.

Search dùng `LIKE ? ESCAPE '!'`; escape `!`, `%`, `_` để chúng được tìm như ký tự thường. Điều kiện WHERE và danh sách parameter của query trang phải khớp với `COUNT(*)`. Dữ liệu người dùng và `LIMIT ?, ?` được bind qua `GenericDAO`; chỉ cột và hướng sort đã whitelist ở server mới được ghép vào SQL. Ví dụ query theo bộ lọc:

```sql
SELECT n.id, n.title, n.thumbnail, n.category_id,
       c.name AS category_name, u.full_name AS author_name, n.created_date
FROM news n
JOIN category c ON c.id = n.category_id
JOIN `user` u ON u.id = n.created_by
WHERE (n.title LIKE ? ESCAPE '!' OR n.short_description LIKE ? ESCAPE '!')
  AND n.category_id = ?
ORDER BY n.created_date DESC, n.id DESC
LIMIT ?, ?;
```

Chỉ thêm WHERE tương ứng khi filter hiện diện; `COUNT(*)` dùng cùng điều kiện. Không `SELECT *` hoặc lấy `content LONGTEXT` ở list. Trường `thumbnail` được đọc để chuẩn bị cho Sprint 8, nhưng Sprint 7 chỉ hiển thị placeholder khi NULL và không cho sửa bằng form.

## 3. Lớp cần triển khai

1. **Model/Mapper:** thêm `NewsModel`, `NewsListItem`, `NewsDetail`, `NewsListCriteria`; mapper JDBC cho record đầy đủ và các projection JOIN. Mapper chỉ đọc `ResultSet`, không xử lý nghiệp vụ.
2. **DAO:** thêm `INewsDAO`/`NewsDAO extends GenericDAO` với `findPage(criteria, offset, limit)`, `countByCriteria(criteria)`, `findById(id)`, `findDetailById(id)`, `insert(news)`, `update(news)`, `delete(id)`. `NewsDAO` sở hữu SQL và whitelist sort, không tự mở hoặc đóng connection. `insert` chỉ ghi Title, Short Description, Content, `category_id`, `created_by`; thumbnail mặc định NULL. `update` sửa Title, Short Description, Content, `category_id`, `modified_date`; không ghi đè `id`, `created_by`, `created_date`, `thumbnail`.
3. **Service:** thêm `INewsService`/`NewsService` dùng constructor injection. Validate và trim Title/Short Description/Content; Title tối đa 255, Short Description tối đa 500, các trường bắt buộc không rỗng; `categoryId` phải dương và tồn tại. Create nhận author ID từ controller/session, kiểm tra hợp lệ, DB FK là lớp bảo vệ cuối khi Category/User bị xoá đồng thời. Edit phải xác nhận News tồn tại, giữ nguyên ID/author/thumbnail, xử lý trường hợp Category bị xoá giữa bước kiểm tra và UPDATE. Delete dùng FK cascade hiện có để xoá Comment cùng News; không xoá Comment bằng vòng lặp ở Service.
4. **Controller:** thêm `NewsController` tại `/admin/news`, parse input, gọi Service, gắn `PageResult`, criteria, danh sách Category và lỗi lên request. Mọi form POST có CSRF token; lỗi validation forward lại form với giá trị đã nhập và token. ID thiếu/sai/không tồn tại được xử lý thống nhất; lỗi DB ngoài các FK đã xác định không bị chuyển thành lỗi validation chung.
5. **JSP/navigation:** thêm `admin/news/list.jsp`, `form.jsp`, `detail.jsp`; thêm link News Management cho ADMIN trên `home.jsp`. Dùng `c:out` cho Title, Short Description, tên Category, tên tác giả và nội dung text; `detail.jsp` giữ xuống dòng bằng CSS `white-space: pre-wrap`. Form chọn Category từ DB, có empty state và hướng dẫn tạo Category nếu danh sách rỗng. Delete phải nêu rõ Comment liên quan sẽ bị xoá theo cascade. Không render HTML thô từ `content` trong Sprint 7.

Luồng ghi dữ liệu: request → `AuthorizationFilter` → `NewsController` (CSRF/session) → `NewsService` (validation) → `NewsDAO` (SQL) → `GenericDAO` (JDBC/resource). Luồng đọc: `NewsDAO` → mapper/projection → `NewsService` → `NewsController` → JSP. Tái sử dụng `PageResult`, `AuthSession`, `CsrfTokenManager`, `AuthorizationFilter`, `CategoryService` và `GenericDAO`; không tạo lớp JDBC song song.

## 4. Kế hoạch triển khai theo thứ tự

| Bước | Công việc | Kết quả kiểm tra được |
|---|---|---|
| 1 | Chốt model/projection và hợp đồng `INewsDAO`/`INewsService`; xác định query list/detail và quy tắc thumbnail/content Sprint 7 | Mapper khớp cột DB, không nhận author/thumbnail từ form |
| 2 | Viết DAO cho CRUD, JOIN list/detail, Search + Filter + Sort + Count + Limit | Query bind đúng; sort whitelist; count và list cùng filter |
| 3 | Viết Service cho validation, author từ session, Category FK, page clamp, update giữ các trường bất biến, delete cascade | Unit test cho create/update/delete/search/pagination |
| 4 | Nối Controller/JSP và menu ADMIN; PRG, CSRF, giữ filter khi đổi trang | Luồng List/Add/Edit/Delete/View hoạt động trên Tomcat |
| 5 | Thêm integration test MySQL với fixture riêng cho User, Category, News, Comment; cleanup theo ID do test tạo | REQ-TEST-003 TC01..TC07 và cascade pass |
| 6 | Chạy suite, build WAR, smoke test HTTP trên Tomcat 9/tương thích Servlet 4 | ADMIN thao tác đủ chức năng; USER/anonymous bị chặn |

## 5. Test bắt buộc và dữ liệu test

| Cấp | Case |
|---|---|
| Unit Service | Title/Short Description/Content/Category rỗng; giới hạn 255/500; Category không tồn tại; author ID thiếu; update giữ ID/author/thumbnail; tìm không ra; delete News không tồn tại |
| Unit Search/Pagination | Search title và short description; literal `%`/`_`/`!`; Category filter độc lập và kết hợp search; sort Title/Created Date hai chiều, fallback khi `sortName` giả mạo; page đầu/cuối/rỗng/vượt tổng; cửa sổ tối đa 5 link |
| DAO + MySQL | JOIN trả đúng Category/author; `COUNT(*)` khớp filter; `LIMIT ?, ?`; thứ tự ổn định khi trùng Title/Created Date; không SQL injection qua sort/filter |
| CRUD + MySQL | Add/Edit/Delete; FK Category/User; xoá News có Comment làm Comment biến mất theo cascade; Category còn nguyên; cập nhật không đổi author/thumbnail |
| Controller/Security | Anonymous redirect login; USER nhận 403; ADMIN xem và ghi dữ liệu; POST thiếu/sai CSRF nhận 403 và không gọi Service; form lỗi giữ dữ liệu/token; View/ID sai trả lỗi có kiểm soát |

Integration test dùng schema MySQL dành riêng cho test, tạo code/username/email với mã ngẫu nhiên theo mỗi run và ghi lại các ID đã tạo. Cleanup theo đúng ID theo thứ tự Comment → News → Category → User; không dùng `DELETE ... WHERE code LIKE 'prefix%'`, không dựa vào seed hoặc User đầu tiên trong DB. Chỉ chạy `database/schema.sql` trên database test đã xác nhận là disposable vì file chứa `DROP TABLE`. Trước khi chạy full DB suite, rà lại các test cũ đang cleanup bằng code cố định để không xoá dữ liệu ngoài fixture.

Lệnh kiểm chứng sau khi có database test riêng:

```powershell
mvn test
mvn clean package -DrunDbTests=true
```

Smoke test trên Tomcat: đăng nhập ADMIN, tạo Category test, tạo News, xem list/detail, sửa, tìm theo Title/Short Description, lọc Category, sort hai chiều, đi trang đầu/cuối, xoá News có Comment và xác nhận Comment bị cascade; xác nhận USER/anonymous không truy cập `/admin/news`, POST thiếu CSRF không thay đổi DB. Dọn đúng record được tạo trong smoke test. Đồng thời hoàn tất smoke test Category/Sprint 6 còn thiếu nếu chưa có kết quả từ môi trường chạy thật.

## 6. Definition of Done

Sprint 7 đạt khi ADMIN dùng được List/Add/Edit/Delete/View/Search/Filter Category/Sort/Pagination; `created_by` luôn lấy từ session; Title/Short Description/Content/Category được validate; News gắn Category/User hợp lệ; xoá News có Comment đúng chính sách cascade; query an toàn, kết quả phân trang ổn định; form/JSP escape dữ liệu; test REQ-TEST-003 TC01..TC07 và smoke test HTTP pass. TC08/TC09, upload, thumbnail mới và rich text là tiêu chí Sprint 8.
