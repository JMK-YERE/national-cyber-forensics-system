package com.tz.forensics.config;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@cyber.go.tz");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setFullName("System Administrator");
            admin.setRole("ADMIN");
            admin.setOrganization("National Cyber Security - Tanzania");
            admin.setEnabled(true);
            userRepository.save(admin);
            System.out.println("✅ Admin created: admin / Admin@123");
        }
    }
}
