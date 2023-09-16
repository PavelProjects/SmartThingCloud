package ru.pobopo.smartthing.cloud.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smartthing.cloud.controller.model.AuthRequest;
import ru.pobopo.smartthing.cloud.controller.model.TokenResponse;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.service.UserAuthService;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserAuthService userAuthService;

    @Autowired
    public UserController(UserAuthService userAuthService) {
        this.userAuthService = userAuthService;
    }

    @PostMapping("/auth")
    public TokenResponse authUser(@RequestBody AuthRequest request) throws ValidationException {
        String token = userAuthService.authAndGenerateToken(request.getLogin(), request.getPassword());
        return new TokenResponse(token);
    }

}
