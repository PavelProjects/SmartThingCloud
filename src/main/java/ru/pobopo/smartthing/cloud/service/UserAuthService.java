package ru.pobopo.smartthing.cloud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.jwt.JwtTokenUtil;
import ru.pobopo.smartthing.cloud.model.AuthorizedUser;
import ru.pobopo.smartthing.cloud.model.TokenType;
import ru.pobopo.smartthing.cloud.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthService {
    public static final String USER_COOKIE_NAME = "SMTJwt";

    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;

    @Value("${jwt.token.ttl}")
    private long tokenTimeToLive;

    public AuthorizedUser authenticate(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        UserEntity user = userRepository.findByLogin(userDetails.getUsername());
        return AuthorizedUser.build(TokenType.USER, user, userDetails.getAuthorities());
    }

    public String generateToken(AuthorizedUser authorizedUser) {
        return jwtTokenUtil.doGenerateToken(
                authorizedUser.getTokenType().getName(),
                authorizedUser.toClaims(),
                tokenTimeToLive
        );
    }

    public ResponseCookie getUserCookie(AuthorizedUser authorizedUser) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(
                        USER_COOKIE_NAME,
                        generateToken(authorizedUser)
                )
                .path("/")
                .maxAge(tokenTimeToLive)
                .httpOnly(true);
        return builder.build();
    }

    public void validate(AuthorizedUser authorizedUser) throws AccessDeniedException {
        if (authorizedUser.getUser() == null) {
            log.error("Missing user in token");
            throw new AccessDeniedException("Bad token");
        }
        if (!userRepository.existsById(authorizedUser.getUser().getId())) {
            throw new AccessDeniedException("User not found");
        }
    }
}
