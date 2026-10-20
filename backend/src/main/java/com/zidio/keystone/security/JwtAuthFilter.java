package com.zidio.keystone.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        System.out.println(
                "JWT DEBUG - Request: "
                        + request.getMethod()
                        + " "
                        + request.getRequestURI()
        );

        System.out.println(
                "JWT DEBUG - Authorization header present: "
                        + (authorizationHeader != null)
        );

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            System.out.println(
                    "JWT DEBUG - No Bearer token"
            );

            filterChain.doFilter(request, response);
            return;
        }

        String token =
                authorizationHeader.substring(7).trim();

        try {

            var claims = jwtService.parse(token);

            String email = claims.getSubject();

            String role =
                    claims.get("role", String.class);

            System.out.println(
                    "JWT DEBUG - Email: "
                            + email
            );

            System.out.println(
                    "JWT DEBUG - Role: "
                            + role
            );

            var authority =
                    new SimpleGrantedAuthority(
                            "ROLE_" + role
                    );

            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(authority)
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            System.out.println(
                    "JWT DEBUG - Authentication set: "
                            + SecurityContextHolder
                            .getContext()
                            .getAuthentication()
            );

            System.out.println(
                    "JWT DEBUG - Authorities: "
                            + authentication.getAuthorities()
            );

        } catch (Exception exception) {

            System.out.println(
                    "JWT DEBUG - TOKEN ERROR: "
                            + exception.getMessage()
            );

            SecurityContextHolder.clearContext();

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.setContentType(
                    "application/json"
            );

            response.getWriter().write(
                    """
                    {
                        "status": 401,
                        "message": "Invalid or expired JWT token"
                    }
                    """
            );

            return;
        }

        filterChain.doFilter(request, response);
    }
}