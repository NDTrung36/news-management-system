package com.example.news.dao;

import com.example.news.dao.impl.GenericDAO;
import com.example.news.dao.impl.RoleDAO;
import com.example.news.model.RoleModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnabledIfSystemProperty(named = "runDbTests", matches = "true")
public class RoleDaoIntegrationTest {

    private static final String TEST_USERNAME = "role-sprint5-int";
    private static final String TEST_EMAIL = "role-sprint5-int@example.com";

    private GenericDAO genericDAO;
    private RoleDAO roleDAO;

    @BeforeEach
    public void setUp() {
        genericDAO = new GenericDAO();
        roleDAO = new RoleDAO();
        cleanup();
    }

    @AfterEach
    public void tearDown() {
        cleanup();
    }

    @Test
    public void shouldFindRoleByCodeAndAllRolesAssignedToUser() {
        RoleModel adminRole = roleDAO.findByCode("ADMIN");
        assertNotNull(adminRole);
        assertEquals("Administrator", adminRole.getName());

        long userId = genericDAO.insert(
                "INSERT INTO `user` (username, password, full_name, email, status) VALUES (?, ?, ?, ?, ?)",
                TEST_USERNAME, "not-used", "Role Test", TEST_EMAIL, 1);
        genericDAO.update(
                "INSERT INTO user_role (user_id, role_id) SELECT ?, id FROM `role` WHERE code IN (?, ?)",
                userId, "USER", "ADMIN");

        List<RoleModel> roles = roleDAO.findByUserId(userId);

        assertEquals(2, roles.size());
    }

    private void cleanup() {
        genericDAO.delete("DELETE FROM `user` WHERE username = ?", TEST_USERNAME);
    }
}
