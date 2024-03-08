package ru.pobopo.smartthing.cloud.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.jwt.JwtTokenUtil;
import ru.pobopo.smartthing.cloud.model.AuthenticatedUser;
import ru.pobopo.smartthing.cloud.service.GatewayAuthService;
import ru.pobopo.smartthing.cloud.service.UserAuthService;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static ru.pobopo.smartthing.cloud.service.UserAuthService.USER_COOKIE_NAME;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {
    private static final String USER_TOKEN_HEADER = "SmartThing-Token-User";
    private static final String GATEWAY_TOKEN_HEADER = "SmartThing-Token-Gateway";

    private final JwtTokenUtil jwtTokenUtil;
    private final UserAuthService userAuthService;
    private final GatewayAuthService gatewayAuthService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Cookie cookie = WebUtils.getCookie(request, USER_COOKIE_NAME);
        String token = StringUtils.firstNonBlank(
                cookie != null ? cookie.getValue() : null,
                request.getHeader(USER_TOKEN_HEADER),
                request.getHeader(GATEWAY_TOKEN_HEADER)
        );
        if (StringUtils.isNotBlank(token)) {
            try {
                AuthenticatedUser authenticatedUser = parseToken(token);
                switch (authenticatedUser.getTokenType()) {
                    case USER: {
                        userAuthService.validate(authenticatedUser);
                        break;
                    }
                    case GATEWAY: {
                        gatewayAuthService.validate(authenticatedUser, token);
                        break;
                    }
                }
                setUserDetailsToContext(authenticatedUser, request);
            } catch (Exception e) {
                log.error("Token validation failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private AuthenticatedUser parseToken(String token) throws AccessDeniedException {
        if (jwtTokenUtil.isTokenExpired(token)) {
            throw new AccessDeniedException("Token expired!");
        }
        return AuthenticatedUser.fromClaims(jwtTokenUtil.getAllClaimsFromToken(token));
    }

    private void setUserDetailsToContext(AuthenticatedUser authenticatedUser, HttpServletRequest request) {
        if (authenticatedUser == null) {
            return;
        }
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
            "",
            authenticatedUser.getAuthorities()
        );
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
}
