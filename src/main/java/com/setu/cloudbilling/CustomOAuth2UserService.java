package com.setu.cloudbilling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String providerName = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null) {
            throw new OAuth2AuthenticationException("OAuth2 provider did not return an email");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);

        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();

            if (user.getProvider() != AuthProvider.valueOf(providerName)) {
                // Optionally update provider to reflect linked account
                user.setProvider(AuthProvider.valueOf(providerName));
                if (user.getName() == null && name != null) user.setName(name);
                userRepository.save(user);
                System.out.println("Account Linked: Local user " + email + " just logged in with " + providerName);
            }
        } else {
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setProvider(AuthProvider.valueOf(providerName));
            // No password for OAuth
            userRepository.save(user);
        }

        return oAuth2User;
    }
}
