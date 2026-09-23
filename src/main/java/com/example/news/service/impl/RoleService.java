package com.example.news.service.impl;

import com.example.news.dao.IRoleDAO;
import com.example.news.dao.impl.RoleDAO;
import com.example.news.exception.ValidationException;
import com.example.news.model.RoleModel;
import com.example.news.service.IRoleService;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RoleService implements IRoleService {

    private final IRoleDAO roleDAO;

    public RoleService() {
        this(new RoleDAO());
    }

    public RoleService(IRoleDAO roleDAO) {
        this.roleDAO = roleDAO;
    }

    @Override
    public Set<String> findRoleCodesByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("User ID must be a positive number");
        }

        List<RoleModel> roles = roleDAO.findByUserId(userId);
        Set<String> roleCodes = new LinkedHashSet<>();
        if (roles != null) {
            for (RoleModel role : roles) {
                if (role == null || role.getCode() == null) {
                    continue;
                }
                String normalizedCode = role.getCode().trim().toUpperCase(Locale.ROOT);
                if (!normalizedCode.isEmpty()) {
                    roleCodes.add(normalizedCode);
                }
            }
        }
        return Collections.unmodifiableSet(roleCodes);
    }
}
