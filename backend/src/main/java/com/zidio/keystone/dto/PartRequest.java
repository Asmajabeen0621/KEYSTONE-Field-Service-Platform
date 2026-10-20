package com.zidio.keystone.dto; import jakarta.validation.constraints.*; public record PartRequest(@NotNull Long partId,@Min(1) int quantity) {}
