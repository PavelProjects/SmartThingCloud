package ru.pobopo.smartthing.cloud.controller;

import org.springframework.beans.factory.annotation.Autowired;
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
import ru.pobopo.smartthing.cloud.controller.model.GenerateTokenRequest;
import ru.pobopo.smartthing.cloud.controller.model.TokenResponse;
import ru.pobopo.smartthing.cloud.dto.AuthorizedUserDto;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.mapper.AuthorizedUserMapper;
import ru.pobopo.smartthing.cloud.model.AuthorizedUser;
import ru.pobopo.smartthing.cloud.service.AuthService;

import javax.naming.AuthenticationException;
import javax.servlet.http.HttpServletResponse;

import static ru.pobopo.smartthing.cloud.model.Role.Constants.GATEWAY;
import static ru.pobopo.smartthing.cloud.model.Role.Constants.USER;

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

    @RequiredRole(roles = {USER, GATEWAY})
    @GetMapping
    public AuthorizedUserDto authorizedUser(
            @AuthenticationPrincipal AuthorizedUser authorizedUser
    ) {
        return authorizedUserMapper.toDto(authorizedUser);
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
        AuthorizedUser user = authService.authorizeUser(auth);
        ResponseCookie responseCookie = authService.getUserCookie(user);
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
        return authorizedUserMapper.toDto(user);
    }

    @RequiredRole(roles = USER)
    @PostMapping("/gateway")
    public TokenResponse authGateway(@RequestBody GenerateTokenRequest request)
        throws ValidationException, AuthenticationException, AccessDeniedException {
        return new TokenResponse(authService.getGatewayToken(request.getGatewayId(), request.getDays()));
    }

    @RequiredRole(roles = USER)
    @PostMapping("/user/logout")
    public void userLogout() throws AuthenticationException {
        authService.logout();
    }

    @RequiredRole(roles = {USER, GATEWAY})
    @PostMapping("/gateway/logout/{gatewayId}")
    public void gatewayLogout(@PathVariable String gatewayId) throws ValidationException, AccessDeniedException, AuthenticationException {
        authService.logoutGateway(gatewayId);
    }
}
