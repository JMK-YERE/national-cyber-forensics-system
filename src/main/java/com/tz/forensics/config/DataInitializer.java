package com.tz.forensics.config;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.initial-admin-username:}")
    private String initialAdminUsername;

    @Value("${app.initial-admin-password:}")
    private String initialAdminPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (initialAdminUsername == null || initialAdminUsername.isBlank() || initialAdminPassword == null || initialAdminPassword.length() < 12) {
            return;
        }
        if (!userRepository.existsByUsername(initialAdminUsername)) {
            User admin = new User();
            admin.setUsername(initialAdminUsername);
            admin.setEmail("admin@cyber.go.tz");
            admin.setPassword(passwordEncoder.encode(initialAdminPassword));
            admin.setFullName("System Administrator");
            admin.setRole("ADMIN");
            admin.setOrganization("National Cyber Security - Tanzania");
            admin.setEnabled(true);
            userRepository.save(admin);
            System.out.println("✅ Initial admin account created from configured environment credentials.");
        }
    }
}
