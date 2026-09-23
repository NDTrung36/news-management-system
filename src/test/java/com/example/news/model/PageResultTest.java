package com.example.news.model;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PageResultTest {

    @Test
    public void visiblePagesShouldStayWithinAFivePageWindow() {
        assertWindow(1, 20, 1, 5);
        assertWindow(10, 20, 8, 12);
        assertWindow(20, 20, 16, 20);
        assertWindow(2, 3, 1, 3);
        assertWindow(Integer.MAX_VALUE, Integer.MAX_VALUE,
                Integer.MAX_VALUE - 4, Integer.MAX_VALUE);
        assertWindow(1, 0, 0, 0);
    }

    private void assertWindow(int page, int totalPages, int expectedFirst, int expectedLast) {
        PageResult<Object> result = new PageResult<>(Collections.emptyList(), page, 10, 0, totalPages);
        assertEquals(expectedFirst, result.getFirstVisiblePage());
        assertEquals(expectedLast, result.getLastVisiblePage());
    }
}
