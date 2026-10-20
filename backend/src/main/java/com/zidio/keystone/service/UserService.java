package com.zidio.keystone.service;
import com.zidio.keystone.domain.AppUser; import com.zidio.keystone.repository.AppUserRepository; import org.springframework.stereotype.Service; import java.util.NoSuchElementException;
@Service public class UserService { private final AppUserRepository repo; public UserService(AppUserRepository repo){this.repo=repo;} public AppUser get(String email){return repo.findByEmail(email).orElseThrow(()->new NoSuchElementException("User not found"));} }
