package ru.pobopo.smartthing.cloud.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smartthing.cloud.annotation.RequiredRole;
import ru.pobopo.smartthing.cloud.controller.model.AuthRequest;
import ru.pobopo.smartthing.cloud.controller.model.GatewayTokenRequest;
import ru.pobopo.smartthing.cloud.controller.model.TokenResponse;
import ru.pobopo.smartthing.cloud.dto.AuthorizedUserDto;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.mapper.AuthorizedUserMapper;
import ru.pobopo.smartthing.cloud.model.AuthenticatedUser;
import ru.pobopo.smartthing.cloud.service.GatewayAuthService;
import ru.pobopo.smartthing.cloud.service.UserAuthService;

import javax.naming.AuthenticationException;
import javax.servlet.http.HttpServletResponse;

import static ru.pobopo.smartthing.cloud.model.Role.Constants.GATEWAY;
import static ru.pobopo.smartthing.cloud.model.Role.Constants.USER;

@RestController
@RequestMapping("/auth")
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
    public AuthorizedUserDto authUser(
            HttpServletResponse response,
            @RequestBody AuthRequest request
    ) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword())
        );
        if (!auth.isAuthenticated()) {
            throw new BadCredentialsException("Wrong user credits");
        }
        AuthenticatedUser user = userAuthService.authenticate(auth);
        ResponseCookie responseCookie = userAuthService.buildCookie(user);
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
        return authorizedUserMapper.toDto(user);
    }

    @RequiredRole(roles = USER)
    @PostMapping("/gateway")
    public TokenResponse authGateway(@RequestBody GatewayTokenRequest request)
        throws ValidationException, AuthenticationException, AccessDeniedException {
        return new TokenResponse(gatewayAuthService.generateToken(request.getGatewayId()));
    }

    @RequiredRole(roles = USER)
    @PostMapping("/user/logout")
    public void userLogout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, userAuthService.logoutCookie().toString());
    }

    @RequiredRole(roles = {USER, GATEWAY})
    @PostMapping("/gateway/logout")
    public void gatewayLogout(@RequestBody GatewayTokenRequest request) throws Exception {
        gatewayAuthService.logout(request.getGatewayId());
    }
}
