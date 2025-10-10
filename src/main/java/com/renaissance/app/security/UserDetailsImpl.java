package com.renaissance.app.security;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.renaissance.app.model.Role;
import com.renaissance.app.model.User;

/**
 * Custom implementation of Spring Security's UserDetails.
 * Wraps around the User entity and exposes required security information.
 */
public class UserDetailsImpl implements UserDetails {

    private final Long id;
    private final String username; // use username instead of email for consistency
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Role role;
    private final boolean enabled;

    public UserDetailsImpl(Long id,
                           String username,
                           String email,
                           String password,
                           Collection<? extends GrantedAuthority> authorities,
                           Role role,
                           boolean enabled) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.role = role;
        this.enabled = enabled;
    }

    /**
     * Build UserDetailsImpl from User entity.
     */
    public static UserDetailsImpl build(User user) {
        List<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        return new UserDetailsImpl(
                user.getUserId(),
                user.getUsername(),     // NEW: use username field
                user.getEmail(),
                user.getPassword(),
                authorities,
                user.getRole(),
                user.isEmailVerified()  // enable only if email is verified
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Spring Security uses this for login. 
     * We're returning the username, not email, for consistency.
     */
    @Override
    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // can extend later
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // can extend later
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // can extend later
    }

    @Override
    public boolean isEnabled() {
        return enabled; // tied to email verification
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserDetailsImpl that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
