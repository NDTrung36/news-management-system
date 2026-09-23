# Sprint 5 — Authorization: implementation plan

Ngày lập: 23/09/2026. Baseline: Sprint 4 đã hoàn thành Register, Login, Logout, session, BCrypt và refactor JDBC resource ownership. Lần kiểm chứng gần nhất chạy clean build cùng MySQL với 40 test, không có failure, error hoặc skipped. Lượt lập plan không chạy lại test vì không thay đổi source code.

Mục tiêu Sprint 5 là bổ sung role và authorization server-side cho toàn bộ `/admin/*`. Căn cứ: REQ-AUTH-004, REQ-AUTH-005, REQ-SECURITY-004, REQ-FILTER-001 và Sprint 5 trong assignment. Stack giữ nguyên Java 8, Servlet 4 `javax.servlet`, JSP/JSTL, JDBC, MySQL và Maven WAR.

Sprint 5 hoàn thành khi hệ thống phân biệt rõ ba trường hợp:

| Trạng thái request đến `/admin/*` | Kết quả |
|---|---|
| Chưa đăng nhập hoặc session đã hết hạn | Redirect đến `/login` |
| Đã đăng nhập nhưng không có role `ADMIN` | Trả HTTP 403 và hiển thị trang 403 |
| Có role `ADMIN` | Cho request đi tiếp đến controller |

Authorization phải chạy trên server cho cả URL truy cập trực tiếp, GET và POST. Việc ẩn link hoặc button trên JSP chỉ phục vụ trải nghiệm sử dụng, không thay thế Filter.

## 1. Phạm vi và quyết định kiến trúc

Trong phạm vi Sprint 5:

- Thêm `RoleModel`, `RoleMapper`, `IRoleDAO`/`RoleDAO` và `IRoleService`/`RoleService`.
- Tải role từ `role` và `user_role` khi login thành công.
- Mở rộng `AuthenticatedUser` để giữ tập role code immutable, không chứa password/hash.
- Thêm `AuthorizationFilter` bảo vệ `/admin/*`.
- Thêm trang 403 và cấu hình error handling liên quan.
- Bảo vệ các form POST của Category bằng CSRF đã có từ Sprint 4.
- Bổ sung test cho role loading, Filter, 403, CSRF và regression.

Ngoài phạm vi Sprint 5:

- User Management và Role Management CRUD.
- Giao diện gán hoặc thu hồi role.
- Bảng `permission`, `role_permission` hoặc permission động trong database.
- Admin dashboard hoàn chỉnh.
- Authorization cho Comment, News và Upload chưa được implement ở các sprint sau.

Assignment chỉ định schema `role` và `user_role`, không định nghĩa bảng permission. Vì vậy permission trong Sprint 5 được biểu diễn bằng policy server-side: URL `/admin/*` yêu cầu role `ADMIN`. Thiết kế không hardcode quyền trong JSP hoặc controller; Filter chịu trách nhiệm enforcement cho nhóm URL này.

## 2. Hiện trạng và khoảng trống

Database đã có:

- Bảng `role` với unique `code`.
- Bảng `user_role` có primary key `(user_id, role_id)` và foreign key đầy đủ.
- Seed role `ADMIN`, `USER` và mapping cho hai tài khoản mẫu.
- User đăng ký ở Sprint 4 được gán role `USER` trong cùng transaction.

Source hiện tại còn thiếu:

- Chưa có Role model, mapper, DAO và service.
- `AuthenticatedUser` mới có `id`, `username`, `fullName`.
- `AuthService.login(...)` chưa tải role.
- `/admin/category` đang truy cập được mà không qua Filter.
- Chưa có trang 403.
- Form create/update/delete Category chưa dùng CSRF token.

Không thay đổi `schema.sql` trong Sprint 5. Không chạy trực tiếp script này trên database hiện tại vì script có `DROP TABLE`.

## 3. Authorization matrix

| URL pattern | Anonymous | `USER` | `ADMIN` | Enforcement |
|---|---:|---:|---:|---|
| `/login`, `/register`, `/home`, `/403` | Cho phép | Cho phép | Cho phép | Controller hiện có |
| `/logout` POST | Không có identity để logout | Cho phép | Cho phép | Controller + CSRF |
| `/admin/*` | Redirect `/login` | HTTP 403 | Cho phép | `AuthorizationFilter` |
| Public News/Category ở sprint sau | Cho phép | Cho phép | Cho phép | Không thuộc Filter admin |
| Comment ở sprint sau | Chưa chốt | Dự kiến cho phép | Dự kiến cho phép | Chốt khi implement Comment |

`ADMIN` được xem là role quản trị. Không yêu cầu tài khoản ADMIN đồng thời phải có row role `USER`; các chức năng chung dành cho người dùng phải chấp nhận cả hai role khi được implement.

## 4. Bước 1 — Role domain và constants

Tạo các thành phần:

| File | Trách nhiệm |
|---|---|
| `model/RoleModel.java` | Chứa `id`, `code`, `name`; không thêm field không tồn tại trong schema |
| `mapper/RoleMapper.java` | Map `id`, `code`, `name` từ `ResultSet` |
| `security/RoleCodes.java` | Constants `ADMIN`, `USER`; không rải string literal trong Filter và service |

`RoleModel` có thể kế thừa `AbstractModel` để thống nhất model hiện tại, nhưng `RoleMapper` chỉ map các cột thực tế của bảng `role`; `createdDate` và `modifiedDate` để `null` vì schema role không có hai cột này.

Không dùng enum để parse toàn bộ role từ database. Tập role trong session giữ string code nhằm không vô tình bỏ qua role mới; authorization chỉ cấp quyền cho code đã được policy nhận diện rõ ràng. Role lạ không được suy diễn thành ADMIN.

## 5. Bước 2 — Role DAO và Service

Tạo `IRoleDAO` và `RoleDAO extends GenericDAO` với contract tối thiểu:

```text
List<RoleModel> findByUserId(Long userId)
RoleModel findByCode(String code)
```

`findByUserId` dùng JOIN có parameter:

```sql
SELECT r.id, r.code, r.name
FROM role r
JOIN user_role ur ON ur.role_id = r.id
WHERE ur.user_id = ?
ORDER BY r.id
```

`RoleDAO` chỉ chứa SQL, mapper và tham số. Không import `DatabaseUtil`, `Connection`, `PreparedStatement` hoặc `ResultSet`; resource ownership tiếp tục nằm trong `GenericDAO` theo refactor Sprint 4.

Tạo `IRoleService`/`RoleService`:

```text
Set<String> findRoleCodesByUserId(Long userId)
```

Service thực hiện:

- Reject `null` hoặc ID không dương trước khi gọi DAO.
- Chuẩn hóa code bằng `trim` và uppercase `Locale.ROOT`.
- Loại bỏ code rỗng và duplicate.
- Trả `Set` immutable, không để caller sửa role đã tải.
- Không tự cấp role mặc định nếu database không trả kết quả.

Nếu user không có role do dữ liệu sai, kết quả là tập rỗng và authentication phải fail-closed ở bước login; không tự suy đoán `USER`.

## 6. Bước 3 — Gắn role vào authentication session

Mở rộng `AuthenticatedUser`:

```text
id
username
fullName
roleCodes: Set<String>
hasRole(String roleCode): boolean
```

Constructor phải defensive-copy và bọc immutable collection. Class tiếp tục `final`, `Serializable` và không chứa password/hash. `hasRole` xử lý `null` an toàn và so sánh theo role code đã chuẩn hóa.

Cập nhật `AuthService`:

1. Validate username/password.
2. Tìm user và verify BCrypt.
3. Kiểm tra `status = 1`.
4. Gọi `IRoleService.findRoleCodesByUserId(userId)`.
5. Nếu không có role, trả cùng thông báo authentication chung và không tạo session.
6. Tạo `AuthenticatedUser` gồm identity và role codes.

Default constructor tạo `UserDAO`, `RoleService` và `BCryptPasswordHasher`. Constructor injection phục vụ test được đổi thành ba dependency: `IUserDAO`, `IRoleService`, `PasswordHasher`. Toàn bộ test và call site hiện tại phải được cập nhật; không để unit test gọi database thật.

Role là snapshot tại thời điểm login. Thay đổi role trong database có hiệu lực sau lần login tiếp theo hoặc khi session hết hạn tối đa 30 phút. Đây là trade-off phù hợp scope hiện tại vì chưa có Role Management. Khi bổ sung thao tác thu hồi role, cần invalidate session liên quan hoặc revalidate role trên request để việc thu hồi có hiệu lực ngay.

## 7. Bước 4 — AuthorizationFilter

Tạo `filter/AuthorizationFilter.java`, map `urlPatterns = "/admin/*"` và chỉ xử lý `DispatcherType.REQUEST`.

Luồng xử lý:

```text
Request /admin/*
        ↓
Đọc AUTH_USER qua AuthSession
        ↓
Không có identity ───────────→ redirect contextPath + /login
        ↓
Có identity nhưng thiếu ADMIN → sendError(403)
        ↓
Có ADMIN ────────────────────→ chain.doFilter(request, response)
```

Yêu cầu implementation:

- Dùng `request.getContextPath()` khi redirect.
- Không tạo session mới trong Filter.
- Không query parameter để quyết định role.
- Không tin hidden field, cookie tự tạo hoặc dữ liệu từ frontend.
- Không gọi controller khi authentication/authorization thất bại.
- Không redirect về URL do client truyền để tránh open redirect trong Sprint 5.
- Có constructor không tham số cho Servlet container và constructor injection `AuthSession` cho unit test.

Filter kiểm tra mọi method dưới `/admin/*`, nên POST create/update/delete Category cũng bị chặn khi gọi URL trực tiếp.

## 8. Bước 5 — Trang 403 và error handling

Tạo:

- `controller/web/ForbiddenController.java` map `/403`.
- `WEB-INF/views/403.jsp`.
- Error mapping HTTP 403 trong `WEB-INF/web.xml` nếu container cần route `sendError(403)` đến trang thân thiện.

`ForbiddenController` luôn đặt status `HttpServletResponse.SC_FORBIDDEN`, sau đó forward đến JSP. JSP chỉ hiển thị thông báo không có quyền và link về `/home`; không hiển thị role nội bộ, stack trace hoặc thông tin policy.

`/403` không nằm dưới `/admin/*`, tránh redirect loop. Error dispatch không được Filter chặn lại.

## 9. Bước 6 — UI và CSRF cho admin Category

Cập nhật `HomeController`/`home.jsp`:

- Đặt attribute `isAdmin` từ `AuthenticatedUser.hasRole(RoleCodes.ADMIN)`.
- Chỉ hiển thị link quản trị Category cho ADMIN.
- Vẫn xem Filter là enforcement chính; việc ẩn link không phải authorization.

Cập nhật `CategoryController` và hai JSP admin:

- GET list/form tạo CSRF token bằng `CsrfTokenManager` và truyền vào view.
- Form create/update/delete gửi hidden field `_csrf`.
- POST thiếu hoặc sai token trả 403 trước khi gọi Service.
- Không đổi business logic Category trong Sprint 5.

CSRF là phần hardening cần thiết vì route admin có thao tác thay đổi dữ liệu. Authorization xác định ai được phép thao tác; CSRF xác nhận request POST xuất phát từ session hợp lệ. Hai cơ chế không thay thế nhau.

## 10. Bước 7 — Kiểm chứng

### Unit test

| Nhóm | Case cần pass |
|---|---|
| `RoleServiceTest` | ID invalid, không có role, duplicate role, normalize code, immutable result |
| `AuthServiceTest` | Login USER, login ADMIN, user không có role, wrong password, locked user, thông báo lỗi không tiết lộ chi tiết |
| `AuthorizationFilterTest` | Anonymous redirect login; USER nhận 403; ADMIN gọi chain đúng một lần; expired session; context path đúng |
| `AuthenticatedUserTest` | Defensive copy, collection immutable, `hasRole` đúng/sai/null |
| `ForbiddenControllerTest` | Status 403 và forward đúng JSP |
| `CategoryController` security | POST thiếu/sai CSRF bị chặn; token đúng mới gọi Service |

Filter test dùng Servlet proxy/mock nhẹ tương thích Java 8 như test session hiện có. Test phải assert cả response lẫn việc `FilterChain` có hoặc không được gọi; chỉ kiểm tra status là chưa đủ.

### Integration test với MySQL

| Case | Bằng chứng |
|---|---|
| User đăng ký | Có đúng role `USER` |
| Login user | `AuthenticatedUser.roleCodes` chứa `USER`, không chứa `ADMIN` |
| Login admin fixture | Role codes chứa `ADMIN` |
| Role DAO | JOIN trả đúng role theo `user_id` |
| User không có role | Không tạo authenticated session |
| Regression | Category CRUD, authentication và GenericDAO transaction vẫn pass |

Fixture phải dùng username/email riêng, tạo mapping role bằng business key và cleanup theo ID. Không phụ thuộc password của seed account và không xóa role hoặc user seed.

### Tomcat smoke test

1. Mở `/admin/category` khi chưa login → chuyển đến `/login`.
2. Login bằng tài khoản role USER → truy cập trực tiếp `/admin/category` → trang 403, HTTP status 403.
3. Login bằng tài khoản role ADMIN → truy cập `/admin/category` → xem được danh sách.
4. ADMIN create/update/delete Category với CSRF hợp lệ → thành công.
5. Gửi POST admin thiếu CSRF → HTTP 403, dữ liệu không thay đổi.
6. Logout rồi gọi lại `/admin/category` → chuyển đến `/login`.
7. Kiểm tra URL dưới context path của WAR, không hardcode `/news-management-system` trong Java/JSP.

Tài khoản ADMIN dùng cho smoke test phải là fixture/dev account có password đã biết và BCrypt hợp lệ; không đưa plaintext password vào source hoặc plan.

### Lệnh verify

```powershell
mvn test
mvn test -DrunDbTests=true
mvn clean package -DrunDbTests=true
```

Sau build, kiểm tra WAR có Filter class, Role classes, `403.jsp` và `web.xml`. Build WAR thành công chưa thay thế Tomcat smoke test.

## 11. Thứ tự implementation và commit

1. `feat(role): add role model mapper dao and service`
2. `feat(auth): load user roles into authenticated session`
3. `feat(authz): protect admin routes with authorization filter`
4. `feat(web): add forbidden page and admin navigation`
5. `fix(security): add csrf protection to admin category forms`
6. `test(authz): verify role loading filters and access denial`

Mỗi commit phải build được hoặc có dependency rõ ràng với commit liền trước. Không trộn Role Management CRUD vào chuỗi commit Sprint 5.

## 12. Definition of Done

Sprint 5 hoàn thành khi:

- Role được đọc từ MySQL qua đầy đủ Mapper → DAO → Service.
- Login tạo `AuthenticatedUser` immutable có role codes; session không chứa credential/hash.
- Anonymous không truy cập được bất kỳ `/admin/*` nào.
- USER không truy cập được `/admin/*`, kể cả gọi URL hoặc POST trực tiếp.
- ADMIN truy cập được `/admin/category` và các request hợp lệ đi qua Filter.
- Mọi authorization decision được thực hiện server-side.
- Trang 403 trả đúng HTTP status và không tạo redirect loop.
- POST Category được bảo vệ bởi cả authorization và CSRF.
- Test unit, MySQL integration, regression và clean package đều pass.
- Luồng Anonymous → USER → ADMIN → Logout được smoke test trên Tomcat tương thích Servlet 4 (`javax.servlet`).
