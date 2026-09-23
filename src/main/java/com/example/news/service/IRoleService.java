package com.example.news.service;

import java.util.Set;

public interface IRoleService {

    Set<String> findRoleCodesByUserId(Long userId);
}
