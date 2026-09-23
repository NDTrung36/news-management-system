<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>${not empty news.id ? 'Edit News' : 'Create News'}</title>
    <style>
        body { font-family: sans-serif; margin: 24px; }
        .form-group { margin-bottom: 16px; }
        label { display: block; margin-bottom: 6px; font-weight: bold; }
        input[type="text"], select, textarea { width: min(720px, 100%); padding: 8px; box-sizing: border-box; font: inherit; }
        textarea { min-height: 120px; resize: vertical; }
        .alert { color: #d9534f; background-color: #fdf7f7; border: 1px solid #d9534f; padding: 10px; margin-bottom: 16px; border-radius: 4px; }
        .notice { color: #555; background-color: #f7f7f7; border: 1px solid #ddd; padding: 10px; margin-bottom: 16px; border-radius: 4px; }
        .btn { display: inline-block; padding: 8px 16px; text-decoration: none; border-radius: 4px; border: 1px solid #ccc; cursor: pointer; }
        .btn-primary { background-color: #0275d8; color: white; border-color: #0275d8; }
        .btn-secondary { background-color: #f0f0f0; color: #333; margin-left: 8px; }
    </style>
</head>
<body>
    <h1>${not empty news.id ? 'Edit News' : 'Create News'}</h1>

    <c:if test="${not empty error}">
        <div class="alert" role="alert"><c:out value="${error}" /></div>
    </c:if>

    <c:choose>
        <c:when test="${empty categories}">
            <div class="notice">
                Create a <a href="${pageContext.request.contextPath}/admin/category?action=create">Category</a>
                before creating news.
            </div>
            <a href="${pageContext.request.contextPath}/admin/news" class="btn btn-secondary">Back to list</a>
        </c:when>
        <c:otherwise>
            <form method="post" action="${pageContext.request.contextPath}/admin/news?action=${not empty news.id ? 'update' : 'create'}">
                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}' />">
                <c:if test="${not empty news.id}">
                    <input type="hidden" name="id" value="<c:out value='${news.id}' />">
                </c:if>

                <div class="form-group">
                    <label for="title">Title *</label>
                    <input id="title" name="title" type="text" maxlength="255" required
                           value="<c:out value='${news.title}' />">
                </div>

                <div class="form-group">
                    <label for="shortDescription">Short description *</label>
                    <textarea id="shortDescription" name="shortDescription" maxlength="500" required><c:out value="${news.shortDescription}" /></textarea>
                </div>

                <div class="form-group">
                    <label for="content">Content *</label>
                    <textarea id="content" name="content" required><c:out value="${news.content}" /></textarea>
                </div>

                <div class="form-group">
                    <label for="categoryId">Category *</label>
                    <select id="categoryId" name="categoryId" required>
                        <option value="">Select a category</option>
                        <c:forEach items="${categories}" var="category">
                            <option value="<c:out value='${category.id}' />"
                                    ${news.categoryId == category.id ? 'selected' : ''}>
                                <c:out value="${category.name}" />
                            </option>
                        </c:forEach>
                    </select>
                </div>

                <p class="notice">Thumbnail upload and rich-text editing are introduced in Sprint 8. This form stores plain text only.</p>

                <div>
                    <button type="submit" class="btn btn-primary">${not empty news.id ? 'Update' : 'Save'}</button>
                    <a href="${pageContext.request.contextPath}/admin/news" class="btn btn-secondary">Cancel</a>
                </div>
            </form>
        </c:otherwise>
    </c:choose>
</body>
</html>
