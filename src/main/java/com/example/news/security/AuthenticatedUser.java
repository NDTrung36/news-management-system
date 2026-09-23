package com.example.news.security;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class AuthenticatedUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private final String fullName;
    private final Set<String> roleCodes;

    public AuthenticatedUser(Long id, String username, String fullName) {
        this(id, username, fullName, Collections.emptySet());
    }

    public AuthenticatedUser(Long id, String username, String fullName, Set<String> roleCodes) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.roleCodes = normalizeRoleCodes(roleCodes);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public Set<String> getRoleCodes() {
        return roleCodes;
    }

    public boolean hasRole(String roleCode) {
        if (roleCode == null) {
            return false;
        }
        return roleCodes.contains(roleCode.trim().toUpperCase(Locale.ROOT));
    }

    private Set<String> normalizeRoleCodes(Set<String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String roleCode : source) {
            if (roleCode == null) {
                continue;
            }
            String value = roleCode.trim().toUpperCase(Locale.ROOT);
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        return Collections.unmodifiableSet(normalized);
    }
}
