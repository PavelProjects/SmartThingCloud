package ru.pobopo.smartthing.cloud.model;

import io.jsonwebtoken.Claims;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.UserEntity;

import javax.security.auth.Subject;
import java.io.Serializable;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
public class AuthenticatedUser implements Serializable, Principal {
    private final static String CLAIM_TOKEN_TYPE = "token_type";
    private final static String CLAIM_USER_ID = "user_id";
    private final static String CLAIM_USER_LOGIN = "user_login";
    private final static String CLAIM_USER_AUTHORITIES = "user_authorities";
    private final static String CLAIM_GATEWAY_ID = "gateway_id";
    private final static String CLAIM_GATEWAY_NAME = "gateway_name";
    private final static String CLAIM_GATEWAY_DESCRIPTION = "gateway_description";

    private final TokenType tokenType;
    private final UserEntity user;
    private final Collection<SimpleGrantedAuthority> authorities;
    private final GatewayEntity gateway;

    public static AuthenticatedUser build(TokenType tokenType, UserEntity user, Collection<? extends GrantedAuthority> authorities) {
        return build(tokenType, user, authorities, null);
    }
    
    public static AuthenticatedUser build(
        TokenType tokenType,
        UserEntity user,
        Collection<? extends GrantedAuthority> authorities,
        GatewayEntity gateway
    ) {
        Objects.requireNonNull(tokenType);
        Objects.requireNonNull(user);

        AuthenticatedUserBuilder builder = new AuthenticatedUserBuilder();
        builder
            .tokenType(tokenType)
            .user(user)
            .gateway(gateway);

        switch (tokenType) {
            case USER:
                builder.authorities(
                    authorities.stream().map((a) -> new SimpleGrantedAuthority(a.getAuthority())).collect(Collectors.toList())
                );
                break;
            case GATEWAY:
                builder.authorities(List.of(new SimpleGrantedAuthority(Role.GATEWAY.getName())));
                break;
        }

        return builder.build();
    }

    public static AuthenticatedUser fromClaims(Claims claims) {
        TokenType tokenType = TokenType.fromString((String) claims.get(CLAIM_TOKEN_TYPE));

        UserEntity user = new UserEntity();
        user.setId((String) claims.get(CLAIM_USER_ID));
        user.setLogin((String) claims.get(CLAIM_USER_LOGIN));

        Collection<String> authorities = (Collection<String>) claims.get(CLAIM_USER_AUTHORITIES);

        GatewayEntity gatewayEntity = null;
        if (claims.containsKey(CLAIM_GATEWAY_ID)) {
            gatewayEntity = new GatewayEntity();
            gatewayEntity.setId((String) claims.get(CLAIM_GATEWAY_ID));
            gatewayEntity.setName((String) claims.get(CLAIM_GATEWAY_NAME));
            gatewayEntity.setDescription((String) claims.get(CLAIM_GATEWAY_DESCRIPTION));
        }

        return build(
            tokenType,
            user,
            authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()),
            gatewayEntity
        );
    }

    public Map<String, Object> toClaims() {
        HashMap<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, tokenType.getName());

        claims.put(CLAIM_USER_ID, user.getId());
        claims.put(CLAIM_USER_LOGIN, user.getLogin());
        claims.put(CLAIM_USER_AUTHORITIES, authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));

        if (gateway != null) {
            claims.put(CLAIM_GATEWAY_ID, gateway.getId());
            claims.put(CLAIM_GATEWAY_NAME, gateway.getName());
            claims.put(CLAIM_GATEWAY_DESCRIPTION, gateway.getDescription());
        }

        return claims;
    }

    @Override
    public String toString() {
        return String.format(
            "[Type=%s, UserId=%s, UserLogin=%s, GatewayId=%s]",
            tokenType.getName(),
            user.getId(),
            user.getLogin(),
            gateway != null ? gateway.getId() : ""
        );
    }

    @Override
    public String getName() {
        return tokenType.equals(TokenType.GATEWAY) && gateway != null ? gateway.getId() : user.getLogin();
    }

    @Override
    public boolean implies(Subject subject) {
        return Principal.super.implies(subject);
    }
}
