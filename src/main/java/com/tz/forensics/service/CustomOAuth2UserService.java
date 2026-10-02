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

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = new User();
            user.setUsername(email.split("@")[0] + "_" + provider);
            user.setEmail(email);
            user.setFullName(name != null ? name : user.getUsername());
            user.setPassword("OAUTH2_USER_NO_PASSWORD");
            user.setRole("INDIVIDUAL");
            user.setEnabled(true);
            user.setOauthProvider(provider);
            user.setOauthId(sub);
            user.setProfilePicture(picture);
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("New OAuth user created: {}", user.getUsername());
        } else {
            if (user.getOauthProvider() == null) {
                user.setOauthProvider(provider);
                user.setOauthId(sub);
                user.setProfilePicture(picture);
                userRepository.save(user);
            }
        }
        return oauth2User;
    }
}
