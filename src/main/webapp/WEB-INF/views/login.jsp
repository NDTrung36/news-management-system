<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Đăng nhập</title>
</head>
<body>
    <h1>Đăng nhập</h1>

    <c:if test="${not empty message}">
        <p role="status"><c:out value="${message}" /></p>
    </c:if>
    <c:if test="${not empty error}">
        <p role="alert"><c:out value="${error}" /></p>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/login">
        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}' />">
        <div>
            <label for="username">Username</label>
            <input id="username" name="username" type="text" maxlength="50" required
                   autocomplete="username" value="<c:out value='${username}' />">
        </div>
        <div>
            <label for="password">Password</label>
            <input id="password" name="password" type="password" maxlength="72" required
                   autocomplete="current-password">
        </div>
        <button type="submit">Đăng nhập</button>
    </form>

    <p>Chưa có tài khoản? <a href="${pageContext.request.contextPath}/register">Đăng ký</a></p>
</body>
</html>
