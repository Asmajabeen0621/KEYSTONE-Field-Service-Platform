package com.zidio.keystone.dto; import jakarta.validation.constraints.*; public record TimeRequest(@Min(1) int minutes,@Size(max=1000) String note) {}
