package ru.pobopo.smartthing.cloud.service.impl;

import org.apache.commons.lang3.StringUtils;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.repository.UserRepository;
import ru.pobopo.smartthing.cloud.service.UserService;

@Component
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
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
    public String createUser(String login, String password) throws ValidationException {
        if (StringUtils.isBlank(login) || StringUtils.isBlank(password)) {
            throw new ValidationException("Login or password is missing!");
        }
        if (getUser(login) != null) {
            throw new ValidationException("User with this login already exist!");
        }

        String fixedLogin = login.replaceAll("\\s+","");

        UserEntity user = new UserEntity();
        user.setLogin(fixedLogin);
        user.setPassword(passwordEncoder.encode(password));
        user.setCreationDate(LocalDateTime.now());

        userRepository.save(user);
        return user.getId();
    }
}
