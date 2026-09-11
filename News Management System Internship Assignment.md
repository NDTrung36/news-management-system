# **ĐỀ BÀI THỰC TẬP SINH** 

## **XÂY DỰNG WEBSITE TIN TỨC BẰNG JAVA WEB – SERVLET/JSP/JDBC** 

# **1. THÔNG TIN CHUNG** 

### **1.1. Tên project** 

**News Management System** 

### **1.2. Mục tiêu** 

Xây dựng một website quản lý và đọc tin tức bằng Java Web. 

Hệ thống gồm 2 khu vực: 

- Website dành cho người dùng. 

- Website quản trị dành cho Admin. 

Project phải được xây dựng độc lập, có kiến trúc và chức năng tương đương một hệ thống Java Web thực tế. 

### **1.3. Mục tiêu đào tạo** 

Sau khi hoàn thành project, thực tập sinh phải hiểu và sử dụng được: 

- Java Core • OOP • Servlet • JSP • JSTL • HTTP Request/Response • Session • Cookie • Filter • JDBC • MySQL • MVC • DAO • Service • Authentication • Authorization • Pagination • Sorting • Searching • File Upload • Git 

1 

• Maven • Tomcat • Debugging • Basic Web Security 

# **2. CÔNG NGHỆ** 

## **REQ-TECH-001 — Backend** 

Bắt buộc sử dụng: 

• Java 8+ • Servlet • JSP • JSTL • JDBC • Maven • Apache Tomcat 

Không sử dụng: 

• Spring Boot • Spring MVC • Hibernate • JPA • Spring Data JPA 

Mục tiêu của project là giúp thực tập sinh hiểu cách một Java Web application hoạt động ở mức Servlet/ JDBC. 

## **REQ-TECH-002 — Database** 

Sử dụng: 

- MySQL 

Phải sử dụng JDBC để giao tiếp với database. 

Không sử dụng ORM. 

## **REQ-TECH-003 — Frontend** 

Có thể sử dụng: 

- HTML 

- CSS 

2 

- JavaScript • Bootstrap • jQuery 

Không yêu cầu frontend framework như Angular/React/Vue. 

# **3. KIẾN TRÚC HỆ THỐNG** 

## **REQ-ARCH-001 — Layer Architecture** 

Project phải được tổ chức theo mô hình: 

```
Browser
   |
   v
Controller / Servlet
   |
   v
Service
   |
   v
DAO
   |
   v
JDBC
   |
   v
MySQL
```

### **Quy tắc** 

Controller: 

- Nhận request. • Validate dữ liệu cơ bản. 

- Gọi Service. 

- Chuẩn bị dữ liệu cho JSP. 

- Redirect/Forward. 

Service: 

- Xử lý business logic. 

- Validate nghiệp vụ. 

- Gọi DAO. 

DAO: 

- Chỉ xử lý database. 

3 

• Không chứa business logic. 

JSP: 

• Chỉ xử lý presentation. • Không chứa Java business logic. 

# **4. CẤU TRÚC PACKAGE** 

Project nên có cấu trúc tương tự: 

```
src/main/java
└── com.example.news
    ├── controller
    │   ├── admin
    │   └── web
    │
    ├── dao
    │   ├── impl
    │   └── ...
    │
    ├── service
    │   ├── impl
    │   └── ...
    │
    ├── model
    │
    ├── mapper
    │
    ├── paging
    │
    ├── sort
    │
    ├── filter
    │
    └── utils
```

Tên package có thể thay đổi nhưng phải giữ nguyên tư tưởng phân tầng. 

# **5. DATABASE DESIGN** 

## **REQ-DB-001 — User** 

Tạo bảng <mark>`user` .</mark> 

4 

Tối thiểu: 

```
id
username
password
full_name
email
status
created_date
modified_date
```

## **REQ-DB-002 — Role** 

Tạo bảng <mark>`role` .</mark> 

```
id
code
name
```

Role tối thiểu: 

```
ADMIN
USER
```

## **REQ-DB-003 — User Role** 

Tạo bảng: 

```
user_role
```

Quan hệ: 

```
User 1 ---- N UserRole N ---- 1 Role
```

## **REQ-DB-004 — Category** 

Tạo bảng: 

5 

```
category
```

Field tối thiểu: 

```
id
name
code
created_date
modified_date
```

## **REQ-DB-005 — News** 

Tạo bảng: 

```
news
```

Field tối thiểu: 

```
id
title
short_description
content
thumbnail
category_id
created_by
created_date
modified_date
```

## **REQ-DB-006 — Comment** 

Tạo bảng: 

```
comment
```

Field: 

```
id
content
user_id
```

6 

```
news_id
created_date
```

## **REQ-DB-007 — Database Constraint** 

Phải sử dụng: 

• Primary Key. • Foreign Key. • Index. • Unique constraint phù hợp. 

Ví dụ: 

```
user.username
user.email
role.code
category.code
```

không được trùng. 

# **6. MODEL** 

## **REQ-MODEL-001** 

Tạo: 

```
AbstractModel
UserModel
RoleModel
CategoryModel
NewModel
CommentModel
```

<mark>`AbstractModel`</mark> chứa các field dùng chung: 

```
id
createdDate
modifiedDate
createdBy
modifiedBy
```

7 

# **7. JDBC / DAO** 

## **REQ-DAO-001 — Generic DAO** 

Xây dựng DAO dùng chung cho các thao tác: 

```
findAll
findOne
insert
update
delete
count
```

## **REQ-DAO-002 — DAO Interface** 

Tối thiểu: 

```
IUserDAO
IRoleDAO
ICategoryDAO
INewDAO
ICommentDAO
```

## **REQ-DAO-003 — DAO Implementation** 

Tối thiểu: 

```
UserDAO
RoleDAO
CategoryDAO
NewDAO
CommentDAO
```

## **REQ-DAO-004 — PreparedStatement** 

Tất cả SQL có dữ liệu từ người dùng phải sử dụng: 

```
PreparedStatement
```

8 

Không được nối chuỗi trực tiếp: 

#### `"... WHERE username = '" + username + "'"` 

Mục tiêu: tránh SQL Injection. 

# **8. ROW MAPPER** 

## **REQ-MAPPER-001** 

Xây dựng cơ chế: 

```
ResultSet
   ↓
RowMapper
   ↓
Model
```

Tối thiểu: 

```
UserMapper
RoleMapper
CategoryMapper
NewMapper
CommentMapper
```

# **9. SERVICE** 

## **REQ-SERVICE-001** 

Tạo: 

```
IUserService
UserService
```

```
IRoleService
RoleService
```

```
ICategoryService
CategoryService
```

9 

```
INewService
NewService
ICommentService
CommentService
```

## **REQ-SERVICE-002** 

Service phải chịu trách nhiệm: 

- Business logic. • Validation nghiệp vụ. • Gọi DAO. • Xử lý dữ liệu trước khi trả về Controller. 

Không được đưa business logic lớn vào Servlet. 

# **10. ĐĂNG KÝ** 

## **REQ-AUTH-001 — Register** 

Tạo màn hình: 

```
/register
```

Form: 

```
Username
Password
Confirm Password
Full Name
Email
```

### **Acceptance Criteria** 

- Username bắt buộc. 

- Username không được trùng. 

- Password bắt buộc. 

- Password tối thiểu 6 ký tự. • Confirm Password phải giống Password. 

- Email đúng format. 

- Email không được trùng. 

- Password phải được hash. 

- Đăng ký thành công thì thông báo cho người dùng. 

10 

# **11. ĐĂNG NHẬP** 

## **REQ-AUTH-002 — Login** 

URL: 

```
/login
```

Form: 

```
Username
Password
```

### **Acceptance Criteria** 

### **Trường hợp đúng:** 

```
Username đúng
Password đúng
        ↓
Login thành công
        ↓
Tạo Session
```

### **Trường hợp sai:** 

```
Username/password sai
        ↓
Hiển thị thông báo lỗi
```

Không được tiết lộ quá chi tiết thông tin authentication. 

# **12. LOGOUT** 

## **REQ-AUTH-003** 

URL: 

```
/logout
```

11 

Khi logout: 

- Xóa authentication session. • Không còn quyền truy cập trang cần đăng nhập. • Redirect về trang login hoặc home. 

# **13. AUTHORIZATION** 

## **REQ-AUTH-004 — Authorization Filter** 

Tạo: 

```
AuthorizationFilter
```

Kiểm tra các URL: 

```
/admin/*
```

### **Chưa đăng nhập** 

```
/admin/*
      ↓
/login
```

### **Đã đăng nhập nhưng không có quyền** 

```
/admin/*
      ↓
/403
```

### **Có quyền** 

```
/admin/*
      ↓
Cho phép request
```

12 

# **14. ROLE / PERMISSION** 

## **REQ-AUTH-005** 

Tối thiểu: 

### **USER** 

Có quyền: 

```
Xem bài viết
Xem category
Comment
```

### **ADMIN** 

Có quyền: 

```
Xem bài viết
Comment
Quản lý User
Quản lý Role
Quản lý Category
Quản lý News
Quản lý Comment
```

Authorization phải được kiểm tra ở server. 

Không được chỉ ẩn menu/button trên frontend. 

# **15. CATEGORY MANAGEMENT** 

## **REQ-CATEGORY-001** 

Admin có thể: 

```
List
Add
Edit
Delete
Search
Sort
Pagination
```

13 

## **REQ-CATEGORY-002 — Category List** 

Hiển thị: 

```
ID
Name
Code
Created Date
Modified Date
Actions
```

## **REQ-CATEGORY-003 — Add Category** 

Form: 

```
Name
Code
```

Validation: 

• Name không rỗng. • Code không rỗng. • Code không được trùng. 

## **REQ-CATEGORY-004 — Edit Category** 

Admin có thể chỉnh sửa: 

```
Name
Code
```

Phải validate dữ liệu giống Add. 

## **REQ-CATEGORY-005 — Delete Category** 

Admin có thể xóa category. 

Phải xử lý trường hợp category đang được sử dụng bởi News. 

14 

Có thể: 

- Không cho xóa và hiển thị thông báo. 

- Hoặc xử lý theo thiết kế database. 

Không được để database phát sinh dữ liệu mồ côi. 

# **16. NEWS MANAGEMENT** 

## **REQ-NEWS-001** 

Admin có thể: 

```
List
Add
Edit
Delete
View
Search
Filter Category
Sort
Pagination
```

# **17. NEWS LIST** 

## **REQ-NEWS-002** 

Hiển thị: 

```
ID
Title
Thumbnail
Category
Created By
Created Date
Actions
```

15 

# **18. NEWS CREATE** 

## **REQ-NEWS-003** 

Form: 

```
Title
Short Description
Content
Thumbnail
Category
```

Validation: 

- Title bắt buộc. 

- Short Description bắt buộc. 

- Content bắt buộc. 

- Category bắt buộc. 

- Thumbnail phải là image hợp lệ. 

# **19. NEWS EDIT** 

## **REQ-NEWS-004** 

Admin có thể chỉnh sửa: 

```
Title
Short Description
Content
Thumbnail
Category
```

Phải giữ nguyên ID bài viết. 

# **20. NEWS DELETE** 

## **REQ-NEWS-005** 

Admin có thể xóa News. 

Nếu News có Comment: 

16 

Phải có phương án xử lý: 

```
CASCADE
```

hoặc: 

```
Không cho xóa
```

hoặc xử lý nghiệp vụ phù hợp. 

# **21. RICH TEXT EDITOR** 

## **REQ-NEWS-006** 

Content của News phải hỗ trợ Rich Text Editor. 

Có thể sử dụng: 

```
CKEditor
```

Các chức năng cơ bản: 

• Bold • Italic • Heading • List • Link • Image • Paragraph 

# **22. FILE UPLOAD** 

## **REQ-UPLOAD-001** 

Xây dựng chức năng upload ảnh. 

API: 

```
POST /api/upload
```

17 

## **REQ-UPLOAD-002** 

Kiểm tra: 

- Extension. 

- MIME type. 

- File size. 

- File name. 

Chỉ cho phép các định dạng image được quy định. 

## **REQ-UPLOAD-003** 

Không lưu file bằng tên gốc nếu có nguy cơ trùng. 

Ví dụ: 

```
original.jpg
```

có thể được đổi thành: 

```
UUID.jpg
```

API trả về: 

```
{
"status":"success",
"url":"/uploads/xxxxx.jpg"
}
```

# **23. WEBSITE USER** 

## **REQ-WEB-001** 

Trang chủ: 

```
/
```

Bao gồm: 

18 

```
Header
Logo
Category Menu
Search
Login/Register
News List
Pagination
Footer
```

# **24. NEWS LIST USER** 

## **REQ-WEB-002** 

User có thể xem: 

```
Thumbnail
Title
Short Description
Category
Created Date
```

# **25. CATEGORY PAGE** 

## **REQ-WEB-003** 

User có thể chọn category. 

Ví dụ: 

```
/category?code=java
```

Chỉ hiển thị News thuộc category đó. 

# **26. NEWS DETAIL** 

## **REQ-WEB-004** 

URL: 

19 

```
/news?id={id}
```

Hiển thị: 

```
Title
Thumbnail
Author
Created Date
Category
Content
Comment
```

# **27. SEARCH** 

## **REQ-SEARCH-001** 

User/Admin có thể tìm kiếm News. 

Có thể tìm theo: 

```
Title
Short Description
```

Ví dụ: 

```
/news?search=java
```

SQL phải sử dụng: 

```
PreparedStatement
```

# **28. PAGINATION** 

## **REQ-PAGING-001** 

Xây dựng pagination reusable. 

Ví dụ: 

20 

```
page = 1
maxPageItem = 10
```

Công thức: 

```
offset = (page - 1) * maxPageItem
```

SQL: 

```
LIMIT ?, ?
```

## **REQ-PAGING-002** 

Pagination phải hiển thị: 

```
Previous
1
2
3
4
5
Next
```

Xử lý đúng: 

- Trang đầu. • Trang cuối. • Không có dữ liệu. • Page vượt quá tổng số trang. 

# **29. SORTING** 

## **REQ-SORT-001** 

Cho phép sort: 

```
Title
Created Date
```

Ví dụ: 

21 

```
sortName=createdDate
sortBy=desc
```

## **REQ-SORT-002** 

Không được đưa trực tiếp giá trị sort từ client vào SQL. 

Phải whitelist các field được phép sort. 

Ví dụ: 

```
createdDate -> created_date
title       -> title
```

# **30. COMMENT** 

## **REQ-COMMENT-001** 

User đã đăng nhập có thể comment. 

Form: 

```
Content
[Comment]
```

## **REQ-COMMENT-002** 

Comment hiển thị: 

```
Username
Content
Created Date
```

22 

## **REQ-COMMENT-003** 

Không cho phép: 

- Comment rỗng. 

- User chưa đăng nhập comment. 

- Gửi dữ liệu không hợp lệ. 

## **REQ-COMMENT-004** 

Admin có thể: 

```
List Comment
Delete Comment
```

# **31. JSP / JSTL** 

## **REQ-JSP-001** 

Không được sử dụng Java Scriptlet trong JSP. 

Không được viết: 

```
<%
// Java code
%>
```

Phải sử dụng: 

```
EL
JSTL
```

Ví dụ: 

```
<c:forEach>
<c:if>
<c:choose>
```

23 

# **32. LAYOUT** 

## **REQ-UI-001** 

User layout: 

```
Header
Menu
Content
Footer
```

Admin layout: 

```
Header
Sidebar
Content
Footer
```

Có thể sử dụng: 

```
SiteMesh
```

hoặc cơ chế layout tương đương. 

# **33. CONFIGURATION** 

## **REQ-CONFIG-001** 

Database config phải nằm trong file configuration. 

Ví dụ: 

```
db.properties
```

Bao gồm: 

```
db.url
db.username
db.password
```

Không hard-code: 

24 

```
username
password
host
database
```

trong Java source code. 

# **34. EXCEPTION HANDLING** 

## **REQ-EXCEPTION-001** 

Phải xử lý: 

```
SQLException
IOException
ServletException
ValidationException
FileUploadException
```

Không được: 

```
catch(Exception e) {
    e.printStackTrace();
}
```

rồi bỏ qua lỗi. 

# **35. VALIDATION** 

## **REQ-VALIDATION-001** 

Validation phải được thực hiện ở server. 

Tối thiểu: 

### **User** 

```
username
password
email
```

25 

### **Category** 

```
name
code
```

### **News** 

```
title
shortDescription
content
category
```

### **Comment** 

```
content
```

# **36. SECURITY** 

## **REQ-SECURITY-001 — SQL Injection** 

Không nối chuỗi dữ liệu người dùng vào SQL. 

Bắt buộc PreparedStatement. 

## **REQ-SECURITY-002 — Password** 

Không lưu password dạng plain text. 

Ví dụ không được lưu: 

```
123456
```

Phải hash password. 

## **REQ-SECURITY-003 — XSS** 

Dữ liệu do user nhập phải được xử lý phù hợp trước khi render. 

Đặc biệt chú ý: 

26 

```
Comment
News content
Username
Search
```

## **REQ-SECURITY-004 — Authorization** 

User không được truy cập trực tiếp URL Admin. 

Ví dụ: 

```
/admin-news
/admin-user
/admin-category
```

Server phải kiểm tra permission. 

# **37. URL MAPPING** 

Tối thiểu: 

```
/login
/register
/logout
/
/home
/category
/news
/admin-home
/admin-user
/admin-role
/admin-category
/admin-news
/admin-comment
/api/upload
```

Có thể thay đổi URL nhưng phải đảm bảo đầy đủ chức năng. 

27 

# **38. FILTER** 

## **REQ-FILTER-001** 

Tạo: 

```
AuthorizationFilter
```

## **REQ-FILTER-002** 

Có thể tạo: 

```
EncodingFilter
```

để đảm bảo UTF-8. 

# **39. DEPENDENCY INJECTION** 

## **REQ-DI-001** 

Có thể sử dụng CDI/Weld. 

Ví dụ: 

```
Controller
    |
    | @Inject
    v
Service
    |
    | @Inject
    v
DAO
```

Mục tiêu: 

- Hiểu Dependency Injection. 

- Giảm coupling. 

- Không tự new toàn bộ dependency trong Controller. 

Phần này có thể được đánh dấu là nâng cao. 

28 

# **40. TEST CASE** 

Thực tập sinh phải tự xây dựng Test Case. 

## **REQ-TEST-001 — Login** 

Tối thiểu: 

```
TC01: Username/password đúng
TC02: Username sai
TC03: Password sai
TC04: Username rỗng
TC05: Password rỗng
TC06: User chưa login truy cập admin
TC07: USER truy cập chức năng ADMIN
```

## **REQ-TEST-002 — Category** 

```
TC01: Add thành công
TC02: Name rỗng
TC03: Code rỗng
TC04: Duplicate code
TC05: Edit
TC06: Delete
TC07: Search
TC08: Sort
TC09: Pagination
```

## **REQ-TEST-003 — News** 

```
TC01: Add
TC02: Edit
TC03: Delete
TC04: Search
TC05: Filter category
TC06: Sort
TC07: Pagination
TC08: Upload thumbnail
TC09: Invalid image
```

29 

## **REQ-TEST-004 — Comment** 

```
TC01: Comment thành công
TC02: Comment rỗng
TC03: User chưa login
TC04: Delete comment bằng Admin
```

# **41. GIT** 

## **REQ-GIT-001** 

Sử dụng Git. 

Branch: 

```
main
develop
feature/*
```

Ví dụ: 

```
feature/login
feature/register
feature/category
feature/news
feature/comment
feature/upload
```

## **REQ-GIT-002** 

Commit phải có ý nghĩa. 

Ví dụ: 

```
feat: implement user login
feat: implement category management
feat: implement news pagination
fix: validate duplicate category code
fix: handle invalid image upload
```

Không sử dụng commit message kiểu: 

30 

```
test
abc
update
fix
aaa
```

# **42. README** 

## **REQ-DOC-001** 

README phải có: 

`1. Project Overview` 

`2. Features` 

`3. Technologies` 

`4. Architecture` 

`5. Database Design` 

`6. Installation` 

`7. Configuration` 

`8. How to Run` 

`9. Demo Accounts` 

`10. URL List` 

`11. API Documentation` 

`12. Screenshots` 

`13. Test Cases` 

`14. Known Issues` 

# **43. DEPLOYMENT** 

## **REQ-DEPLOY-001** 

Project phải build được: 

```
.war
```

Ví dụ: 

```
mvn clean package
```

31 

## **REQ-DEPLOY-002** 

WAR phải deploy được lên: 

```
Apache Tomcat
```

## **REQ-DEPLOY-003** 

Sau khi deploy phải: 

- Kết nối được MySQL. 

- Login được. 

- CRUD được. 

- Upload được. 

- Comment được. 

- Authorization hoạt động. 

- Không có lỗi nghiêm trọng trong log. 

# **44. SPRINT PLAN** 

## **SPRINT 1 — Java Web Foundation** 

Nội dung: 

- Maven 

- Tomcat 

- Servlet 

- JSP 

- JSTL 

- Request/Response 

- Forward 

- Redirect 

- Session 

Deliverable: 

```
Hello Servlet
Login page
Basic JSP
Basic Servlet routing
```

32 

# **SPRINT 2 — Database + JDBC** 

Nội dung: 

- MySQL 

- Database design 

- SQL • JOIN • JDBC 

- Connection 

- PreparedStatement 

- ResultSet 

Deliverable: 

```
Database hoàn chỉnh
JDBC connection
Generic DAO
Basic CRUD
```

# **SPRINT 3 — Architecture** 

Nội dung: 

```
Model
DAO
Service
Controller
Mapper
```

Deliverable: 

```
Category CRUD
```

phải chạy qua: 

```
Controller
→ Service
→ DAO
→ JDBC
→ MySQL
```

33 

# **SPRINT 4 — Authentication** 

Nội dung: 

- Register • Login 

- Logout 

- Session 

- Password hash 

Deliverable: 

```
Register
Login
Logout
```

# **SPRINT 5 — Authorization** 

Nội dung: 

- Role 

- Permission 

- Filter 

- Authentication check 

- Authorization check 

Deliverable: 

```
ADMIN
USER
AuthorizationFilter
403 page
```

# **SPRINT 6 — Category Management** 

Nội dung: 

- List 

- Add 

- Edit 

- Delete 

- Search 

- Sort 

34 

• Pagination 

Deliverable: 

```
Category Management hoàn chỉnh
```

# **SPRINT 7 — News Management** 

Nội dung: 

- News CRUD • Category relation • Search • Filter • Pagination • Sort 

Deliverable: 

```
News Management hoàn chỉnh
```

# **SPRINT 8 — Upload + Rich Text** 

Nội dung: 

- Multipart upload 

- Image validation 

- File storage • CKEditor 

- Thumbnail 

Deliverable: 

```
Upload API
News thumbnail
Rich text content
```

35 

# **SPRINT 9 — User Website** 

Nội dung: 

- Home • Category • News list • News detail • Search • Pagination 

Deliverable: 

```
Website User hoàn chỉnh
```

# **SPRINT 10 — Comment** 

Nội dung: 

- Add comment • List comment • Delete comment • Authentication check 

Deliverable: 

```
Comment system
```

# **SPRINT 11 — Security + Refactor** 

Kiểm tra: 

- SQL Injection 

- XSS 

- Password 

- Authorization 

- Validation 

- Exception handling 

- Code duplication 

Deliverable: 

36 

```
Security checklist
Refactored code
```

# **SPRINT 12 — Testing + Deployment** 

Nội dung: 

- Test Case 

- Bug fixing 

- README 

- Build WAR 

- Deploy Tomcat 

- Final demo 

Deliverable: 

```
Final Project
```

# **45. DEFINITION OF DONE** 

Một chức năng chỉ được coi là hoàn thành khi: 

```
[ ] Code hoàn chỉnh
```

- `[ ] Database hoàn chỉnh` 

- `[ ] Validation` 

- `[ ] Exception handling` 

- `[ ] Security phù hợp [ ] UI hoạt động [ ] Test Case [ ] Test thành công [ ] Không có lỗi nghiêm trọng [ ] Code được commit lên Git` 

# **46. DEFINITION OF DONE — TOÀN PROJECT** 

## **Backend** 

- `[ ] Servlet` 

- `[ ] JSP/JSTL` 

37 

```
[ ] JDBC
```

```
[ ] DAO
```

```
[ ] Service
```

```
[ ] Model
```

- `[ ] Mapper` 

```
[ ] Authentication
```

```
[ ] Authorization
```

```
[ ] Filter
```

```
[ ] Pagination
```

```
[ ] Sorting
```

```
[ ] Search
```

- `[ ] Upload` 

- `[ ] Comment` 

- `[ ] Exception Handling` 

```
[ ] Validation
```

## **Database** 

```
[ ] User
```

- `[ ] Role` 

- `[ ] User Role` 

- `[ ] Category` 

- `[ ] News` 

- `[ ] Comment` 

```
[ ] Primary Key
```

```
[ ] Foreign Key
```

- `[ ] Index` 

- `[ ] Unique` 

```
[ ] Sample Data
```

## **User Website** 

```
[ ] Home
[ ] Category
```

- `[ ] News List` 

- `[ ] News Detail` 

- `[ ] Search` 

- `[ ] Login` 

```
[ ] Register
```

- `[ ] Logout` 

```
[ ] Comment
```

## **Admin** 

```
[ ] Dashboard
```

- `[ ] User Management` 

38 

- `[ ] Role Management` 

- `[ ] Category Management` 

- `[ ] News Management` 

- `[ ] Comment Management` 

- `[ ] Upload` 

## **Deployment** 

- `[ ] Maven build` 

- `[ ] WAR` 

- `[ ] Tomcat` 

- `[ ] MySQL` 

- `[ ] Configuration` 

- `[ ] README` 

# **47. TIÊU CHÍ ĐÁNH GIÁ** 

## **47.1. Java Core — 20 điểm** 

```
OOP                 4 điểm
Interface           3 điểm
Abstract class      2 điểm
Collection           3 điểm
Generic              2 điểm
Exception            3 điểm
Clean Code           3 điểm
```

## **47.2. Java Web — 20 điểm** 

```
Servlet              4 điểm
JSP/JSTL             3 điểm
Request/Response     3 điểm
Session/Cookie       3 điểm
Filter               3 điểm
MVC                  4 điểm
```

## **47.3. Database/JDBC — 25 điểm** 

```
SQL                  5 điểm
JOIN                 3 điểm
```

39 

```
JDBC                 5 điểm
PreparedStatement    4 điểm
Transaction          3 điểm
Pagination           3 điểm
Database Design      2 điểm
```

## **47.4. Architecture — 20 điểm** 

|`Controller            4 điểm`|
|---|
|`Service               4 điểm`|
|`DAO                   4 điểm`|
|`Mapper                3 điểm`|
|`Model                 2 điểm`|
|`Separation of concern 3 điểm`|



## **47.5. Engineering — 15 điểm** 

|`Git                   3 điểm`|
|---|
|`Test Case             3 điểm`|
|`README                2 điểm`|
|`Debugging             3 điểm`|
|`Code Quality          2 điểm`|
|`Deployment            2 điểm`|



# **48. MỨC ĐỘ YÊU CẦU** 

## **MUST HAVE** 

Bắt buộc hoàn thành: 

```
Servlet
JSP
JSTL
JDBC
MySQL
DAO
Service
Model
Login
Register
Logout
```

40 

```
Role
Authorization
Category CRUD
News CRUD
Search
Pagination
Sorting
Upload
Comment
Git
Deployment
```

## **SHOULD HAVE** 

Nên hoàn thành: 

```
CKEditor
CDI/Weld
Advanced validation
Better exception handling
XSS protection
Transaction
Advanced UI
```

## **COULD HAVE** 

Nếu còn thời gian: 

```
REST API
AJAX
Dashboard statistics
Logging
Unit Test
Docker
CI/CD
```

# **49. QUY ĐỊNH ĐỐI VỚI THỰC TẬP SINH** 

## **Không được copy source code** 

Thực tập sinh được tham khảo: 

- Documentation. 

- Stack Overflow. 

41 

• GitHub. • Tutorial. 

- AI. 

Nhưng phải tự hiểu code. 

Mentor có quyền yêu cầu giải thích bất kỳ đoạn code nào. 

# **50. YÊU CẦU KHI DEMO** 

Thực tập sinh phải tự demo: 

### **User** 

```
Register
Login
View News
Search
Filter Category
View Detail
Comment
Logout
```

### **Admin** 

```
Login
Dashboard
User Management
Role Management
Category CRUD
News CRUD
Upload
Comment Management
Logout
```

### **Security** 

Phải demo: 

```
User truy cập Admin
SQL Injection
Password storage
Unauthorized request
```

và giải thích cách hệ thống xử lý. 

42 

# **51. YÊU CẦU VẤN ĐÁP** 

Sau khi hoàn thành, thực tập sinh phải giải thích được: 

1. Servlet là gì? 2. Request và Response hoạt động như thế nào? 3. Forward khác Redirect như thế nào? 4. Session hoạt động thế nào? 5. Filter dùng để làm gì? 6. Vì sao cần Service Layer? 7. Vì sao cần DAO? 8. JDBC hoạt động như thế nào? 9. PreparedStatement giải quyết vấn đề gì? 10. SQL Injection là gì? 11. ResultSet là gì? 12. RowMapper dùng để làm gì? 13. Pagination hoạt động như thế nào? 14. <mark>`LIMIT ?, ?`</mark> hoạt động thế nào? 15. Authentication khác Authorization như thế nào? 16. Vì sao không được chỉ kiểm tra quyền ở frontend? 17. Password tại sao phải hash? 18. Foreign Key dùng để làm gì? 19. JOIN dùng khi nào? 20. Vì sao không viết SQL trong Servlet? 21. Vì sao không viết Java code trong JSP? 22. MVC là gì? 23. Dependency Injection là gì? 24. WAR là gì? 25. Tomcat làm nhiệm vụ gì? 

# **52. KẾT QUẢ MONG ĐỢI** 

Sau khi hoàn thành project, thực tập sinh phải có khả năng tự xây dựng một Java Web application từ đầu theo mô hình: 



<!-- Start of picture text -->
                 ┌──────────────┐<br>                 │   Browser    │<br>                 └──────┬───────┘<br>                        │<br>                        ▼<br>                 ┌──────────────┐<br>                 │  Controller  │<br>                 │   Servlet    │<br>                 └──────┬───────┘<br>                        │<br>                        ▼<br><!-- End of picture text -->

43 



<!-- Start of picture text -->
                 ┌──────────────┐<br>                 │   Service    │<br>                 └──────┬───────┘<br>                        │<br>                        ▼<br>                 ┌──────────────┐<br>                 │     DAO      │<br>                 └──────┬───────┘<br>                        │<br>                        ▼<br>                 ┌──────────────┐<br>                 │     JDBC     │<br>                 └──────┬───────┘<br>                        │<br>                        ▼<br>                 ┌──────────────┐<br>                 │    MySQL     │<br>                 └──────────────┘<br><!-- End of picture text -->

Thực tập sinh không chỉ cần làm cho project "chạy được", mà phải hiểu được toàn bộ flow từ: 

```
HTTP Request
    ↓
Servlet
    ↓
Service
    ↓
DAO
    ↓
JDBC
    ↓
SQL
    ↓
MySQL
    ↓
ResultSet
    ↓
Mapper
    ↓
Model
    ↓
Service
    ↓
Servlet
    ↓
JSP
    ↓
HTTP Response
```

44 

Mục tiêu cuối cùng là sau project, thực tập sinh có thể tiếp tục nhận một yêu cầu backend Java mới và tự phân tích, thiết kế database, thiết kế API/Servlet, viết DAO/Service/Controller và hoàn thiện chức năng mà không cần phụ thuộc hoàn toàn vào source code mẫu. 

45 

