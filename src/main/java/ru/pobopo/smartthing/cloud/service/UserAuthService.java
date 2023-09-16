package ru.pobopo.smartthing.cloud.service;

import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.exception.ValidationException;

@Component
public interface UserAuthService {
    String authAndGenerateToken(String login, String password) throws ValidationException;
}
