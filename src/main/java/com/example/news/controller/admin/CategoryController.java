package com.example.news.controller.admin;

import com.example.news.exception.ValidationException;
import com.example.news.model.CategoryModel;
import com.example.news.service.ICategoryService;
import com.example.news.service.impl.CategoryService;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/admin/category")
public class CategoryController extends HttpServlet {

    private final ICategoryService categoryService;

    public CategoryController() {
        this(new CategoryService());
    }

    public CategoryController(ICategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        String action = request.getParameter("action");
        if (action == null) {
            action = "list";
        }

        switch (action) {
            case "create":
                showCreateForm(request, response);
                break;
            case "edit":
                showEditForm(request, response);
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

        String action = request.getParameter("action");
        if ("create".equals(action)) {
            createCategory(request, response);
        } else if ("update".equals(action)) {
            updateCategory(request, response);
        } else if ("delete".equals(action)) {
            deleteCategory(request, response);
        } else {
            response.sendRedirect(request.getContextPath() + "/admin/category");
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String errorParam = request.getParameter("error");
        if ("notFound".equals(errorParam)) {
            request.setAttribute("error", "Category not found");
        }
        request.setAttribute("categories", categoryService.findAll());
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/admin/category/list.jsp");
        dispatcher.forward(request, response);
    }

    private void showCreateForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getAttribute("category") == null) {
            request.setAttribute("category", new CategoryModel());
        }
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/admin/category/form.jsp");
        dispatcher.forward(request, response);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idParam = request.getParameter("id");
        Long id = parseId(idParam);
        if (id == null) {
            response.sendRedirect(request.getContextPath() + "/admin/category?error=notFound");
            return;
        }

        CategoryModel category = categoryService.findById(id);
        if (category == null) {
            response.sendRedirect(request.getContextPath() + "/admin/category?error=notFound");
            return;
        }

        request.setAttribute("category", category);
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/admin/category/form.jsp");
        dispatcher.forward(request, response);
    }

    private void createCategory(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String name = request.getParameter("name");
        String code = request.getParameter("code");

        CategoryModel model = new CategoryModel(name, code);
        try {
            categoryService.create(model);
            response.sendRedirect(request.getContextPath() + "/admin/category");
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("category", model);
            RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/admin/category/form.jsp");
            dispatcher.forward(request, response);
        }
    }

    private void updateCategory(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idParam = request.getParameter("id");
        String name = request.getParameter("name");
        String code = request.getParameter("code");

        Long id = parseId(idParam);
        CategoryModel model = new CategoryModel(id, name, code);

        if (id == null) {
            request.setAttribute("error", "Category ID is invalid or missing");
            request.setAttribute("category", model);
            RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/admin/category/form.jsp");
            dispatcher.forward(request, response);
            return;
        }

        try {
            boolean updated = categoryService.update(model);
            if (!updated) {
                response.sendRedirect(request.getContextPath() + "/admin/category?error=notFound");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/admin/category");
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("category", model);
            RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/admin/category/form.jsp");
            dispatcher.forward(request, response);
        }
    }

    private void deleteCategory(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idParam = request.getParameter("id");
        Long id = parseId(idParam);

        if (id == null) {
            response.sendRedirect(request.getContextPath() + "/admin/category?error=notFound");
            return;
        }

        try {
            boolean deleted = categoryService.delete(id);
            if (!deleted) {
                response.sendRedirect(request.getContextPath() + "/admin/category?error=notFound");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/admin/category");
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("categories", categoryService.findAll());
            RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/admin/category/list.jsp");
            dispatcher.forward(request, response);
        }
    }

    private Long parseId(String idStr) {
        if (idStr == null || idStr.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(idStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
