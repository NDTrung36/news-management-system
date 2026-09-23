<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Category Management</title>
    <style>
        body { font-family: sans-serif; margin: 24px; }
        table { border-collapse: collapse; width: 100%; margin-top: 16px; }
        th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }
        th { background-color: #f2f2f2; }
        .alert { color: #d9534f; background-color: #fdf7f7; border: 1px solid #d9534f; padding: 10px; margin-bottom: 16px; border-radius: 4px; }
        .btn { display: inline-block; padding: 6px 12px; text-decoration: none; border-radius: 4px; border: 1px solid #ccc; cursor: pointer; }
        .btn-primary { background-color: #0275d8; color: white; border-color: #0275d8; }
        .btn-danger { background-color: #d9534f; color: white; border-color: #d9534f; }
        .btn-secondary { background-color: #f0f0f0; color: #333; }
        .actions form { display: inline; }
    </style>
</head>
<body>
    <h1>Category Management</h1>

    <c:if test="${not empty error}">
        <div class="alert" role="alert">
            <c:out value="${error}" />
        </div>
    </c:if>

    <p>
        <a href="${pageContext.request.contextPath}/admin/category?action=create" class="btn btn-primary">Add Category</a>
    </p>

    <table>
        <thead>
            <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Code</th>
                <th>Created Date</th>
                <th>Modified Date</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            <c:choose>
                <c:when test="${not empty categories}">
                    <c:forEach items="${categories}" var="cat">
                        <tr>
                            <td><c:out value="${cat.id}" /></td>
                            <td><c:out value="${cat.name}" /></td>
                            <td><c:out value="${cat.code}" /></td>
                            <td><c:out value="${cat.createdDate}" /></td>
                            <td><c:out value="${cat.modifiedDate}" /></td>
                            <td class="actions">
                                <a href="${pageContext.request.contextPath}/admin/category?action=edit&amp;id=${cat.id}" class="btn btn-secondary">Edit</a>
                                <form method="post" action="${pageContext.request.contextPath}/admin/category?action=delete">
                                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}' />">
                                    <input type="hidden" name="id" value="<c:out value='${cat.id}' />">
                                    <button type="submit" class="btn btn-danger" onclick="return confirm('Are you sure you want to delete this category?');">Delete</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    <tr>
                        <td colspan="6" style="text-align: center;">No categories found.</td>
                    </tr>
                </c:otherwise>
            </c:choose>
        </tbody>
    </table>
</body>
</html>
