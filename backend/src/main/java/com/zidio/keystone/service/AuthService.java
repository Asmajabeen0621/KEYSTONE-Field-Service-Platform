package com.zidio.keystone.service;
import com.zidio.keystone.dto.*;
import com.zidio.keystone.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Service public class AuthService { private final UserService users; private final PasswordEncoder encoder; private final JwtService jwt; public AuthService(UserService users,PasswordEncoder encoder,JwtService jwt){this.users=users;this.encoder=encoder;this.jwt=jwt;} public LoginResponse login(LoginRequest r){var u=users.get(r.email()); if(!encoder.matches(r.password(),u.getPasswordHash())) throw new IllegalArgumentException("Invalid email or password"); return new LoginResponse(jwt.generate(u.getEmail(),u.getRole().name()),u.getRole().name(),u.getFullName(),u.getEmail());} }
