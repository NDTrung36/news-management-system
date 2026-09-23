# Sprint 6 — Category Management: implementation plan

Ngày lập: 23/09/2026. Căn cứ: `News Management System Internship Assignment.md`, REQ-CATEGORY-001..005, REQ-PAGING-001..002, REQ-SORT-002, REQ-TEST-002 và mục Sprint 6. Stack hiện tại: Java 8, Servlet 4 (`javax.servlet`), JSP/JSTL, JDBC, MySQL, Maven WAR.

Trạng thái đầu vào: Sprint 5 đã có role, `AuthorizationFilter` cho `/admin/*`, trang 403 và CSRF cho Category. Ngày 23/09/2026, 23 test liên quan Sprint 5 pass; bộ 56 test với MySQL còn 3 test cũ fail vì giả định category seed `java` luôn tồn tại. Chưa có bằng chứng smoke test trên Tomcat. Vì vậy phần việc ở mục 0 là điều kiện nhận Sprint 5 trước khi đóng Sprint 6, không được ghi nhận Sprint 5 là đã đạt Definition of Done đầy đủ.

## 0. Gate đầu vào: hoàn tất kiểm chứng Sprint 5

1. Đổi ba test `CategoryDaoIntegrationTest.testFindByCodeAndFindById`, `GenericDaoReadIntegrationTest.testQueryOneCategory`, `CategoryPreparedStatementIntegrationTest.testSelectCategoryByCode` sang fixture category riêng theo test: tạo record với code dành riêng cho test, assert trên record đó, cleanup đúng record đã tạo. Không xoá hoặc tái nạp seed hiện có; không chạy `database/schema.sql` trên database đang dùng vì file có `DROP TABLE`.
2. Chạy `mvn clean package -DrunDbTests=true`; yêu cầu toàn bộ test pass, không skipped.
3. Trên Tomcat 9 hoặc container tương thích Servlet 4, kiểm tra anonymous → `/admin/category` redirect `/login`; USER → 403; ADMIN → danh sách Category; POST thiếu CSRF → 403; logout rồi truy cập lại admin → login. Dùng tài khoản test riêng và dọn dữ liệu test sau khi chạy.
4. Nếu không có Tomcat khả dụng tại môi trường triển khai, ghi rõ gate smoke test còn mở. Không suy diễn từ unit test hoặc WAR build rằng luồng HTTP thực tế đã đạt.

## 1. Phạm vi Sprint 6

Hoàn chỉnh trang `/admin/category` cho ADMIN với List, Add, Edit, Delete, Search, Sort và Pagination. Danh sách phải hiển thị ID, Name, Code, Created Date, Modified Date, Actions theo REQ-CATEGORY-002. Add/Edit giữ validation Name/Code không rỗng, Code không trùng. Delete phải báo rõ khi Category đã được News sử dụng và không để dữ liệu mồ côi.

Đã có từ sprint trước: `CategoryModel`, `CategoryMapper`, `ICategoryDAO`/`CategoryDAO`, `ICategoryService`/`CategoryService`, `CategoryController`, hai JSP quản trị, unique constraint `category.code`, FK `news.category_id`, kiểm tra role bằng Filter và CSRF cho POST. Sprint 6 mở rộng các lớp này, không tạo pipeline Category thứ hai.

Ngoài phạm vi: News CRUD và public Category page, User/Role CRUD, thay đổi schema `category`, thay đổi `GenericDAO` nếu không có lỗi thực tế cần sửa. Search theo Name/Code là quyết định của Sprint 6; ví dụ sort theo Title trong REQ-SORT-001 dành cho News, không áp nguyên xi cho Category.

## 2. Hợp đồng trang danh sách

GET `/admin/category` hoặc `?action=list` nhận các query parameter:

| Parameter | Giá trị hợp lệ | Mặc định / khi sai |
|---|---|---|
| `search` | Chuỗi đã trim, tối đa 100 ký tự; tìm trong `name` và `code` | Rỗng = tất cả Category; giá trị quá dài được báo validation có kiểm soát |
| `sortName` | `id`, `name`, `code`, `createdDate` | `id` |
| `sortBy` | `asc`, `desc` | `asc` |
| `page` | Số nguyên dương | `1` |

Page size cố định 10 trong Sprint 6. Không nhận `maxPageItem` tuỳ ý từ client để tránh truy vấn quá lớn; vẫn dùng tên `pageSize`/`maxPageItem` trong model theo ví dụ tài liệu. Khi thay search hoặc sort trên UI, reset `page=1`. Khi page lớn hơn số trang sau lọc, đưa về trang cuối; tập rỗng hiển thị page 1, 0 kết quả và không có Previous/Next. Dùng secondary sort `id` để thứ tự ổn định nếu nhiều hàng trùng Name/Created Date.

Search là contains, không phân biệt hoa thường theo collation MySQL hiện có. Ký tự `%`, `_`, `!` trong input được escape để được tìm như ký tự thường: SQL dùng `LIKE ? ESCAPE '!'`. Cùng một điều kiện WHERE và parameter phải được áp dụng cho cả truy vấn dữ liệu lẫn `COUNT(*)`.

## 3. Thiết kế lớp và luồng dữ liệu

1. Tạo value object `CategoryListCriteria` chứa `search`, `sortName`, `sortBy`, `page`; chuẩn hoá một lần ở Service hoặc factory. Không lưu SQL fragment do client gửi.
2. Tạo model phân trang tái sử dụng `PageResult<T>` gồm `items`, `page`, `pageSize`, `totalItems`, `totalPages`, `hasPrevious`, `hasNext`. Model không phụ thuộc Servlet/DAO để Sprint 7 dùng lại cho News.
3. Bổ sung `ICategoryDAO`/`CategoryDAO` với `findPage(criteria, offset, limit)` và `countBySearch(search)` hoặc hợp đồng tương đương. DAO sở hữu SQL; `GenericDAO` tiếp tục sở hữu `Connection`, `PreparedStatement`, `ResultSet` và vòng đời đóng resource. Không khởi tạo connection trong CategoryDAO.
4. Bổ sung `ICategoryService`/`CategoryService` với `PageResult<CategoryModel> search(CategoryListCriteria criteria)`; Service validate/normalize input, gọi count, tính totalPages và offset bằng phép toán `long`, clamp page, sau đó lấy đúng một trang dữ liệu. Giữ `findAll()` hiện có để không phá vỡ call site và test cũ trong lúc chuyển đổi.
5. `CategoryController` parse request, gọi Service, đặt `pageResult` và `criteria` lên request, forward JSP. Controller không viết SQL, không tự sort/filter một danh sách đã load toàn bộ từ DB.
6. `list.jsp` hiển thị search form, sort controls, total count, thông báo khi chưa có Category và phân trang Previous / số trang / Next. Tạo link bằng JSTL `c:url` + `c:param` để giữ search/sort khi đổi trang và encode query parameter. Dùng `c:out` cho dữ liệu Category và input phản chiếu từ request.

SQL dự kiến:

```sql
SELECT id, name, code, created_date, modified_date
FROM category
WHERE name LIKE ? ESCAPE '!' OR code LIKE ? ESCAPE '!'
ORDER BY <whitelisted_column> <whitelisted_direction>, id ASC
LIMIT ?, ?;

SELECT COUNT(*)
FROM category
WHERE name LIKE ? ESCAPE '!' OR code LIKE ? ESCAPE '!';
```

Khi `search` rỗng, bỏ WHERE và bind tương ứng. Chỉ cột và hướng sort đã map từ whitelist server-side mới được ghép vào SQL; mọi dữ liệu người dùng và `LIMIT` được bind qua `GenericDAO`. Không đưa trực tiếp `sortName`/`sortBy` vào chuỗi SQL.

## 4. Hoàn thiện CRUD và xử lý lỗi

- Add/Edit: giữ trim và kiểm tra rỗng/độ dài ở Service. Kiểm tra code trùng trước khi ghi để có thông báo rõ. Đồng thời nhận diện lỗi unique constraint từ MySQL ở lúc `INSERT`/`UPDATE` để xử lý race condition giữa hai request; chỉ map đúng lỗi duplicate `category.code` thành `ValidationException`, lỗi DB khác phải được ném tiếp.
- Edit: ID không tồn tại trả về danh sách với thông báo. Khi update validation fail, giữ Name/Code và CSRF token trong form.
- Delete: giữ pre-check `countNewsByCategoryId`; FK `ON DELETE RESTRICT` là lớp bảo vệ cuối. Nếu một News được thêm giữa pre-check và DELETE, map đúng lỗi FK thành thông báo “Category đang được sử dụng”; không coi mọi `DatabaseException` là lỗi validation. Không xoá News tự động.
- POST create/update/delete luôn đi qua `AuthorizationFilter` và kiểm tra CSRF trước Service; GET không đổi dữ liệu. Sau thành công dùng redirect về list (PRG) để refresh không gửi lại form.
- Xử lý trường hợp tên/code có ký tự HTML và search có ký tự `%`, `_`, `!` bằng escaping phù hợp ở SQL/JSP.

## 5. Test và kiểm chứng

| Cấp | Case bắt buộc |
|---|---|
| Unit Service | Search rỗng/có giá trị, normalize, sort whitelist/fallback, page 0/sai/overflow, page vượt tổng, tập rỗng, offset đúng |
| Unit Controller | Truyền đúng criteria, forward list, giữ query trên UI, validation error và CSRF token sau POST |
| DAO + MySQL | Search theo Name/Code, literal `%`/`_`/`!`, count và danh sách cùng filter, ASC/DESC ổn định, `LIMIT ?, ?`, không SQL injection qua sort |
| CRUD + MySQL | Add/Edit/Delete thành công, Name/Code rỗng, code trùng, FK ngăn xoá Category đang có News, không tạo dữ liệu mồ côi |
| Security | Anonymous/USER bị chặn với GET và POST admin; ADMIN hợp lệ được qua; POST thiếu/sai CSRF trả 403, không thay đổi DB |

Integration test tạo fixture riêng bằng code có namespace test, dọn đúng ID đã tạo trong `@AfterEach`/finally; không giả định seed `java`, số lượng Category hoặc ID cố định. News fixture cần `created_by` là user test riêng hoặc account fixture hợp lệ. Không chạy test song song trên cùng fixture mutable. Có thể giữ seed nguyên trạng nhưng test phải pass khi seed Category bị sửa hoặc thiếu.

Lệnh verify:

```powershell
mvn test
mvn clean package -DrunDbTests=true
```

Sau build, smoke test Tomcat: ADMIN tìm theo Name/Code, sort cả hai chiều, đi trang đầu/cuối, tìm rỗng, sửa/tạo/xoá Category, thử xoá Category đang có News, refresh sau POST; USER và anonymous không vào `/admin/category`. Xác nhận response status và DB state sau những request thay đổi dữ liệu. Không chạy `schema.sql` để “làm sạch” database có dữ liệu cần giữ.

## 6. Thứ tự thực hiện và Definition of Done

1. Hoàn tất gate Sprint 5 ở mục 0.
2. Thêm criteria và `PageResult<T>`; test biên cho parsing/pagination.
3. Thêm query tìm kiếm, sort whitelist, count và pagination ở CategoryDAO/Service; MySQL integration test.
4. Kết nối Controller/JSP, link giữ filter/sort và thông báo khi chưa có Category.
5. Hoàn thiện xử lý duplicate/FK race cho CRUD; kiểm tra CSRF/role regression.
6. Chạy full test, build WAR và smoke test trên Tomcat.

Sprint 6 hoàn thành khi ADMIN dùng được đủ List/Add/Edit/Delete/Search/Sort/Pagination theo REQ-CATEGORY-001..005; category đang được News tham chiếu không bị xoá; mọi input vào SQL được bind hoặc whitelist; search/sort/page hoạt động cùng nhau và giữ trạng thái qua link; pagination xử lý trang đầu/cuối/rỗng/vượt tổng; các test bắt buộc và smoke test pass, không còn test phụ thuộc category seed `java`.
