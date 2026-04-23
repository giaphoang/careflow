package com.example.hms.security.oauth;

import com.example.hms.security.jwt.JwtService;
import com.example.hms.security.model.AppUser;
import com.example.hms.security.model.Role;
import com.example.hms.security.repository.AppUserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(AppUserRepository appUserRepository,
                                              @Lazy PasswordEncoder passwordEncoder,
                                              JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");
        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException("Google account email not available");
        }

        AppUser user = appUserRepository.findByUsername(email)
                .orElseGet(() -> appUserRepository.save(new AppUser(
                        email,
                        passwordEncoder.encode(UUID.randomUUID().toString()),
                        Role.RECEPTIONIST
                )));

        String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername(), user.getRole().name());

        String redirectUrl = frontendUrl + "/oauth-callback"
                + "?accessToken=" + encode(accessToken)
                + "&refreshToken=" + encode(refreshToken)
                + "&tokenType=" + encode("Bearer")
                + "&username=" + encode(user.getUsername())
                + "&role=" + encode(user.getRole().name());

        response.sendRedirect(redirectUrl);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
