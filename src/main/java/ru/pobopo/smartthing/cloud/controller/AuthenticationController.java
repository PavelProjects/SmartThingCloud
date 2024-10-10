package ru.pobopo.smartthing.cloud.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smartthing.cloud.aspect.RequiredRole;
import ru.pobopo.smartthing.cloud.controller.dto.*;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.mapper.AuthorizedUserMapper;
import ru.pobopo.smartthing.cloud.model.AuthenticatedUser;
import ru.pobopo.smartthing.cloud.service.gateway.GatewayAuthService;
import ru.pobopo.smartthing.cloud.service.user.UserAuthService;

import javax.naming.AuthenticationException;

import static ru.pobopo.smartthing.cloud.model.Role.Constants.GATEWAY;
import static ru.pobopo.smartthing.cloud.model.Role.Constants.USER;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationManager authenticationManager;
    private final AuthorizedUserMapper authorizedUserMapper;
    private final UserAuthService userAuthService;
    private final GatewayAuthService gatewayAuthService;

    @RequiredRole(roles = {USER, GATEWAY})
    @GetMapping
    public AuthorizedUserDto authorizedUser(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return authorizedUserMapper.toDto(authenticatedUser);
    }

    @PostMapping("/user")
    public ResponseEntity<UserTokenPair> authUser(
            @RequestBody AuthRequest request
    ) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword())
        );
        if (!auth.isAuthenticated()) {
            throw new BadCredentialsException("Wrong user credits");
        }
        return userAuthService.authenticate(auth);
    }

    @PostMapping("/user/refresh")
    public UserTokenPair refreshUserToken(
            @RequestBody RefreshRequest request,
            HttpServletResponse response
    ) {
        return userAuthService.refreshToken(request.getRefreshToken(), response);
    }

    @RequiredRole(roles = USER)
    @PostMapping("/gateway/{gatewayId}")
    public TokenResponse authGateway(@PathVariable String gatewayId)
        throws ValidationException, AuthenticationException, AccessDeniedException {
        return new TokenResponse(gatewayAuthService.generateToken(gatewayId));
    }

    @RequiredRole(roles = USER)
    @PostMapping("/user/logout")
    public void userLogout(@AuthenticationPrincipal AuthenticatedUser user, HttpServletResponse response) {
        userAuthService.logout(user, response);
    }

    @RequiredRole(roles = {USER, GATEWAY})
    @PostMapping("/gateway/logout/{gatewayId}")
    public void gatewayLogout(@PathVariable String gatewayId) throws Exception {
        gatewayAuthService.logout(gatewayId);
    }
}
