package ru.pobopo.smartthing.cloud.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtTokenUtil {
    private static final long serialVersionUID = -2550185165626007488L;

    private final Key key;
    private final JwtParser parser;

    @Autowired
    public JwtTokenUtil(Environment env) {
        String secret = env.getProperty("JWT_SECRET");
        if (StringUtils.isBlank(secret)) {
            log.error("JWT_SECRET env variable missing! generating new random key for JWT.");
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes());
        }
        this.parser = Jwts.parserBuilder().setSigningKey(key).build();
    }

    //retrieve subject from jwt token
    public String getTokenSubject(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    //retrieve expiration date from jwt token
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
    //for retrieveing any information from token we will need the secret key
    public Claims getAllClaimsFromToken(String token) {
        return parser.parseClaimsJws(token).getBody();
    }

    //check if the token has expired
    public boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration != null && expiration.before(new Date());
    }
    // ttl - seconds
    public String doGenerateToken(String subject, Map<String, Object> claims, long ttl) {
         JwtBuilder builder = Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .signWith(key, SignatureAlgorithm.HS512);

        if (ttl > 0) {
            builder.setExpiration(new Date(System.currentTimeMillis() + ttl * 1000));
        }
        return builder.compact();
    }
}
