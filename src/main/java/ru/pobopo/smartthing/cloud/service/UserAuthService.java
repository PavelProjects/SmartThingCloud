package ru.pobopo.smartthing.cloud.service;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import ru.pobopo.smartthing.cloud.dto.UserTokenPair;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.entity.UserTokenEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.jwt.JwtTokenUtil;
import ru.pobopo.smartthing.cloud.model.AuthenticatedUser;
import ru.pobopo.smartthing.cloud.model.TokenType;
import ru.pobopo.smartthing.cloud.repository.UserRepository;
import ru.pobopo.smartthing.cloud.repository.UserTokenRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static ru.pobopo.smartthing.cloud.entity.UserTokenEntity.CLAIM_TOKEN_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthService {
    public static final String USER_COOKIE_NAME = "SMTJwt";

    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;
    private final UserTokenRepository tokenRepository;

    @Value("${jwt.token.ttl}")
    private long tokenTtl;

    @Value("${jwt.token.refresh.ttl}")
    private long refreshTokenTtl;

    @Transactional
    public ResponseEntity<UserTokenPair> authenticate(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        UserEntity user = userRepository.findByLogin(userDetails.getUsername());

        Optional<UserTokenEntity> tokenEntity = tokenRepository.findByUser(user);
        if (tokenEntity.isPresent()) {
            log.warn("Deleting old user token {}", tokenEntity);
            tokenRepository.delete(tokenEntity.get());
        }

        UserTokenPair tokenPair = createNewTokenPair(AuthenticatedUser.build(TokenType.USER, user, userDetails.getAuthorities()));

        log.info("User {} is authenticated", user);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, buildCookie(tokenPair.getToken()).toString());
        return new ResponseEntity<>(tokenPair, headers, HttpStatus.OK);
    }

    @Transactional
    public UserTokenPair refreshToken(String refreshToken, HttpServletResponse response) {
        if (jwtTokenUtil.isTokenExpired(refreshToken)) {
            throw new AccessDeniedException("Refresh token expired!");
        }
        String tokenId = (String) jwtTokenUtil.getAllClaimsFromToken(refreshToken).get(CLAIM_TOKEN_ID);
        if (StringUtils.isBlank(refreshToken)) {
            throw new AccessDeniedException("No id in token");
        }
        Optional<UserTokenEntity> tokenEntity = tokenRepository.findById(tokenId);
        if (tokenEntity.isEmpty()) {
            throw new AccessDeniedException("This refresh token was already used!");
        }
        UserEntity user = tokenEntity.get().getUser();
        log.info("Refreshing token for user {}", user);

        tokenRepository.delete(tokenEntity.get());
        log.warn("Old token for user {} was deleted", user);

        UserTokenPair tokenPair = createNewTokenPair(AuthenticatedUser.build(TokenType.USER, user, userDetailsService.getAuthorities(user)));
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(tokenPair.getToken()).toString());

        log.info("Token for user {} was refreshed", user);
        return tokenPair;
    }

    public void logout(AuthenticatedUser user, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, logoutCookie().toString());
        Optional<UserTokenEntity> tokenEntity = tokenRepository.findByUser(user.getUser());
        if (tokenEntity.isPresent()) {
            tokenRepository.delete(tokenEntity.get());
            log.warn("Refresh token {} was deleted", tokenEntity.get());
        }
    }

    public void validate(AuthenticatedUser authenticatedUser) throws AccessDeniedException {
        if (authenticatedUser.getUser() == null) {
            log.error("Missing user in token");
            throw new AccessDeniedException("Bad token");
        }
        if (!userRepository.existsById(authenticatedUser.getUser().getId())) {
            throw new AccessDeniedException("User not found");
        }
    }

    private ResponseCookie buildCookie(String token) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(
                        USER_COOKIE_NAME,
                        token
                )
                .path("/")
                .maxAge(tokenTtl)
                .httpOnly(true);
        return builder.build();
    }

    private ResponseCookie logoutCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(
                        USER_COOKIE_NAME,
                        ""
                )
                .path("/")
                .maxAge(1)
                .httpOnly(true);

        return builder.build();
    }

    private UserTokenPair createNewTokenPair(AuthenticatedUser authenticatedUser) {
        UserTokenEntity refreshToken = new UserTokenEntity();
        refreshToken.setUser(authenticatedUser.getUser());
        refreshToken.setCreationDate(LocalDateTime.now());
        tokenRepository.save(refreshToken);
        log.info("New refresh token was created: {}", refreshToken);

        return UserTokenPair.builder()
                .token(jwtTokenUtil.doGenerateToken(
                        TokenType.USER.getName(),
                        authenticatedUser.toClaims(),
                        tokenTtl
                ))
                .refresh(jwtTokenUtil.doGenerateToken(
                        TokenType.USER_REFRESH.getName(),
                        refreshToken.toClaims(),
                        refreshTokenTtl
                ))
                .build();
    }
}
