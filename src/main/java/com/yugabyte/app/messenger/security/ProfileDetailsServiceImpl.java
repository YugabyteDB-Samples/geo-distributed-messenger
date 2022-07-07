package com.yugabyte.app.messenger.security;

import com.yugabyte.app.messenger.data.entity.Profile;
import com.yugabyte.app.messenger.data.service.ProfileRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ProfileDetailsServiceImpl implements UserDetailsService {

    private final ProfileRepository userRepository;

    @Autowired
    public ProfileDetailsServiceImpl(ProfileRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Profile user = userRepository.findByEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException("No user present with username: " + username);
        } else {
            return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getHashedPassword(),
                    getAuthorities(user));
        }
    }

    private static List<GrantedAuthority> getAuthorities(Profile user) {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

    }

}
