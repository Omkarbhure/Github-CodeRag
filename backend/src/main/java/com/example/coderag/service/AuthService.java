package com.example.coderag.service;

import com.example.coderag.dto.LoginRequest;
import com.example.coderag.dto.RegisterRequest;
import com.example.coderag.exception.InvalidCredentialsException;
import com.example.coderag.exception.UserAlreadyExistsException;
import com.example.coderag.model.AuthProvider;
import com.example.coderag.model.User;
import com.example.coderag.repository.UserRepository;
import com.example.coderag.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public record AuthResult(User user, String token) {}

    @Transactional
    public AuthResult register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("An account with email " + email + " already exists");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .authProvider(AuthProvider.LOCAL)
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser.getId(), savedUser.getEmail(), savedUser.getGithubUsername());

        log.info("Registered new local user with email: {}", email);
        return new AuthResult(savedUser, token);
    }

    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getGithubUsername());
        log.info("User logged in successfully: {}", email);
        return new AuthResult(user, token);
    }

    @Transactional
    public AuthResult processOAuthPostLogin(OAuth2User oAuth2User) {
        Object idObj = oAuth2User.getAttribute("id");
        if (idObj == null) {
            throw new IllegalArgumentException("GitHub user ID not present in OAuth response");
        }
        final String githubId = String.valueOf(idObj);
        final String githubUsername = oAuth2User.getAttribute("login");
        String rawEmail = oAuth2User.getAttribute("email");
        final String email = rawEmail != null ? rawEmail.trim().toLowerCase() : null;

        User user = userRepository.findByGithubId(githubId)
                .map(existingUser -> {
                    if (githubUsername != null && !githubUsername.equals(existingUser.getGithubUsername())) {
                        existingUser.setGithubUsername(githubUsername);
                    }
                    if (email != null && existingUser.getEmail() == null) {
                        existingUser.setEmail(email);
                    }
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    if (email != null) {
                        return userRepository.findByEmail(email)
                                .map(existingUserByEmail -> {
                                    log.info("Linking GitHub account {} to existing user with email {}", githubId, email);
                                    existingUserByEmail.setGithubId(githubId);
                                    existingUserByEmail.setGithubUsername(githubUsername);
                                    return userRepository.save(existingUserByEmail);
                                })
                                .orElseGet(() -> createNewGitHubUser(githubId, githubUsername, email));
                    }
                    return createNewGitHubUser(githubId, githubUsername, null);
                });

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getGithubUsername());
        log.info("Processed GitHub OAuth login for user: id={}, githubUsername={}", user.getId(), user.getGithubUsername());
        return new AuthResult(user, token);
    }

    private User createNewGitHubUser(String githubId, String githubUsername, String email) {
        log.info("Creating new GitHub OAuth user: githubId={}, username={}", githubId, githubUsername);
        User newUser = User.builder()
                .githubId(githubId)
                .githubUsername(githubUsername)
                .email(email)
                .authProvider(AuthProvider.GITHUB)
                .build();
        return userRepository.save(newUser);
    }
}
