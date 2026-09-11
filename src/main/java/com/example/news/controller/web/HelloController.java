package com.example.news.controller.web;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/hello")
public class HelloController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String name = request.getParameter("name");
        if (name == null || name.trim().isEmpty()) {
            name = "Guest";
        } else {
            name = name.trim();
        }
// tạo ra một câu chào và lưu vào request object với tên định danh là "message". 
// Data này sẽ được truyền sang file JSP.
        request.setAttribute("message", "Hello, " + name);

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/hello.jsp");
        dispatcher.forward(request, response);
    }
}
