package com.example.coderag.security;

import com.example.coderag.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final AuthService authService;
    private final JwtService jwtService;
    private final CookieService cookieService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(AuthService authService, JwtService jwtService, CookieService cookieService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.cookieService = cookieService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to " + frontendUrl);
            return;
        }

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        AuthService.AuthResult authResult = authService.processOAuthPostLogin(oAuth2User);

        ResponseCookie jwtCookie = cookieService.createJwtCookie(
                authResult.token(),
                jwtService.getJwtExpirationSeconds()
        );

        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());

        String targetUrl = frontendUrl + "/dashboard";
        log.info("Redirecting OAuth2 authenticated user to {}", targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
