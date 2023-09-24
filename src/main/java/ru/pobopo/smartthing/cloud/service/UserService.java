package ru.pobopo.smartthing.cloud.service;

import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.exception.ValidationException;

@Component
public interface UserService {
    UserEntity createUser(String login, String password) throws ValidationException;

    UserEntity getUserById(String id);
    UserEntity getUser(String login);

    void grantUserRole(UserEntity user, String role) throws ValidationException;
}
