package com.example.coderag.controller;

import com.example.coderag.dto.LoginRequest;
import com.example.coderag.dto.RegisterRequest;
import com.example.coderag.dto.UserDto;
import com.example.coderag.model.AuthProvider;
import com.example.coderag.model.User;
import com.example.coderag.security.CookieService;
import com.example.coderag.security.JwtService;
import com.example.coderag.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CookieService cookieService;

    @Test
    void register_ShouldReturn201AndSetCookie() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("register@example.com")
                .authProvider(AuthProvider.LOCAL)
                .build();

        when(authService.register(any())).thenReturn(new AuthService.AuthResult(user, "test-jwt-token"));

        RegisterRequest request = new RegisterRequest("register@example.com", "securePassword123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.email").value("register@example.com"))
                .andExpect(jsonPath("$.authProvider").value("LOCAL"));
    }

    @Test
    void login_ShouldReturn200AndSetCookie() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("login@example.com")
                .authProvider(AuthProvider.LOCAL)
                .build();

        when(authService.login(any())).thenReturn(new AuthService.AuthResult(user, "test-jwt-token"));

        LoginRequest request = new LoginRequest("login@example.com", "securePassword123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.email").value("login@example.com"));
    }

    @Test
    void logout_ShouldReturn200AndClearCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void getMe_WithoutCookie_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }
}
