package com.example.news.dao;

import com.example.news.model.RoleModel;

import java.util.List;

public interface IRoleDAO {

    List<RoleModel> findByUserId(Long userId);

    RoleModel findByCode(String code);
}
