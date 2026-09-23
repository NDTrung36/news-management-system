<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>View News</title>
    <style>
        body { font-family: sans-serif; margin: 24px; max-width: 900px; }
        dt { font-weight: bold; margin-top: 12px; }
        dd { margin: 4px 0 0; }
        .content { white-space: pre-wrap; line-height: 1.5; }
        .muted { color: #666; }
        .btn { display: inline-block; margin-top: 24px; padding: 8px 16px; text-decoration: none; border-radius: 4px; border: 1px solid #ccc; background-color: #f0f0f0; color: #333; }
    </style>
</head>
<body>
    <h1><c:out value="${news.title}" /></h1>
    <dl>
        <dt>Short description</dt>
        <dd><c:out value="${news.shortDescription}" /></dd>

        <dt>Category</dt>
        <dd><c:out value="${news.categoryName}" /></dd>

        <dt>Created by</dt>
        <dd><c:out value="${news.authorName}" /></dd>

        <dt>Created date</dt>
        <dd><c:out value="${news.createdDate}" /></dd>

        <dt>Modified date</dt>
        <dd><c:out value="${news.modifiedDate}" /></dd>

        <dt>Thumbnail</dt>
        <dd>
            <c:choose>
                <c:when test="${not empty news.thumbnail}"><c:out value="${news.thumbnail}" /></c:when>
                <c:otherwise><span class="muted">No thumbnail</span></c:otherwise>
            </c:choose>
        </dd>

        <dt>Content</dt>
        <dd class="content"><c:out value="${news.content}" /></dd>
    </dl>

    <a href="${pageContext.request.contextPath}/admin/news?action=edit&amp;id=${news.id}" class="btn">Edit</a>
    <a href="${pageContext.request.contextPath}/admin/news" class="btn">Back to list</a>
</body>
</html>
