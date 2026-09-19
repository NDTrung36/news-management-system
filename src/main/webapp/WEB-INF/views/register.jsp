<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Đăng ký</title>
</head>
<body>
    <h1>Đăng ký</h1>

    <c:if test="${not empty error}">
        <p role="alert"><c:out value="${error}" /></p>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/register">
        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}' />">
        <div>
            <label for="username">Username</label>
            <input id="username" name="username" type="text" maxlength="50" required
                   autocomplete="username" value="<c:out value='${username}' />">
        </div>
        <div>
            <label for="password">Password</label>
            <input id="password" name="password" type="password" maxlength="72" required
                   autocomplete="new-password">
        </div>
        <div>
            <label for="confirmPassword">Confirm Password</label>
            <input id="confirmPassword" name="confirmPassword" type="password" maxlength="72" required
                   autocomplete="new-password">
        </div>
        <div>
            <label for="fullName">Full Name</label>
            <input id="fullName" name="fullName" type="text" maxlength="150" required
                   autocomplete="name" value="<c:out value='${fullName}' />">
        </div>
        <div>
            <label for="email">Email</label>
            <input id="email" name="email" type="email" maxlength="255" required
                   autocomplete="email" value="<c:out value='${email}' />">
        </div>
        <button type="submit">Đăng ký</button>
    </form>

    <p>Đã có tài khoản? <a href="${pageContext.request.contextPath}/login">Đăng nhập</a></p>
</body>
</html>
