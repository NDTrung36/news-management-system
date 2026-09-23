package com.example.news.dao;

import com.example.news.model.NewsDetail;
import com.example.news.model.NewsListCriteria;
import com.example.news.model.NewsListItem;
import com.example.news.model.NewsModel;

import java.util.List;

public interface INewsDAO {

    List<NewsListItem> findPage(NewsListCriteria criteria, long offset, int limit);

    long countByCriteria(NewsListCriteria criteria);

    NewsModel findById(Long id);

    NewsDetail findDetailById(Long id);

    long insert(NewsModel news);

    int update(NewsModel news);

    int delete(Long id);
}
