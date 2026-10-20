package com.zidio.keystone.dto; import com.zidio.keystone.domain.Priority; import jakarta.validation.constraints.*;
public record WorkOrderRequest(@NotBlank @Size(max=255) String title,@Size(max=4000) String description,@NotNull Priority priority,@NotNull Long customerId,@NotNull Long siteId) {}
