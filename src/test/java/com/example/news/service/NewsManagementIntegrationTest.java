package com.example.news.service;

import com.example.news.dao.impl.CategoryDAO;
import com.example.news.dao.impl.GenericDAO;
import com.example.news.dao.impl.NewsDAO;
import com.example.news.dao.impl.UserDAO;
import com.example.news.model.NewsDetail;
import com.example.news.model.NewsListCriteria;
import com.example.news.model.NewsListItem;
import com.example.news.model.NewsModel;
import com.example.news.model.PageResult;
import com.example.news.service.impl.NewsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "runDbTests", matches = "true")
public class NewsManagementIntegrationTest {

    private GenericDAO genericDAO;
    private NewsDAO newsDAO;
    private INewsService newsService;
    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> categoryIds = new ArrayList<>();
    private final List<Long> newsIds = new ArrayList<>();
    private final List<Long> commentIds = new ArrayList<>();
    private long userId;
    private long categoryId;
    private String fixturePrefix;

    @BeforeEach
    public void setUp() {
        genericDAO = new GenericDAO();
        newsDAO = new NewsDAO();
        newsService = new NewsService(newsDAO, new CategoryDAO(), new UserDAO());
        fixturePrefix = "n7" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        userId = createUser("News Fixture Author");
        categoryId = createCategory("News Fixture Category");
    }

    @AfterEach
    public void tearDown() {
        for (Long commentId : commentIds) {
            genericDAO.delete("DELETE FROM `comment` WHERE id = ?", commentId);
        }
        for (Long newsId : newsIds) {
            genericDAO.delete("DELETE FROM news WHERE id = ?", newsId);
        }
        for (Long categoryId : categoryIds) {
            genericDAO.delete("DELETE FROM category WHERE id = ?", categoryId);
        }
        for (Long userId : userIds) {
            genericDAO.delete("DELETE FROM `user` WHERE id = ?", userId);
        }
    }

    @Test
    public void crudShouldMapFullDetailAndKeepImmutableDatabaseFieldsOnUpdate() {
        long newsId = insertNews("Initial title", "Initial short description", "Initial content", categoryId);

        NewsModel inserted = newsDAO.findById(newsId);
        assertNotNull(inserted);
        assertEquals(userId, inserted.getCreatedById().longValue());
        assertEquals(categoryId, inserted.getCategoryId().longValue());
        assertNull(inserted.getThumbnail());
        assertNotNull(inserted.getCreatedDate());

        genericDAO.update("UPDATE news SET thumbnail = ? WHERE id = ?", "uploads/existing.png", newsId);
        NewsModel update = newsDAO.findById(newsId);
        update.setTitle("Updated title");
        update.setShortDescription("Updated short description");
        update.setContent("Updated content");
        assertEquals(1, newsDAO.update(update));

        NewsModel updated = newsDAO.findById(newsId);
        assertEquals("Updated title", updated.getTitle());
        assertEquals("Updated short description", updated.getShortDescription());
        assertEquals("Updated content", updated.getContent());
        assertEquals(userId, updated.getCreatedById().longValue());
        assertEquals("uploads/existing.png", updated.getThumbnail());
        assertNotNull(updated.getModifiedDate());

        NewsDetail detail = newsDAO.findDetailById(newsId);
        assertNotNull(detail);
        assertEquals("News Fixture Category", detail.getCategoryName());
        assertEquals("News Fixture Author", detail.getAuthorName());
        assertEquals("Updated content", detail.getContent());
    }

    @Test
    public void searchShouldMatchTitleAndShortDescriptionAndTreatLikeCharactersLiterally() {
        long titleId = insertNews(fixturePrefix + " title match", "ordinary description", "content", categoryId);
        long shortDescriptionId = insertNews("ordinary title", fixturePrefix + " short match", "content", categoryId);
        long percentId = insertNews(fixturePrefix + " percent%match", "ordinary", "content", categoryId);
        insertNews(fixturePrefix + " percentXmatch", "ordinary", "content", categoryId);
        long underscoreId = insertNews(fixturePrefix + " under_score", "ordinary", "content", categoryId);
        insertNews(fixturePrefix + " underXscore", "ordinary", "content", categoryId);
        long exclamationId = insertNews(fixturePrefix + " bang!mark", "ordinary", "content", categoryId);
        insertNews(fixturePrefix + " bangXmark", "ordinary", "content", categoryId);

        assertSingleItem(fixturePrefix + " title match", titleId);
        assertSingleItem(fixturePrefix + " short match", shortDescriptionId);
        assertSingleItem(fixturePrefix + " percent%", percentId);
        assertSingleItem(fixturePrefix + " under_", underscoreId);
        assertSingleItem(fixturePrefix + " bang!", exclamationId);
    }

    @Test
    public void searchShouldFilterSortAndPaginateWithStableOrder() {
        long otherCategoryId = createCategory("Other Fixture Category");
        long matchingInOtherCategory = insertNews(fixturePrefix + " filter-only", "other", "content", otherCategoryId);
        long matchingInDefaultCategory = insertNews(fixturePrefix + " filter-only", "default", "content", categoryId);
        for (int index = 1; index <= 21; index++) {
            String suffix = String.format(Locale.ROOT, "%02d", index);
            insertNews(fixturePrefix + " page-" + suffix, "page", "content", categoryId);
        }

        PageResult<NewsListItem> filtered = newsService.search(new NewsListCriteria(
                fixturePrefix + " filter-only", categoryId, "title", "asc", 1));
        assertEquals(1L, filtered.getTotalItems());
        assertEquals(matchingInDefaultCategory, filtered.getItems().get(0).getId().longValue());
        assertFalse(filtered.getItems().stream()
                .anyMatch(item -> item.getId().equals(matchingInOtherCategory)));

        PageResult<NewsListItem> secondPage = newsService.search(new NewsListCriteria(
                fixturePrefix + " page-", categoryId, "title", "asc", 2));
        PageResult<NewsListItem> descendingPage = newsService.search(new NewsListCriteria(
                fixturePrefix + " page-", categoryId, "title", "desc", 1));
        assertEquals(21L, secondPage.getTotalItems());
        assertEquals(3, secondPage.getTotalPages());
        assertEquals(10, secondPage.getItems().size());
        assertEquals(fixturePrefix + " page-11", secondPage.getItems().get(0).getTitle());
        assertEquals(fixturePrefix + " page-21", descendingPage.getItems().get(0).getTitle());

        NewsListCriteria unsafeSort = new NewsListCriteria(fixturePrefix + " page-", categoryId,
                "title DESC, (SELECT 1)", "desc", 999);
        PageResult<NewsListItem> clamped = newsService.search(unsafeSort);
        assertEquals("createdDate", unsafeSort.getSortName());
        assertEquals(3, clamped.getPage());
    }

    @Test
    public void deletingNewsShouldCascadeToCommentsWithoutDeletingCategory() {
        long newsId = insertNews("Cascade title", "Cascade description", "Cascade content", categoryId);
        long commentId = genericDAO.insert(
                "INSERT INTO `comment` (content, user_id, news_id) VALUES (?, ?, ?)",
                "Fixture comment", userId, newsId);
        commentIds.add(commentId);

        assertEquals(1, newsDAO.delete(newsId));

        assertEquals(0L, genericDAO.count("SELECT COUNT(*) FROM `comment` WHERE id = ?", commentId));
        assertEquals(1L, genericDAO.count("SELECT COUNT(*) FROM category WHERE id = ?", categoryId));
        assertNull(newsDAO.findById(newsId));
    }

    private void assertSingleItem(String search, long expectedId) {
        PageResult<NewsListItem> result = newsService.search(
                new NewsListCriteria(search, null, "title", "asc", 1));
        assertEquals(1L, result.getTotalItems());
        assertEquals(expectedId, result.getItems().get(0).getId().longValue());
    }

    private long createUser(String fullName) {
        String username = fixturePrefix + "u" + (userIds.size() + 1);
        long id = genericDAO.insert(
                "INSERT INTO `user` (username, password, full_name, email, status) VALUES (?, ?, ?, ?, ?)",
                username, "test-password", fullName, username + "@example.test", 1);
        userIds.add(id);
        return id;
    }

    private long createCategory(String name) {
        String code = fixturePrefix + "c" + (categoryIds.size() + 1);
        long id = genericDAO.insert("INSERT INTO category (name, code) VALUES (?, ?)", name, code);
        categoryIds.add(id);
        return id;
    }

    private long insertNews(String title, String shortDescription, String content, long selectedCategoryId) {
        NewsModel news = new NewsModel(title, shortDescription, content, selectedCategoryId);
        news.setCreatedById(userId);
        long id = newsDAO.insert(news);
        newsIds.add(id);
        return id;
    }
}
