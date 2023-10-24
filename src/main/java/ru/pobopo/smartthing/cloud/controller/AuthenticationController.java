package ru.pobopo.smartthing.cloud.controller;

import javax.naming.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smartthing.cloud.controller.model.AuthRequest;
import ru.pobopo.smartthing.cloud.controller.model.GenerateTokenRequest;
import ru.pobopo.smartthing.cloud.controller.model.TokenResponse;
import ru.pobopo.smartthing.cloud.dto.AuthorizedUserDto;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.mapper.AuthorizedUserMapper;
import ru.pobopo.smartthing.cloud.model.AuthorizedUser;
import ru.pobopo.smartthing.cloud.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final AuthorizedUserMapper authorizedUserMapper;

    @Autowired
    public AuthenticationController(AuthenticationManager authenticationManager, AuthService authService,
        AuthorizedUserMapper authorizedUserMapper
    ) {
        this.authenticationManager = authenticationManager;
        this.authService = authService;
        this.authorizedUserMapper = authorizedUserMapper;
    }

    @GetMapping
    public AuthorizedUserDto authorizedUser() {
        AuthorizedUser authorizedUser =
            (AuthorizedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return authorizedUserMapper.toDto(authorizedUser);
    }

    @PostMapping("/user")
    public TokenResponse authUser(@RequestBody AuthRequest request) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword())
        );
        if (!auth.isAuthenticated()) {
            throw new BadCredentialsException("Wrong user credits");
        }
        return new TokenResponse(authService.authUser(auth));
    }

    @PostMapping("/gateway")
    public TokenResponse authGateway(@RequestBody GenerateTokenRequest request)
        throws ValidationException, AuthenticationException, AccessDeniedException {
        return new TokenResponse(authService.authGateway(request.getGatewayId()));
    }

    @PostMapping("/user/logout")
    public void userLogout() throws AuthenticationException {
        authService.logout();
    }

    @PostMapping("/gateway/logout")
    public void gatewayLogout(@RequestBody String gatewayId) throws ValidationException, AccessDeniedException, AuthenticationException {
        authService.logoutGateway(gatewayId);
    }
}
