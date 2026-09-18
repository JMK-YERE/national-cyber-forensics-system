package com.tz.forensics.service;

import com.tz.forensics.dto.UserRegistrationDto;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String registerUser(UserRegistrationDto dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            return "Username already exists!";
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            return "Email already exists!";
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFullName(dto.getFullName());
        user.setOrganization(dto.getOrganization());
        user.setRole("REPORTER");
        user.setEnabled(true);

        userRepository.save(user);
        return "SUCCESS";
    }
}
