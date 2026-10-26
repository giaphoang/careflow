package com.example.hms.careflow.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import java.util.Set;
import java.util.stream.Collectors;

public record Actor(String name, Set<String> roles) {
    public static Actor from(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Authentication required");
        return new Actor(auth.getName(), auth.getAuthorities().stream().map(a -> a.getAuthority().replace("ROLE_", ""))
            .collect(Collectors.toUnmodifiableSet()));
    }
    public boolean has(String role) { return roles.contains(role); }
    public void require(String... allowed) {
        if (has("ADMIN")) return;
        for (String role : allowed) if (has(role)) return;
        throw new AccessDeniedException("This role cannot perform this operation");
    }
    public void requireCareFlow() { require("REQUESTER", "CLINICAL_STAFF", "INVENTORY_MANAGER"); }
}
