package com.example.news.service;

import com.example.news.dao.IRoleDAO;
import com.example.news.exception.ValidationException;
import com.example.news.model.RoleModel;
import com.example.news.service.impl.RoleService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RoleServiceTest {

    @Test
    public void findRoleCodesShouldNormalizeDeduplicateAndIgnoreInvalidCodes() {
        IRoleDAO roleDAO = new StubRoleDAO(Arrays.asList(
                role(" user "), role("ADMIN"), role("admin"), role(" "), role(null), null));
        RoleService roleService = new RoleService(roleDAO);

        Set<String> result = roleService.findRoleCodesByUserId(10L);

        assertEquals(Arrays.asList("USER", "ADMIN"), Arrays.asList(result.toArray(new String[0])));
        assertThrows(UnsupportedOperationException.class, () -> result.add("EDITOR"));
    }

    @Test
    public void findRoleCodesShouldReturnEmptyImmutableSetWhenUserHasNoRoles() {
        RoleService roleService = new RoleService(new StubRoleDAO(Collections.emptyList()));

        Set<String> result = roleService.findRoleCodesByUserId(10L);

        assertTrue(result.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> result.add("USER"));
    }

    @Test
    public void findRoleCodesShouldRejectInvalidUserId() {
        RoleService roleService = new RoleService(new StubRoleDAO(Collections.emptyList()));

        assertThrows(ValidationException.class, () -> roleService.findRoleCodesByUserId(null));
        assertThrows(ValidationException.class, () -> roleService.findRoleCodesByUserId(0L));
    }

    private static RoleModel role(String code) {
        RoleModel role = new RoleModel();
        role.setCode(code);
        return role;
    }

    private static class StubRoleDAO implements IRoleDAO {

        private final List<RoleModel> roles;

        private StubRoleDAO(List<RoleModel> roles) {
            this.roles = roles;
        }

        @Override
        public List<RoleModel> findByUserId(Long userId) {
            return roles;
        }

        @Override
        public RoleModel findByCode(String code) {
            return null;
        }
    }
}
