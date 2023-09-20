package ru.pobopo.smartthing.cloud.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.pobopo.smartthing.cloud.context.ContextHolder;
import ru.pobopo.smartthing.cloud.service.TokenService;

@Component
public class SecurityFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(SecurityFilter.class);
    private static final String USER_TOKEN_HEADER = "SmartThing-Token-User";
    private static final String GATEWAY_TOKEN_HEADER = "SmartThing-Token-Gateway";

    private final TokenService tokenService;

    @Autowired
    public SecurityFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = StringUtils.firstNonBlank(request.getHeader(USER_TOKEN_HEADER), request.getHeader(GATEWAY_TOKEN_HEADER));
            if (StringUtils.isNotBlank(token)) {
                try {
                    setUserDetailsToContext(tokenService.validateToken(token), request);
                } catch (Exception e) {
                    log.error("Token validation failed: {}", e.getMessage());
                }
            }
        } else {
            log.warn("Security context is already present");
        }

        filterChain.doFilter(request, response);

        SecurityContextHolder.clearContext();
        ContextHolder.clearContext();
    }

    private void setUserDetailsToContext(UserDetails userDetails, HttpServletRequest request) {
        if (userDetails == null) {
            return;
        }
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities()
        );
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
}
