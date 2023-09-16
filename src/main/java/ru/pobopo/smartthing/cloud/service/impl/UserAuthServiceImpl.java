package ru.pobopo.smartthing.cloud.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.service.TokenService;
import ru.pobopo.smartthing.cloud.service.UserAuthService;

@Component
public class UserAuthServiceImpl implements UserAuthService {
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    @Autowired
    public UserAuthServiceImpl(AuthenticationManager authenticationManager, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @Override
    public String authAndGenerateToken(String login, String password) throws ValidationException {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(login, password)
        );
        if (!auth.isAuthenticated()) {
            throw new BadCredentialsException("Wrong user credits");
        }
        return tokenService.generateToken(login);
    }
}
