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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "runDbTests", matches = "true")
public class CategorySearchIntegrationTest {

    private GenericDAO genericDAO;
    private CategoryService categoryService;
    private String codePrefix;
    private String namePrefix;
    private final List<Long> insertedIds = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        genericDAO = new GenericDAO();
        categoryService = new CategoryService(new CategoryDAO());
        String testId = UUID.randomUUID().toString().replace("-", "");
        codePrefix = "s6-search-" + testId + "-";
        namePrefix = "S6 " + testId + " ";
    }

    @AfterEach
    public void tearDown() {
        for (Long id : insertedIds) {
            genericDAO.delete("DELETE FROM category WHERE id = ?", id);
        }
    }

    @Test
    public void searchShouldMatchNameAndCode() {
        String code = codePrefix + "java";
        insert(namePrefix + "Java Guide", code);

        assertSingleResult(namePrefix + "Java", code);
        assertSingleResult(code, code);
    }

    @Test
    public void searchShouldTreatPercentUnderscoreAndExclamationAsLiterals() {
        insert(namePrefix + "pct%match", codePrefix + "percent");
        insert(namePrefix + "pctXmatch", codePrefix + "percent-other");
        insert(namePrefix + "under_name", codePrefix + "underscore");
        insert(namePrefix + "underXname", codePrefix + "underscore-other");
        insert(namePrefix + "bang!name", codePrefix + "exclamation");
        insert(namePrefix + "bangXname", codePrefix + "exclamation-other");

        assertSingleResult(namePrefix + "pct%", codePrefix + "percent");
        assertSingleResult(namePrefix + "under_", codePrefix + "underscore");
        assertSingleResult(namePrefix + "bang!", codePrefix + "exclamation");
    }

    @Test
    public void searchShouldPaginateAndSortWithStableWhitelistedOrder() {
        for (int index = 1; index <= 21; index++) {
            String suffix = String.format(Locale.ROOT, "%02d", index);
            insert(namePrefix + "Page " + suffix, codePrefix + "page-" + suffix);
        }

        PageResult<CategoryModel> secondPage = categoryService.search(
                new CategoryListCriteria(codePrefix + "page-", "code", "asc", 2));
        PageResult<CategoryModel> descendingPage = categoryService.search(
                new CategoryListCriteria(codePrefix + "page-", "code", "desc", 1));

        assertEquals(21L, secondPage.getTotalItems());
        assertEquals(3, secondPage.getTotalPages());
        assertEquals(2, secondPage.getPage());
        assertEquals(10, secondPage.getItems().size());
        assertEquals(codePrefix + "page-11", secondPage.getItems().get(0).getCode());
        assertTrue(secondPage.hasPrevious());
        assertTrue(secondPage.hasNext());
        assertEquals(codePrefix + "page-21", descendingPage.getItems().get(0).getCode());
    }

    @Test
    public void invalidSortShouldFallBackToIdAndOutOfRangePageShouldBeClamped() {
        insert(namePrefix + "First", codePrefix + "first");
        insert(namePrefix + "Second", codePrefix + "second");

        CategoryListCriteria criteria = new CategoryListCriteria(codePrefix, "id DESC, (SELECT 1)", "desc", 999);
        PageResult<CategoryModel> result = categoryService.search(criteria);

        assertEquals("id", criteria.getSortName());
        assertEquals(1, result.getPage());
        assertEquals(1, result.getTotalPages());
        assertEquals(2, result.getItems().size());
    }

    private void insert(String name, String code) {
        long id = genericDAO.insert("INSERT INTO category (name, code) VALUES (?, ?)", name, code);
        insertedIds.add(id);
    }

    private void assertSingleResult(String search, String expectedCode) {
        PageResult<CategoryModel> result = categoryService.search(
                new CategoryListCriteria(search, "name", "asc", 1));
        assertEquals(1L, result.getTotalItems());
        assertEquals(expectedCode, result.getItems().get(0).getCode());
    }
}
