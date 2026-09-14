package com.example.coderag.service;

import com.example.coderag.dto.LoginRequest;
import com.example.coderag.dto.RegisterRequest;
import com.example.coderag.exception.InvalidCredentialsException;
import com.example.coderag.exception.UserAlreadyExistsException;
import com.example.coderag.model.AuthProvider;
import com.example.coderag.model.User;
import com.example.coderag.repository.UserRepository;
import com.example.coderag.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("encoded_secret")
                .authProvider(AuthProvider.LOCAL)
                .build();
    }

    @Test
    void register_ShouldSaveUserAndReturnToken_WhenEmailIsNew() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("mocked-jwt-token");

        AuthService.AuthResult result = authService.register(request);

        assertNotNull(result);
        assertEquals("mocked-jwt-token", result.token());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_ShouldReturnToken_WhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "encoded_secret")).thenReturn(true);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("mocked-jwt-token");

        AuthService.AuthResult result = authService.login(request);

        assertNotNull(result);
        assertEquals("mocked-jwt-token", result.token());
    }

    @Test
    void login_ShouldThrowException_WhenPasswordDoesNotMatch() {
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongpassword", "encoded_secret")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void processOAuthPostLogin_ShouldCreateNewUser_WhenGithubIdDoesNotExist() {
        OAuth2User oauth2User = mock(OAuth2User.class);
        when(oauth2User.getAttribute("id")).thenReturn(12345);
        when(oauth2User.getAttribute("login")).thenReturn("octocat");
        when(oauth2User.getAttribute("email")).thenReturn("octo@github.com");

        when(userRepository.findByGithubId("12345")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("octo@github.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtService.generateToken(any(), any(), any())).thenReturn("oauth-jwt-token");

        AuthService.AuthResult result = authService.processOAuthPostLogin(oauth2User);

        assertNotNull(result);
        assertEquals("oauth-jwt-token", result.token());
        assertEquals("octocat", result.user().getGithubUsername());
        assertEquals(AuthProvider.GITHUB, result.user().getAuthProvider());
    }

    @Test
    void processOAuthPostLogin_ShouldLinkAccount_WhenEmailMatchesExistingLocalUser() {
        OAuth2User oauth2User = mock(OAuth2User.class);
        when(oauth2User.getAttribute("id")).thenReturn(99999);
        when(oauth2User.getAttribute("login")).thenReturn("octocat_linked");
        when(oauth2User.getAttribute("email")).thenReturn("test@example.com");

        when(userRepository.findByGithubId("99999")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("linked-jwt-token");

        AuthService.AuthResult result = authService.processOAuthPostLogin(oauth2User);

        assertNotNull(result);
        assertEquals("99999", sampleUser.getGithubId());
        assertEquals("octocat_linked", sampleUser.getGithubUsername());
        verify(userRepository).save(sampleUser);
    }
}
