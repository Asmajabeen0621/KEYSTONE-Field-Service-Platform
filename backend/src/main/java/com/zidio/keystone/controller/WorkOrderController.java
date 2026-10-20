package com.zidio.keystone.controller;

import com.zidio.keystone.domain.*;
import com.zidio.keystone.dto.*;
import com.zidio.keystone.service.WorkOrderService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

 private final WorkOrderService workOrderService;
 private final Path uploadDir;

 public WorkOrderController(
         WorkOrderService workOrderService,
         @org.springframework.beans.factory.annotation.Value(
                 "${app.uploads-dir:./uploads}"
         )
         String dir
 ) {
  this.workOrderService = workOrderService;
  this.uploadDir = Paths.get(dir);
 }

 // =========================================================
 // LIST WORK ORDERS
 // =========================================================

 @GetMapping
 public Map<String, Object> list(
         Authentication authentication,
         @RequestParam(defaultValue = "0") int page,
         @RequestParam(defaultValue = "10") int size,
         @RequestParam(required = false) String search,
         @RequestParam(required = false) WorkOrderStatus status
 ) {

  Page<WorkOrder> workOrders =
          workOrderService.list(
                  authentication.getName(),
                  page,
                  size,
                  search,
                  status
          );

  return Map.of(
          "content",
          workOrders.getContent()
                  .stream()
                  .map(WorkOrderResponse::from)
                  .toList(),
          "page", workOrders.getNumber(),
          "size", workOrders.getSize(),
          "totalElements", workOrders.getTotalElements(),
          "totalPages", workOrders.getTotalPages()
  );
 }

 // =========================================================
 // GET SINGLE WORK ORDER
 // =========================================================

 @GetMapping("/{id}")
 public WorkOrderResponse get(
         @PathVariable Long id,
         Authentication authentication
 ) {

  return WorkOrderResponse.from(
          workOrderService.getScoped(
                  id,
                  authentication.getName()
          )
  );
 }

 // =========================================================
 // GET WORK ORDER HISTORY
 // =========================================================

 @GetMapping("/{id}/history")
 public List<StatusHistory> history(
         @PathVariable Long id,
         Authentication authentication
 ) {

  return workOrderService.history(
          id,
          authentication.getName()
  );
 }

 // =========================================================
 // CREATE WORK ORDER
 // CUSTOMER / DISPATCHER / MANAGER
 // =========================================================

 @PostMapping
 @org.springframework.security.access.prepost.PreAuthorize(
         "hasAnyRole('CUSTOMER','DISPATCHER','MANAGER')"
 )
 public WorkOrderResponse create(
         @Valid @RequestBody WorkOrderRequest request,
         Authentication authentication
 ) {

  return WorkOrderResponse.from(
          workOrderService.create(
                  request,
                  authentication.getName()
          )
  );
 }

 // =========================================================
 // ASSIGN WORK ORDER
 // DISPATCHER / MANAGER
 // =========================================================

 @PostMapping("/{id}/assign")
 @org.springframework.security.access.prepost.PreAuthorize(
         "hasAnyRole('DISPATCHER','MANAGER')"
 )
 public WorkOrderResponse assign(
         @PathVariable Long id,
         @Valid @RequestBody AssignRequest request,
         Authentication authentication
 ) {

  return WorkOrderResponse.from(
          workOrderService.assign(
                  id,
                  request,
                  authentication.getName()
          )
  );
 }

 // =========================================================
 // CHANGE STATUS
 // TECHNICIAN / DISPATCHER / MANAGER
 // =========================================================

 @PostMapping("/{id}/status")
 @org.springframework.security.access.prepost.PreAuthorize(
         "hasAnyRole('TECHNICIAN','DISPATCHER','MANAGER')"
 )
 public WorkOrderResponse status(
         @PathVariable Long id,
         @Valid @RequestBody StatusRequest request,
         Authentication authentication
 ) {

  return WorkOrderResponse.from(
          workOrderService.changeStatus(
                  id,
                  request,
                  authentication.getName()
          )
  );
 }

 // =========================================================
 // ADD PART
 // TECHNICIAN / MANAGER
 // =========================================================

 @PostMapping("/{id}/parts")
 @org.springframework.security.access.prepost.PreAuthorize(
         "hasAnyRole('TECHNICIAN','MANAGER')"
 )
 public Map<String, String> part(
         @PathVariable Long id,
         @Valid @RequestBody PartRequest request,
         Authentication authentication
 ) {

  workOrderService.addPart(
          id,
          request,
          authentication.getName()
  );

  return Map.of(
          "message",
          "Part logged successfully"
  );
 }

 // =========================================================
 // LOG TIME
 // TECHNICIAN
 // =========================================================

 @PostMapping("/{id}/time")
 @org.springframework.security.access.prepost.PreAuthorize(
         "hasRole('TECHNICIAN')"
 )
 public Map<String, String> time(
         @PathVariable Long id,
         @Valid @RequestBody TimeRequest request,
         Authentication authentication
 ) {

  workOrderService.addTime(
          id,
          request,
          authentication.getName()
  );

  return Map.of(
          "message",
          "Time logged successfully"
  );
 }

 // =========================================================
 // UPLOAD ATTACHMENT / PHOTO
 // TECHNICIAN / MANAGER
 // =========================================================

 @PostMapping(
         value = "/{id}/attachments",
         consumes = MediaType.MULTIPART_FORM_DATA_VALUE
 )
 @org.springframework.security.access.prepost.PreAuthorize(
         "hasAnyRole('TECHNICIAN','MANAGER')"
 )
 public Map<String, String> upload(
         @PathVariable Long id,
         @RequestPart("file") MultipartFile file,
         Authentication authentication
 ) throws IOException {

  if (file.isEmpty()) {
   throw new IllegalArgumentException(
           "File is empty"
   );
  }

  if (file.getSize() > 10_000_000) {
   throw new IllegalArgumentException(
           "Maximum file size is 10 MB"
   );
  }

  Files.createDirectories(uploadDir);

  String originalFilename =
          Objects.requireNonNull(
                  file.getOriginalFilename()
          );

  String storedFilename =
          UUID.randomUUID()
                  + "-"
                  + Paths.get(originalFilename)
                  .getFileName();

  Files.copy(
          file.getInputStream(),
          uploadDir.resolve(storedFilename),
          StandardCopyOption.REPLACE_EXISTING
  );

  workOrderService.addAttachment(
          id,
          originalFilename,
          storedFilename,
          Optional.ofNullable(
                  file.getContentType()
          ).orElse(
                  "application/octet-stream"
          ),
          file.getSize(),
          authentication.getName()
  );

  return Map.of(
          "message",
          "Photo uploaded",
          "file",
          storedFilename
  );
 }

 // =========================================================
 // GET ATTACHMENTS
 // =========================================================

 @GetMapping("/{id}/attachments")
 public List<Attachment> attachments(
         @PathVariable Long id,
         Authentication authentication
 ) {

  return workOrderService.attachments(
          id,
          authentication.getName()
  );
 }
}