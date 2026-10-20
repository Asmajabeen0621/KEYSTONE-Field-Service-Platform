package com.zidio.keystone.dto; import jakarta.validation.constraints.*; public record LoginRequest(@Email @NotBlank String email,@NotBlank String password){}
