package com.zidio.keystone.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;

    private final long expirationMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes
    ) {

        this.secretKey =
                Keys.hmacShaKeyFor(
                        secret.getBytes(StandardCharsets.UTF_8)
                );

        this.expirationMinutes = expirationMinutes;
    }

    /*
     * Creates a signed JWT for the logged-in user.
     */
    public String generate(
            String email,
            String role
    ) {

        Instant now = Instant.now();

        Instant expiration =
                now.plusSeconds(
                        expirationMinutes * 60
                );

        return Jwts.builder()

                // User identity
                .subject(email)

                // User role
                .claim("role", role)

                // Token creation time
                .issuedAt(
                        Date.from(now)
                )

                // Token expiry time
                .expiration(
                        Date.from(expiration)
                )

                // Sign the token
                .signWith(secretKey)

                .compact();
    }

    /*
     * Validates the JWT signature and expiry
     * and returns its claims.
     */
    public Claims parse(String token) {

        return Jwts.parser()

                .verifyWith(secretKey)

                .build()

                .parseSignedClaims(token)

                .getPayload();
    }
}