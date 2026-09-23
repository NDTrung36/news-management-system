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
        .toolbar { display: flex; gap: 8px; align-items: end; flex-wrap: wrap; }
        .form-group { display: flex; flex-direction: column; gap: 4px; }
        input[type="search"], select { padding: 8px; }
        input[type="search"] { width: 280px; }
        .alert { color: #d9534f; background-color: #fdf7f7; border: 1px solid #d9534f; padding: 10px; margin-bottom: 16px; border-radius: 4px; }
        .btn { display: inline-block; padding: 6px 12px; text-decoration: none; border-radius: 4px; border: 1px solid #ccc; cursor: pointer; }
        .btn-primary { background-color: #0275d8; color: white; border-color: #0275d8; }
        .btn-danger { background-color: #d9534f; color: white; border-color: #d9534f; }
        .btn-secondary { background-color: #f0f0f0; color: #333; }
        .actions form { display: inline; }
        .pagination { display: flex; gap: 6px; margin-top: 16px; align-items: center; }
        .pagination .current { background-color: #0275d8; color: white; border-color: #0275d8; }
        .muted { color: #666; }
    </style>
</head>
<body>
    <h1>Category Management</h1>

    <c:if test="${not empty error}">
        <div class="alert" role="alert">
            <c:out value="${error}" />
        </div>
    </c:if>

    <form method="get" action="${pageContext.request.contextPath}/admin/category" class="toolbar">
        <div class="form-group">
            <label for="search">Search name or code</label>
            <input id="search" name="search" type="search" maxlength="100"
                   value="<c:out value='${criteria.search}' />">
        </div>
        <div class="form-group">
            <label for="sortName">Sort by</label>
            <select id="sortName" name="sortName">
                <option value="id" ${criteria.sortName == 'id' ? 'selected' : ''}>ID</option>
                <option value="name" ${criteria.sortName == 'name' ? 'selected' : ''}>Name</option>
                <option value="code" ${criteria.sortName == 'code' ? 'selected' : ''}>Code</option>
                <option value="createdDate" ${criteria.sortName == 'createdDate' ? 'selected' : ''}>Created Date</option>
            </select>
        </div>
        <div class="form-group">
            <label for="sortBy">Direction</label>
            <select id="sortBy" name="sortBy">
                <option value="asc" ${criteria.sortBy == 'asc' ? 'selected' : ''}>Ascending</option>
                <option value="desc" ${criteria.sortBy == 'desc' ? 'selected' : ''}>Descending</option>
            </select>
        </div>
        <input type="hidden" name="page" value="1">
        <button type="submit" class="btn btn-primary">Apply</button>
        <a href="${pageContext.request.contextPath}/admin/category" class="btn btn-secondary">Reset</a>
    </form>

    <p>
        <a href="${pageContext.request.contextPath}/admin/category?action=create" class="btn btn-primary">Add Category</a>
        <span class="muted">Found <c:out value="${pageResult.totalItems}" /> categories</span>
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
                <c:when test="${not empty pageResult.items}">
                    <c:forEach items="${pageResult.items}" var="cat">
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

    <c:if test="${pageResult.totalPages > 0}">
        <div class="pagination" aria-label="Category pagination">
            <c:if test="${pageResult.hasPrevious}">
                <c:url var="previousUrl" value="/admin/category">
                    <c:param name="search" value="${criteria.search}" />
                    <c:param name="sortName" value="${criteria.sortName}" />
                    <c:param name="sortBy" value="${criteria.sortBy}" />
                    <c:param name="page" value="${pageResult.page - 1}" />
                </c:url>
                <a href="${previousUrl}" class="btn btn-secondary">Previous</a>
            </c:if>

            <c:forEach begin="1" end="${pageResult.totalPages}" var="pageNumber">
                <c:url var="pageUrl" value="/admin/category">
                    <c:param name="search" value="${criteria.search}" />
                    <c:param name="sortName" value="${criteria.sortName}" />
                    <c:param name="sortBy" value="${criteria.sortBy}" />
                    <c:param name="page" value="${pageNumber}" />
                </c:url>
                <c:choose>
                    <c:when test="${pageNumber == pageResult.page}">
                        <span class="btn current"><c:out value="${pageNumber}" /></span>
                    </c:when>
                    <c:otherwise>
                        <a href="${pageUrl}" class="btn btn-secondary"><c:out value="${pageNumber}" /></a>
                    </c:otherwise>
                </c:choose>
            </c:forEach>

            <c:if test="${pageResult.hasNext}">
                <c:url var="nextUrl" value="/admin/category">
                    <c:param name="search" value="${criteria.search}" />
                    <c:param name="sortName" value="${criteria.sortName}" />
                    <c:param name="sortBy" value="${criteria.sortBy}" />
                    <c:param name="page" value="${pageResult.page + 1}" />
                </c:url>
                <a href="${nextUrl}" class="btn btn-secondary">Next</a>
            </c:if>
        </div>
    </c:if>
</body>
</html>
