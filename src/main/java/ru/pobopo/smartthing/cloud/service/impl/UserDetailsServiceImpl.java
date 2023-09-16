package ru.pobopo.smartthing.cloud.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.repository.UserRepository;

@Component
@Scope("singleton")
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByLogin(username);
        if (null != user) {
            return new User(user.getLogin(), user.getPassword(), List.of(new SimpleGrantedAuthority("admin_boi")));
        } else {
            throw new UsernameNotFoundException("User not found: " + username);
        }
    }
}