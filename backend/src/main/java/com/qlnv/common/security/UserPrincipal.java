package com.qlnv.common.security;

import com.qlnv.modules.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleName = "ROLE_" + (user.getRole() != null ? user.getRole().toUpperCase() : "USER");
        return Collections.singletonList(new SimpleGrantedAuthority(roleName));
    }

    public Integer getId() {
        return user.getId();
    }

    public String getEmployeeCode() {
        return user.getEmployeeCode();
    }

    public String getRole() {
        return user.getRole();
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return user.getEmployeeCode();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getAccountStatus() != null && user.getAccountStatus() == 1;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getAccountStatus() != null && user.getAccountStatus() == 1;
    }
}
