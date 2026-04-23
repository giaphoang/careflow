package com.example.hms.security.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class SecurityAuditServiceImpl implements SecurityAuditService {

    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("SECURITY_AUDIT");

    @Override
    public void loginSuccess(String username, String role) {
        AUDIT_LOGGER.info("event=LOGIN_SUCCESS user={} role={} ip={} cid={}", username, role, clientIp(), correlationId());
    }

    @Override
    public void loginFailure(String username, String reason) {
        AUDIT_LOGGER.warn("event=LOGIN_FAILURE user={} reason={} ip={} cid={}", safe(username), reason, clientIp(), correlationId());
    }

    @Override
    public void refreshSuccess(String username) {
        AUDIT_LOGGER.info("event=REFRESH_SUCCESS user={} ip={} cid={}", username, clientIp(), correlationId());
    }

    @Override
    public void refreshFailure(String username, String reason) {
        AUDIT_LOGGER.warn("event=REFRESH_FAILURE user={} reason={} ip={} cid={}", safe(username), reason, clientIp(), correlationId());
    }

    @Override
    public void logout(String username, boolean accessRevoked, boolean refreshRevoked) {
        AUDIT_LOGGER.info("event=LOGOUT user={} access_revoked={} refresh_revoked={} ip={} cid={}",
                safe(username), accessRevoked, refreshRevoked, clientIp(), correlationId());
    }

    @Override
    public void tokenRevoked(String tokenType, String username) {
        AUDIT_LOGGER.info("event=TOKEN_REVOKED token_type={} user={} ip={} cid={}", tokenType, safe(username), clientIp(), correlationId());
    }

    private String clientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "n/a";
        }
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String correlationId() {
        String cid = MDC.get("correlationId");
        return cid == null ? "n/a" : cid;
    }

    private String safe(String value) {
        return (value == null || value.isBlank()) ? "unknown" : value;
    }
}
