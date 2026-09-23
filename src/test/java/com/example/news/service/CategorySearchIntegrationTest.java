package com.example.news.service;

import com.example.news.dao.impl.CategoryDAO;
import com.example.news.model.CategoryListCriteria;
import com.example.news.model.CategoryModel;
import com.example.news.model.PageResult;
import com.example.news.service.impl.CategoryService;
import com.example.news.dao.impl.GenericDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "runDbTests", matches = "true")
public class CategorySearchIntegrationTest {

    private static final String CODE_PREFIX = "s6-search-";

    private GenericDAO genericDAO;
    private CategoryService categoryService;

    @BeforeEach
    public void setUp() {
        genericDAO = new GenericDAO();
        categoryService = new CategoryService(new CategoryDAO());
        cleanup();
    }

    @AfterEach
    public void tearDown() {
        cleanup();
    }

    @Test
    public void searchShouldMatchNameAndEscapeLikeWildcards() {
        insert("S6 Java Guide", CODE_PREFIX + "java");
        insert("S6 Percent % Guide", CODE_PREFIX + "percent");

        PageResult<CategoryModel> byName = categoryService.search(
                new CategoryListCriteria(CODE_PREFIX + "java", "name", "asc", 1));
        PageResult<CategoryModel> byLiteralPercent = categoryService.search(
                new CategoryListCriteria("%", "name", "asc", 1));

        assertEquals(1L, byName.getTotalItems());
        assertEquals("s6-search-java", byName.getItems().get(0).getCode());
        assertEquals(1L, byLiteralPercent.getTotalItems());
        assertEquals("s6-search-percent", byLiteralPercent.getItems().get(0).getCode());
    }

    @Test
    public void searchShouldPaginateAndSortWithStableWhitelistedOrder() {
        for (int index = 1; index <= 21; index++) {
            String suffix = String.format("%02d", index);
            insert("S6 Page " + suffix, CODE_PREFIX + "page-" + suffix);
        }

        PageResult<CategoryModel> secondPage = categoryService.search(
                new CategoryListCriteria(CODE_PREFIX + "page-", "code", "asc", 2));
        PageResult<CategoryModel> descendingPage = categoryService.search(
                new CategoryListCriteria(CODE_PREFIX + "page-", "code", "desc", 1));

        assertEquals(21L, secondPage.getTotalItems());
        assertEquals(3, secondPage.getTotalPages());
        assertEquals(2, secondPage.getPage());
        assertEquals(10, secondPage.getItems().size());
        assertEquals(CODE_PREFIX + "page-11", secondPage.getItems().get(0).getCode());
        assertTrue(secondPage.hasPrevious());
        assertTrue(secondPage.hasNext());
        assertEquals(CODE_PREFIX + "page-21", descendingPage.getItems().get(0).getCode());
    }

    @Test
    public void invalidSortShouldFallBackToIdAndOutOfRangePageShouldBeClamped() {
        insert("S6 First", CODE_PREFIX + "first");
        insert("S6 Second", CODE_PREFIX + "second");

        CategoryListCriteria criteria = new CategoryListCriteria("", "id DESC, (SELECT 1)", "desc", 999);
        criteria.setSearch(CODE_PREFIX);
        PageResult<CategoryModel> result = categoryService.search(criteria);

        assertEquals("id", criteria.getSortName());
        assertEquals(1, result.getPage());
        assertEquals(1, result.getTotalPages());
        assertEquals(2, result.getItems().size());
    }

    private void insert(String name, String code) {
        genericDAO.insert("INSERT INTO category (name, code) VALUES (?, ?)", name, code);
    }

    private void cleanup() {
        genericDAO.delete("DELETE FROM category WHERE code LIKE ?", CODE_PREFIX + "%");
    }
}
