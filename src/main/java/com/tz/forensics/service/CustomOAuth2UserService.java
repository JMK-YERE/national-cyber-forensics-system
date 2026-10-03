package com.tz.forensics.service;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");
        String sub = oauth2User.getAttribute("sub");

        log.info("OAuth2 login: provider={}, email={}", provider, email);

        if (email == null) {
            email = (sub != null ? sub : "oauth") + "@" + provider + ".local";
        }

        final String finalEmail = email;
        User user = userRepository.findByEmail(finalEmail).orElse(null);
        if (user == null) {
            user = new User();
            String username = finalEmail.split("@")[0];
            // Hakikisha username haipo
            if (userRepository.existsByUsername(username)) {
                username = username + "_" + System.currentTimeMillis() % 10000;
            }
            user.setUsername(username);
            user.setEmail(finalEmail);
            user.setFullName(name != null ? name : username);
            user.setPassword("$2a$10$OAUTH2USERPLACEHOLDERxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
            user.setRole("INDIVIDUAL");
            user.setEnabled(true);
            user.setEmailVerified(true);
            user.setApprovalStatus("APPROVED");
            user.setOauthProvider(provider);
            user.setOauthId(sub);
            user.setProfilePicture(limit(picture, 450));
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("New OAuth user created: {}", user.getUsername());
        } else {
            if (user.getOauthProvider() == null) {
                user.setOauthProvider(provider);
                user.setOauthId(sub);
                user.setProfilePicture(limit(picture, 2048));
                userRepository.save(user);
            }
        }
        return oauth2User;
    }

    private String limit(String value, int max) {
        if (value == null) return null;
        String v = value.trim();
        return v.length() <= max ? v : v.substring(0, max);
    }
}
