<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
        <!DOCTYPE html>
        <html>

        <head>
            <meta charset="UTF-8">
            <title>Login</title>
        </head>

        <body>
            <h1>Login</h1>

            <c:if test="${not empty error}">
                <p role="alert">
                    <c:out value="${error}" />
                </p>
            </c:if>

            <form method="post" action="${pageContext.request.contextPath}/login">
                <div>
                    <label for="username">Username</label>
                    <input id="username" name="username" type="text">
                </div>
                <div>
                    <label for="password">Password</label>
                    <input id="password" name="password" type="password">
                </div>
                <button type="submit">Login</button>
            </form>
        </body>

        </html>