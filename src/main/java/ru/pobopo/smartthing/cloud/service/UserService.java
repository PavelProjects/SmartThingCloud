package ru.pobopo.smartthing.cloud.service;

import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.exception.ValidationException;

@Component
public interface UserService {
    String createUser(String login, String password) throws ValidationException;
    UserEntity getUser(String login);
}
