package org.example.nura.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String createAccessToken(Long userId) {
        return createToken(
                userId,
                accessTokenExpiration,
                "ACCESS"
        );
    }

    public String createRefreshToken(Long userId) {
        return createToken(
                userId,
                refreshTokenExpiration,
                "REFRESH"
        );
    }

    private String createToken(
            Long userId,
            long expiration,
            String tokenType
    ) {
        Date now = new Date();
        Date expiryDate =
                new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", tokenType)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public Long getUserId(String token) {
        Claims claims = parseClaims(token);

        return Long.valueOf(claims.getSubject());
    }

    public void validateToken(String token) {
        parseClaims(token);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new BaseException(ErrorCode.EXPIRED_TOKEN);

        } catch (JwtException | IllegalArgumentException e) {
            throw new BaseException(ErrorCode.INVALID_TOKEN);
        }
    }

    public Long getUserIdFromAccessToken(String token) {

        Claims claims = parseClaims(token);

        String tokenType =
                claims.get("type", String.class);

        if (!"ACCESS".equals(tokenType)) {
            throw new BaseException(
                    ErrorCode.INVALID_TOKEN
            );
        }

        return Long.valueOf(
                claims.getSubject()
        );
    }

    public Long getUserIdFromRefreshToken(String token) {

        Claims claims = parseClaims(token);

        String tokenType =
                claims.get("type", String.class);

        if (!"REFRESH".equals(tokenType)) {
            throw new BaseException(
                    ErrorCode.INVALID_TOKEN
            );
        }

        return Long.valueOf(
                claims.getSubject()
        );
    }
}
