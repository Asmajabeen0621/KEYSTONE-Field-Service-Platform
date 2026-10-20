package com.zidio.keystone.dto;
import jakarta.validation.constraints.*;
public record CustomerRequest(@NotBlank @Size(max=200) String name,@Email @Size(max=255) String email,@Size(max=50) String phone) {}
