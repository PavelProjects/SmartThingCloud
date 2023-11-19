package ru.pobopo.smartthing.cloud.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.entity.UserRoleEntity;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.model.Role;
import ru.pobopo.smartthing.cloud.repository.UserRepository;
import ru.pobopo.smartthing.cloud.repository.UserRoleRepository;
import ru.pobopo.smartthing.cloud.service.UserService;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, UserRoleRepository userRoleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserEntity getUser(String login) {
        if (StringUtils.isBlank(login)) {
            return null;
        }
        return userRepository.findByLogin(login);
    }

    @Override
    public UserEntity getUserById(String id) {
        Optional<UserEntity> user = userRepository.findById(id);
        return user.get();
    }

    @Override
    public UserEntity createUser(String login, String password) throws ValidationException {
        if (StringUtils.isBlank(login) || StringUtils.isBlank(password)) {
            throw new ValidationException("Login or password is missing!");
        }
        UserEntity existingUser = getUser(login);
        if (existingUser != null) {
            return existingUser;
        }

        String fixedLogin = login.replaceAll("\\s+","");

        UserEntity user = new UserEntity();
        user.setLogin(fixedLogin);
        user.setPassword(passwordEncoder.encode(password));
        user.setCreationDate(LocalDateTime.now());

        userRepository.save(user);
        grantUserRole(user, Role.USER.getName());
        return user;
    }


    @Override
    public void grantUserRole(UserEntity user, String role) throws ValidationException {
        Objects.requireNonNull(user);
        if (StringUtils.isBlank(role)) {
            throw new ValidationException("Role can't be blank");
        }
        if (userRoleRepository.findByUserIdAndRole(user.getId(), role) != null) {
            return;
        }

        UserRoleEntity userRoleEntity = new UserRoleEntity();
        userRoleEntity.setRole(role);
        userRoleEntity.setUserId(user.getId());
        userRoleRepository.save(userRoleEntity);

        log.info("Granted user {} role {}", user, role);
    }

}
