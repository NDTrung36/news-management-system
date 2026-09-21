Sprint 4 — Authentication: implementation plan

Ngày lập: 18/09/2026. Baseline: source hiện tại và review Sprint 3 đã hoàn thành. Sprint 3 đạt scope Category CRUD; các finding về xử lý constraint, controller test và cấu hình build là backlog cải thiện, không phải blocker để bắt đầu Authentication. Lượt lập plan không chạy lại test.

Mục tiêu Sprint 4 là thay login mock bằng authentication thật qua MySQL và hoàn thành Register, Login, Logout, session, password hash. Căn cứ: [Sprint 4 trong assignment](../News%20Management%20System%20Internship%20Assignment.md), REQ-AUTH-001/002/003. Giữ stack hiện tại: Java 8+, Servlet 4 dùng `javax.servlet`, JSP/JSTL, JDBC, Maven WAR. AuthorizationFilter, kiểm tra ADMIN và trang 403 theo role thuộc Sprint 5.

Hiện trạng cần thay đổi: `LoginController` chấp nhận mọi credential không rỗng; `HomeController` nhận diện user bằng session attribute `username`. Schema đã có `user`, `role`, `user_role`, unique username/email và cột password dài 255 ký tự. `GenericDAO` hiện mở connection riêng cho từng operation nên chưa thể dùng hai lời gọi độc lập để tạo User và role một cách atomic.

Endpoint và hành vi dự kiến:

| Request | Hành vi |
|---|---|
| `GET /register` | Hiển thị form Username, Password, Confirm Password, Full Name, Email |
| `POST /register` | Validate, hash password, tạo User cùng role USER, redirect `/login?registered=1` |
| `GET /login` | Hiển thị form và thông báo đăng ký thành công nếu có |
| `POST /login` | Kiểm tra credential/status; thành công tạo session authentication mới và redirect `/home` |
| `POST /logout` | Xóa session authentication và redirect `/login` |
| `GET /logout` | Trả 405; thao tác logout thực hiện bằng POST |
| `GET /home` | Hiển thị user từ authentication session hoặc Guest |

Bước 1 — Chốt model, input và contract

Tạo `UserModel extends AbstractModel`, `UserMapper`, `IUserDAO`/`UserDAO`, `IAuthService`/`AuthService`. Các dependency được truyền qua constructor theo cách đang dùng trong Category. Controller có constructor không tham số để Servlet container khởi tạo và constructor injection để test.

| Dữ liệu | Contract |
|---|---|
| `UserModel` | `username`, `passwordHash`, `fullName`, `email`, `status`; audit fields kế thừa |
| Mapping database | `passwordHash` đọc/ghi cột `password`; không thêm cột schema |
| `RegisterForm` | Năm input đúng assignment; Confirm Password chỉ dùng validation, không persist |
| `AuthenticatedUser` | Object immutable chứa `id`, `username`, `fullName`; có thể Serializable, không chứa password/hash |
| `IUserDAO` | `findByUsername`, `findByEmail`, `insertWithDefaultRole` |
| `IAuthService` | `register(RegisterForm)` trả ID; `login(username, password)` trả `AuthenticatedUser` hoặc lỗi authentication |

`UserMapper` chỉ map các cột thật trong bảng `user`. `RegisterForm` chỉ tồn tại trong quá trình xử lý request. Khi forward form lỗi, chỉ giữ username/fullName/email; không đưa password hoặc confirmPassword vào request attributes, session, HTML hoặc log.

Bước 2 — Password hashing

Tạo `PasswordHasher` với `hash` và `verify`, cùng implementation `BCryptPasswordHasher`. Chọn dependency `at.favre.lib:bcrypt:0.10.2`; thư viện hỗ trợ hash/verify BCrypt và bytecode Java 7, dùng được với Java 8. Version được pin thay vì lấy latest tự động. Nguồn: [README thư viện](https://github.com/patrickfav/bcrypt), [changelog 0.10.2](https://github.com/patrickfav/bcrypt/blob/main/CHANGELOG).

Chọn BCrypt để tương thích hash `$2a$10$...` đang có trong seed. Hash mới dự kiến cost 12, salt để thư viện tự sinh; đo thời gian hash/verify trên máy chạy trước khi chốt cost. Đây là lựa chọn tương thích hiện trạng: OWASP ưu tiên Argon2id cho hệ thống mới, còn BCrypt có giới hạn input 72 byte và work factor tối thiểu 10. Nguồn: [OWASP Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html).

Password tối thiểu 6 ký tự theo assignment, tối đa 72 byte UTF-8 cho lựa chọn BCrypt. Không trim, truncate hoặc tự pre-hash password; xác thực bằng API verify của thư viện, không hash lại rồi so sánh chuỗi. Username/fullName/email được trim riêng. Hash lỗi định dạng phải được phát hiện và ghi nhận phía server; không trả hash hoặc chi tiết kỹ thuật về trình duyệt.

Bước 3 — Persistence và transaction đăng ký

SQL nằm trong `UserDAO`, dùng placeholder và quote bảng `user`. Unique constraints hiện có là lớp bảo vệ cuối cùng cho duplicate username/email; các pre-check trong Service chỉ giúp hiển thị validation sớm. Duplicate semantics phải khớp collation thật của MySQL, bao gồm test khác biệt chữ hoa/chữ thường.

`insertWithDefaultRole` thực hiện trong cùng connection:

```text
BEGIN
  INSERT user với password hash và status = 1
  Lấy generated user ID
  INSERT user_role với role có code = USER
  Xác nhận đúng một mapping đã được tạo
COMMIT
```

Nếu role USER không tồn tại hoặc bất kỳ bước ghi nào thất bại, rollback toàn bộ. Người đăng ký không được truyền role hoặc status để thay đổi giá trị mặc định của server. Việc gán role USER chuẩn bị dữ liệu cho Sprint 5, chưa thực hiện kiểm tra quyền admin ở Sprint 4.

Thay đổi framework cần thiết: toàn bộ vòng đời JDBC được tập trung trong `GenericDAO`. Contract `IGenericDAO` không public overload nhận `Connection`; thay vào đó cung cấp `executeInTransaction(...)`. Callback nhận một `IGenericDAO` transaction-scoped để chạy nhiều câu SQL trên cùng connection mà không làm lộ connection ra DAO nghiệp vụ. `GenericDAO` sở hữu việc mở connection, commit/rollback, đóng connection và đóng statement/result set bằng try-with-resources. `CategoryDAO` và `UserDAO` kế thừa `GenericDAO`, chỉ truyền SQL, mapper và tham số; không import `DatabaseUtil` hoặc `java.sql.Connection`. Service cũng không nhận connection.

Chỉ chuyển lỗi duplicate-key đã nhận diện thành `DuplicateUserException`, sau đó Service đổi thành validation phù hợp. Trường hợp hai request cùng vượt qua pre-check vẫn phải trả thông báo duplicate, không thành lỗi 500. Các lỗi kết nối/SQL khác giữ nguyên nguyên nhân; rollback lỗi không được che mất lỗi gốc. Không chạy lại `schema.sql` vì script hiện có DROP TABLE.

Bước 4 — AuthService

Register thực hiện: kiểm tra input → kiểm tra duplicate → hash → DAO tạo user/role atomic → trả ID. Validation gồm username bắt buộc/tối đa 50, fullName bắt buộc/tối đa 150 theo schema, email bắt buộc/format hợp lệ/tối đa 255, password theo policy và confirm trùng khớp. Controller không thực hiện các rule này.

Login thực hiện: kiểm tra input → `findByUsername` → BCrypt verify → kiểm tra `status = 1` → trả `AuthenticatedUser`. Tài khoản không tồn tại, password sai hoặc status bị khóa đều trả cùng thông báo: `Username hoặc password không đúng. Vui lòng kiểm tra và thử lại.` Không tạo identity session nếu authentication thất bại. Với username không tồn tại, có thể verify một dummy hash dùng chung để tránh bỏ qua toàn bộ chi phí BCrypt; không sinh dummy hash mới cho mỗi request.

Lỗi authentication dùng `AuthenticationException`; lỗi nhập liệu dùng `ValidationException`; lỗi database được xử lý như lỗi kỹ thuật, không đổi tất cả thành sai credential. Không log credential, password/hash hoặc session ID.

Bước 5 — Controller, session và view

Thêm `RegisterController`, `LogoutController`; cập nhật `LoginController`, `HomeController`. GET hiển thị view; POST gọi Service và dùng redirect sau khi ghi thành công. Register thành công chuyển đến login với thông báo cố định; chưa auto-login. Login thành công của mọi user chuyển về `/home`, việc redirect theo role dành cho Sprint 5.

Tạo helper nhỏ `AuthSession` dùng chung cho Login, Logout và Home. Session key thống nhất `AUTH_USER`, value là `AuthenticatedUser`. Sau login thành công, invalidate session cũ và tạo session mới để đổi session ID và loại bỏ dữ liệu authentication cũ. Đây là bước chống session fixation theo [OWASP Session Management](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html).

Logout dùng `getSession(false)` rồi invalidate nếu có; session đã hết hạn vẫn redirect về login và không tạo session authentication mới. Home chỉ đọc AUTH_USER, không tin attribute username của mock cũ. Các session mock đang tồn tại cần login thật lại sau thay đổi này.

Cấu hình idle timeout dự kiến 30 phút, cookie HttpOnly, session tracking bằng cookie trong `WEB-INF/web.xml`; Secure bật khi môi trường deploy dùng HTTPS. Servlet routing tiếp tục dùng annotation hiện có.

Thêm `register.jsp`; cập nhật `login.jsp`, `home.jsp` với link Register/Login và form POST Logout. Form có required/maxlength tương ứng, nhưng validation server-side vẫn bắt buộc. Nội dung động dùng `c:out`; input password luôn trống khi render lại. UTF-8 được đặt trước khi đọc parameters.

Tạo `CsrfTokenManager` dùng SecureRandom và token gắn với session cho ba form POST Register/Login/Logout; token sai hoặc thiếu trên session đang hoạt động trả 403. Sau login tạo token mới cùng session mới. Đây là synchronizer token pattern cho các form dùng session, theo [OWASP CSRF Prevention](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html).

Bước 6 — Kiểm chứng

| Nhóm | Các case cần pass |
|---|---|
| PasswordHasher | Verify đúng/sai, cùng password sinh hash khác nhau, password chứa khoảng trắng/Unicode, biên 72 byte, hash malformed |
| AuthService Register | Thiếu input, vượt độ dài, email sai, confirm sai, duplicate username/email, status mặc định, không lưu plaintext |
| AuthService Login | User không tồn tại, password sai, status bị khóa, thành công; các failure thông thường cùng thông báo |
| DAO/MySQL | Mapping user, generated ID, user và role được tạo cùng nhau; thiếu role/lỗi mapping rollback không để lại user; duplicate được nhận diện |
| Controller/session | Forward lỗi giữ field an toàn, redirect thành công, session ID thay đổi, session không chứa credential/hash, logout/expired session, CSRF sai |
| Regression | Toàn bộ test Sprint 1–3 và Category CRUD vẫn hoạt động sau khi tập trung resource/transaction ownership vào GenericDAO |
| JDBC resource | Connection, statement và result set đều đóng ở success/failure; transaction dùng đúng một connection, commit khi thành công và rollback khi lỗi |
| Tomcat | Register → Login → Home → Logout → Home hiển thị Guest; route/JSP/session cookie đúng ở context path của WAR |

Service test dùng fake DAO và fake hasher để kiểm tra business rule nhanh; PasswordHasher test dùng BCrypt thật. Controller/session test dùng Servlet mocks tương thích Java 8 và JDK chạy build. DB integration test opt-in theo `runDbTests`; tạo fixture với username/email riêng cho từng run, cleanup bằng ID đã tạo, không xóa dữ liệu seed hoặc dựa vào password seed chưa được xác nhận.

Lệnh verify sau implementation:

```powershell
mvn test
mvn test -DrunDbTests=true
mvn clean package
```

Nếu chạy trên JDK 9+, chốt `maven.compiler.release=8` và kiểm tra runtime/dependency phù hợp trước khi đổi cấu hình build. WAR đóng gói thành công chưa đủ để kết luận JSP hoạt động; cần smoke test trên Tomcat tương thích `javax.servlet`.

Bước 7 — Thứ tự commit và điều kiện hoàn thành

1. `feat: add user model mapper and auth contracts`
2. `feat: add bcrypt password hashing`
3. `feat: add transactional user registration persistence`
4. `feat: implement authentication service`
5. `feat: implement register login logout and auth session`
6. `test: verify authentication and registration rollback`

Sprint 4 hoàn thành khi tài khoản đăng ký được lưu dưới dạng hash, có status active và role USER; login mock bị thay hoàn toàn; credential đúng tạo identity session mới; credential sai/khóa không đăng nhập; logout/timeout xóa identity; form và thông báo hoạt động trên Tomcat; toàn bộ kiểm chứng liên quan pass. AuthorizationFilter và kiểm tra ADMIN/USER triển khai ở Sprint 5, dựa trên identity session và user_role đã chuẩn bị.
