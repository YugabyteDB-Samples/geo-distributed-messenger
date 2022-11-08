package com.yugabyte.app.messenger.security;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletRequest;
import com.yugabyte.app.messenger.data.entity.Profile;
import com.yugabyte.app.messenger.data.repository.ProfileRepository;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUser {

    private final ProfileRepository userRepository;

    @Autowired
    public AuthenticatedUser(ProfileRepository userRepository) {
        this.userRepository = userRepository;
    }

    private Optional<Authentication> getAuthentication() {
        SecurityContext context = SecurityContextHolder.getContext();

        return Optional.ofNullable(context.getAuthentication())
                .filter(authentication -> !(authentication instanceof AnonymousAuthenticationToken));
    }

    public Optional<Profile> get() {
        Optional<Authentication> authentication = getAuthentication();

        if (authentication.isPresent()) {
            UserPrincipal userToken = (UserPrincipal) authentication.get().getPrincipal();
            return Optional.of(userToken.getProfileData());
        }

        Optional<Profile> profile = authentication.map(authToken -> userRepository.findByEmail(authToken.getName()));

        return profile;
    }

    public void logout() {
        UI.getCurrent().getPage().setLocation(SecurityConfiguration.LOGOUT_URL);
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.logout(VaadinServletRequest.getCurrent().getHttpServletRequest(), null, null);
    }
}
