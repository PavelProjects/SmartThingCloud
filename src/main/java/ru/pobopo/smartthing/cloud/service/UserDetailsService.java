package ru.pobopo.smartthing.cloud.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.entity.UserRoleEntity;
import ru.pobopo.smartthing.cloud.repository.UserRepository;
import ru.pobopo.smartthing.cloud.repository.UserRoleRepository;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Scope("singleton")
public class UserDetailsService {
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Autowired
    public UserDetailsService(UserRepository userRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByLogin(username);
        if (null != user) {
            return new User(user.getLogin(), user.getPassword(), getAuthorities(user));
        } else {
            throw new UsernameNotFoundException("User not found: " + username);
        }
    }

    private List<GrantedAuthority> getAuthorities(UserEntity user) {
        List<UserRoleEntity> roles = userRoleRepository.findByUserId(user.getId());
        if (roles.isEmpty()) {
            return List.of();
        }
        return roles.stream().map(role -> new SimpleGrantedAuthority(role.getRole())).collect(Collectors.toList());
    }
}