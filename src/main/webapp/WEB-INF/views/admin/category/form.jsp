<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>
        <c:choose>
            <c:when test="${not empty category.id}">Edit Category</c:when>
            <c:otherwise>Create Category</c:otherwise>
        </c:choose>
    </title>
    <style>
        body { font-family: sans-serif; margin: 24px; }
        .form-group { margin-bottom: 16px; }
        label { display: block; margin-bottom: 6px; font-weight: bold; }
        input[type="text"] { width: 350px; padding: 8px; box-sizing: border-box; }
        .alert { color: #d9534f; background-color: #fdf7f7; border: 1px solid #d9534f; padding: 10px; margin-bottom: 16px; border-radius: 4px; }
        .btn { display: inline-block; padding: 8px 16px; text-decoration: none; border-radius: 4px; border: 1px solid #ccc; cursor: pointer; }
        .btn-primary { background-color: #0275d8; color: white; border-color: #0275d8; }
        .btn-secondary { background-color: #f0f0f0; color: #333; margin-left: 8px; }
    </style>
</head>
<body>
    <h1>
        <c:choose>
            <c:when test="${not empty category.id}">Edit Category</c:when>
            <c:otherwise>Create Category</c:otherwise>
        </c:choose>
    </h1>

    <c:if test="${not empty error}">
        <div class="alert" role="alert">
            <c:out value="${error}" />
        </div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/admin/category?action=${not empty category.id ? 'update' : 'create'}">
        <c:if test="${not empty category.id}">
            <input type="hidden" name="id" value="<c:out value='${category.id}' />">
        </c:if>

        <div class="form-group">
            <label for="name">Name *</label>
            <input id="name" name="name" type="text" maxlength="100" required value="<c:out value='${category.name}' />">
        </div>

        <div class="form-group">
            <label for="code">Code *</label>
            <input id="code" name="code" type="text" maxlength="100" required value="<c:out value='${category.code}' />">
        </div>

        <div>
            <button type="submit" class="btn btn-primary">
                <c:choose>
                    <c:when test="${not empty category.id}">Update</c:when>
                    <c:otherwise>Save</c:otherwise>
                </c:choose>
            </button>
            <a href="${pageContext.request.contextPath}/admin/category" class="btn btn-secondary">Cancel</a>
        </div>
    </form>
</body>
</html>
