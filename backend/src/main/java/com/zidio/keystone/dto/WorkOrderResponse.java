package com.zidio.keystone.dto;

import com.zidio.keystone.domain.*;
import java.time.Instant;

public record WorkOrderResponse(
        Long id,
        String code,
        String title,
        String description,
        Priority priority,
        WorkOrderStatus status,
        Instant slaDueAt,
        Instant createdAt,
        Instant updatedAt,
        Long customerId,
        String customerName,
        Long siteId,
        String siteName,
        Long assigneeId,
        String assigneeName
) {

 public static WorkOrderResponse from(WorkOrder x) {

  Customer customer = x.getCustomer();
  Site site = x.getSite();
  AppUser assignee = x.getAssignee();

  return new WorkOrderResponse(
          x.getId(),
          x.getCode(),
          x.getTitle(),
          x.getDescription(),
          x.getPriority(),
          x.getStatus(),
          x.getSlaDueAt(),
          x.getCreatedAt(),
          x.getUpdatedAt(),

          customer != null ? customer.getId() : null,
          customer != null ? customer.getName() : null,

          site != null ? site.getId() : null,
          site != null ? site.getName() : null,

          assignee != null ? assignee.getId() : null,
          assignee != null ? assignee.getFullName() : null
  );
 }
}