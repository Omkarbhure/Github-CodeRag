package com.example.coderag.security;

import com.example.coderag.model.User;
import com.example.coderag.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String emailOrId) throws UsernameNotFoundException {
        User user;
        try {
            UUID id = UUID.fromString(emailOrId);
            user = userRepository.findById(id)
                    .orElseGet(() -> userRepository.findByEmail(emailOrId)
                            .orElseThrow(() -> new UsernameNotFoundException("User not found with id or email: " + emailOrId)));
        } catch (IllegalArgumentException e) {
            user = userRepository.findByEmail(emailOrId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + emailOrId));
        }

        return UserPrincipal.create(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        return UserPrincipal.create(user);
    }
}
