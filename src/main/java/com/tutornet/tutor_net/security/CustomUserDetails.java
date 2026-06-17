package com.tutornet.tutor_net.security;

import com.tutornet.tutor_net.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = user.getUserRoles().stream()
                .flatMap(ur -> ur.getRole().getPermissions().stream())
                .map(p -> new SimpleGrantedAuthority(p.getSlug()))
                .collect(Collectors.toSet());

        // LOG
        System.out.println("=== AUTHORITIES of " + user.getEmail() + ": " + authorities);
        return authorities;
    }


    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() {
        return !user.getStatus().name().equals("SUSPENDED");
    }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}