package com.yugabyte.app.messenger.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import com.yugabyte.app.messenger.data.entity.Profile;

public class UserPrincipal extends User {
    private Profile profileData;

    public UserPrincipal(String username, String password, Collection<? extends GrantedAuthority> authorities,
            Profile profileData) {
        super(username, password, authorities);
        this.profileData = profileData;
    }

    public UserPrincipal(String username, String password, boolean enabled, boolean accountNonExpired,
            boolean credentialsNonExpired, boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities, Profile profileData) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.profileData = profileData;
    }

    public Profile getProfileData() {
        return profileData;
    }
}
