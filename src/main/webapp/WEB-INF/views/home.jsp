<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Home</title>
</head>
<body>
    <h1>Home</h1>
    <p>Welcome, <c:out value="${username}" />.</p>

    <c:choose>
        <c:when test="${authenticated}">
            <c:if test="${isAdmin}">
                <p><a href="${pageContext.request.contextPath}/admin/category">Category Management</a></p>
            </c:if>
            <form method="post" action="${pageContext.request.contextPath}/logout">
                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}' />">
                <button type="submit">Đăng xuất</button>
            </form>
        </c:when>
        <c:otherwise>
            <p><a href="${pageContext.request.contextPath}/login">Đăng nhập</a> hoặc
                <a href="${pageContext.request.contextPath}/register">Đăng ký</a>.</p>
        </c:otherwise>
    </c:choose>
</body>
</html>
