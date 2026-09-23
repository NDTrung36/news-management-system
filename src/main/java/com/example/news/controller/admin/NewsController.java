package com.example.news.controller.admin;

import com.example.news.exception.ValidationException;
import com.example.news.model.CategoryModel;
import com.example.news.model.NewsDetail;
import com.example.news.model.NewsListCriteria;
import com.example.news.model.NewsListItem;
import com.example.news.model.NewsModel;
import com.example.news.model.PageResult;
import com.example.news.security.AuthSession;
import com.example.news.security.AuthenticatedUser;
import com.example.news.security.CsrfTokenManager;
import com.example.news.service.ICategoryService;
import com.example.news.service.INewsService;
import com.example.news.service.impl.CategoryService;
import com.example.news.service.impl.NewsService;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/admin/news")
public class NewsController extends HttpServlet {

    private final INewsService newsService;
    private final ICategoryService categoryService;
    private final AuthSession authSession;
    private final CsrfTokenManager csrfTokenManager;

    public NewsController() {
        this(new NewsService(), new CategoryService(), new AuthSession(), new CsrfTokenManager());
    }

    public NewsController(INewsService newsService, ICategoryService categoryService,
                          AuthSession authSession, CsrfTokenManager csrfTokenManager) {
        this.newsService = newsService;
        this.categoryService = categoryService;
        this.authSession = authSession;
        this.csrfTokenManager = csrfTokenManager;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
        prepareCsrfToken(request);

        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "list";
        }

        switch (action) {
            case "create":
                showForm(request, response, new NewsModel());
                break;
            case "edit":
                showEditForm(request, response);
                break;
            case "view":
                showDetail(request, response);
                break;
            default:
                showList(request, response);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        if (!csrfTokenManager.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        prepareCsrfToken(request);

        String action = request.getParameter("action");
        if ("create".equals(action)) {
            createNews(request, response);
        } else if ("update".equals(action)) {
            updateNews(request, response);
        } else if ("delete".equals(action)) {
            deleteNews(request, response);
        } else {
            response.sendRedirect(request.getContextPath() + "/admin/news");
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getAttribute("error") == null && "notFound".equals(request.getParameter("error"))) {
            request.setAttribute("error", "News not found");
        }

        NewsListCriteria criteria;
        PageResult<NewsListItem> pageResult;
        try {
            criteria = parseCriteria(request);
            pageResult = newsService.search(criteria);
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            criteria = new NewsListCriteria();
            pageResult = newsService.search(criteria);
        }

        request.setAttribute("criteria", criteria);
        request.setAttribute("pageResult", pageResult);
        request.setAttribute("categories", categoryService.findAll());
        forward(request, response, "/WEB-INF/views/admin/news/list.jsp");
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = parsePositiveId(request.getParameter("id"));
        if (id == null) {
            redirectNotFound(request, response);
            return;
        }

        NewsModel news = newsService.findById(id);
        if (news == null) {
            redirectNotFound(request, response);
            return;
        }
        showForm(request, response, news);
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = parsePositiveId(request.getParameter("id"));
        NewsDetail detail = id == null ? null : newsService.findDetailById(id);
        if (detail == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        request.setAttribute("news", detail);
        forward(request, response, "/WEB-INF/views/admin/news/detail.jsp");
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, NewsModel news)
            throws ServletException, IOException {
        List<CategoryModel> categories = categoryService.findAll();
        request.setAttribute("news", news);
        request.setAttribute("categories", categories);
        forward(request, response, "/WEB-INF/views/admin/news/form.jsp");
    }

    private void createNews(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        NewsModel news = buildNewsModel(request, null);
        AuthenticatedUser user = authSession.getAuthenticatedUser(request);
        if (user == null || user.getId() == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            newsService.create(news, user.getId());
            response.sendRedirect(request.getContextPath() + "/admin/news");
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            showForm(request, response, news);
        }
    }

    private void updateNews(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = parsePositiveId(request.getParameter("id"));
        NewsModel news = buildNewsModel(request, id);
        if (id == null) {
            request.setAttribute("error", "News ID is invalid or missing");
            showForm(request, response, news);
            return;
        }

        try {
            boolean updated = newsService.update(news);
            if (!updated) {
                redirectNotFound(request, response);
                return;
            }
            response.sendRedirect(request.getContextPath() + "/admin/news");
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            showForm(request, response, news);
        }
    }

    private void deleteNews(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = parsePositiveId(request.getParameter("id"));
        if (id == null) {
            redirectNotFound(request, response);
            return;
        }

        try {
            boolean deleted = newsService.delete(id);
            if (!deleted) {
                redirectNotFound(request, response);
                return;
            }
            response.sendRedirect(request.getContextPath() + "/admin/news");
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            showList(request, response);
        }
    }

    private NewsModel buildNewsModel(HttpServletRequest request, Long id) {
        return new NewsModel(id,
                request.getParameter("title"),
                request.getParameter("shortDescription"),
                request.getParameter("content"),
                parsePositiveId(request.getParameter("categoryId")));
    }

    private NewsListCriteria parseCriteria(HttpServletRequest request) {
        String categoryIdParameter = request.getParameter("categoryId");
        Long categoryId = parseOptionalCategoryId(categoryIdParameter);
        return new NewsListCriteria(
                request.getParameter("search"),
                categoryId,
                request.getParameter("sortName"),
                request.getParameter("sortBy"),
                parsePage(request.getParameter("page")));
    }

    private Long parseOptionalCategoryId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        Long id = parsePositiveId(value);
        if (id == null) {
            throw new ValidationException("Category filter is invalid");
        }
        return id;
    }

    private Long parsePositiveId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            long id = Long.parseLong(value.trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parsePage(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NewsListCriteria.DEFAULT_PAGE;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return NewsListCriteria.DEFAULT_PAGE;
        }
    }

    private void redirectNotFound(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/admin/news?error=notFound");
    }

    private void prepareCsrfToken(HttpServletRequest request) {
        request.setAttribute("csrfToken", csrfTokenManager.ensureToken(request));
    }

    private void forward(HttpServletRequest request, HttpServletResponse response, String path)
            throws ServletException, IOException {
        RequestDispatcher dispatcher = request.getRequestDispatcher(path);
        dispatcher.forward(request, response);
    }
}
