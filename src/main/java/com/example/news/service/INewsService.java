package com.example.news.service;

import com.example.news.model.NewsDetail;
import com.example.news.model.NewsListCriteria;
import com.example.news.model.NewsListItem;
import com.example.news.model.NewsModel;
import com.example.news.model.PageResult;

public interface INewsService {

    PageResult<NewsListItem> search(NewsListCriteria criteria);

    NewsModel findById(Long id);

    NewsDetail findDetailById(Long id);

    long create(NewsModel news, Long authorId);

    boolean update(NewsModel news);

    boolean delete(Long id);
}
