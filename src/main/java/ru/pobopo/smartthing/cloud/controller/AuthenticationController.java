package ru.pobopo.smartthing.cloud.controller;

import java.util.List;
import javax.naming.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smartthing.cloud.controller.model.AuthRequest;
import ru.pobopo.smartthing.cloud.controller.model.GenerateTokenRequest;
import ru.pobopo.smartthing.cloud.controller.model.TokenResponse;
import ru.pobopo.smartthing.cloud.dto.GatewayShortDto;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.mapper.GatewayMapper;
import ru.pobopo.smartthing.cloud.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthService authService;
    private final GatewayMapper gatewayMapper;

    @Autowired
    public AuthenticationController(AuthService authService, GatewayMapper gatewayMapper) {
        this.authService = authService;
        this.gatewayMapper = gatewayMapper;
    }

    @PostMapping("/user")
    public TokenResponse authUser(@RequestBody AuthRequest request)
        throws ValidationException, AuthenticationException, AccessDeniedException {
        return new TokenResponse(authService.authUser(request.getLogin(), request.getPassword()));
    }

    @PostMapping("/gateway")
    public TokenResponse authGateway(@RequestBody GenerateTokenRequest request)
        throws ValidationException, AuthenticationException, AccessDeniedException {
        return new TokenResponse(authService.authGateway(request.getGatewayId()));
    }

    @PostMapping("/user/logout")
    public void userLogout() throws ValidationException, AccessDeniedException, AuthenticationException {
        authService.userLogout();
    }

    @PostMapping("/gateway/logout")
    public void gatewayLogout() throws ValidationException, AccessDeniedException, AuthenticationException {
        authService.gatewayLogout();
    }

    @GetMapping("/gateway/list")
    public List<GatewayShortDto> getAuthenticatedGateways()
        throws ValidationException, AuthenticationException {
        return gatewayMapper.toShortDto(authService.getUserAuthorizedGateways());
    }
}
