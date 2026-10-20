package com.zidio.keystone.dto;
import jakarta.validation.constraints.*;
public record SiteRequest(@NotBlank @Size(max=200) String name,@Size(max=500) String address) {}
