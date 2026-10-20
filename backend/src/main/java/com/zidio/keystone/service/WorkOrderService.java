package com.zidio.keystone.service;

import com.zidio.keystone.domain.*;
import com.zidio.keystone.dto.*;
import com.zidio.keystone.repository.*;

import org.hibernate.Hibernate;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class WorkOrderService {

 final WorkOrderRepository wo;
 final CustomerRepository customers;
 final SiteRepository sites;
 final AppUserRepository users;
 final StatusHistoryRepository history;
 final PartRepository parts;
 final PartUsageRepository usage;
 final TimeLogRepository times;
 final NotificationRepository notifications;
 final AttachmentRepository attachments;

 public WorkOrderService(
         WorkOrderRepository wo,
         CustomerRepository customers,
         SiteRepository sites,
         AppUserRepository users,
         StatusHistoryRepository history,
         PartRepository parts,
         PartUsageRepository usage,
         TimeLogRepository times,
         NotificationRepository notifications,
         AttachmentRepository attachments
 ) {
  this.wo = wo;
  this.customers = customers;
  this.sites = sites;
  this.users = users;
  this.history = history;
  this.parts = parts;
  this.usage = usage;
  this.times = times;
  this.notifications = notifications;
  this.attachments = attachments;
 }

 @Transactional
 public WorkOrder create(WorkOrderRequest r, String email) {

  var actor = users.findByEmail(email).orElseThrow();

  // Only Customer, Dispatcher and Manager can create work orders
  if (actor.getRole() != Role.CUSTOMER
          && actor.getRole() != Role.DISPATCHER
          && actor.getRole() != Role.MANAGER) {

   throw new SecurityException(
           "Only customer, dispatcher or manager can create work orders"
   );
  }

  Customer c;

  // Customer can create a request only for their own organisation
  if (actor.getRole() == Role.CUSTOMER) {

   if (actor.getCustomer() == null
           || !actor.getCustomer().getId().equals(r.customerId())) {

    throw new SecurityException(
            "Customers may create requests only for their organisation"
    );
   }

   c = actor.getCustomer();

  } else {

   c = customers.findById(r.customerId())
           .orElseThrow(() ->
                   new NoSuchElementException("Customer not found"));
  }

  // Validate site
  var s = sites.findById(r.siteId())
          .orElseThrow(() ->
                  new NoSuchElementException("Site not found"));

  // Ensure the site belongs to the selected customer
  if (!s.getCustomer().getId().equals(c.getId())) {

   throw new IllegalArgumentException(
           "Site does not belong to customer"
   );
  }

  var x = new WorkOrder();

  x.setCode(
          "WO-" +
                  UUID.randomUUID()
                          .toString()
                          .substring(0, 8)
                          .toUpperCase()
  );

  x.setTitle(r.title());
  x.setDescription(r.description());
  x.setPriority(r.priority());
  x.setStatus(WorkOrderStatus.NEW);
  x.setCustomer(c);
  x.setSite(s);
  x.setCreatedAt(Instant.now());
  x.setUpdatedAt(Instant.now());

  x.setSlaDueAt(
          Instant.now()
                  .plusSeconds(
                          slaHours(r.priority()) * 3600L
                  )
  );

  WorkOrder saved = wo.save(x);

  /*
   * Initialize lazy relationships while the transaction
   * is still active. WorkOrderResponse.from() needs
   * customer name and site name after this method returns.
   */
  Hibernate.initialize(saved.getCustomer());
  Hibernate.initialize(saved.getSite());

  return saved;
 }

 int slaHours(Priority p) {
  return switch (p) {
   case CRITICAL -> 4;
   case HIGH -> 8;
   case MEDIUM -> 24;
   case LOW -> 72;
  };
 }

 public Page<WorkOrder> list(
         String email,
         int page,
         int size,
         String search,
         WorkOrderStatus status
 ) {

  var u = users.findByEmail(email).orElseThrow();

  Pageable p = PageRequest.of(
          Math.max(page, 0),
          Math.min(Math.max(size, 1), 100),
          Sort.by("createdAt").descending()
  );

  if (search != null && !search.isBlank()) {

   var all = scope(email);

   var filtered = all.stream()
           .filter(x ->
                   (x.getTitle()
                           + " "
                           + x.getCode()
                           + " "
                           + x.getDescription())
                           .toLowerCase()
                           .contains(search.toLowerCase())
           )
           .filter(x ->
                   status == null || x.getStatus() == status
           )
           .toList();

   int from = Math.min(
           (int) p.getOffset(),
           filtered.size()
   );

   int to = Math.min(
           from + p.getPageSize(),
           filtered.size()
   );

   return new PageImpl<>(
           filtered.subList(from, to),
           p,
           filtered.size()
   );
  }

  if (status != null) {

   var all = scope(email)
           .stream()
           .filter(x -> x.getStatus() == status)
           .toList();

   int from = Math.min(
           (int) p.getOffset(),
           all.size()
   );

   int to = Math.min(
           from + p.getPageSize(),
           all.size()
   );

   return new PageImpl<>(
           all.subList(from, to),
           p,
           all.size()
   );
  }

  if (u.getRole() == Role.TECHNICIAN) {
   return wo.findByAssigneeIdOrderByCreatedAtDesc(
           u.getId(),
           p
   );
  }

  if (u.getRole() == Role.CUSTOMER) {
   return wo.findByCustomerIdOrderByCreatedAtDesc(
           u.getCustomer().getId(),
           p
   );
  }

  return wo.findAllByOrderByCreatedAtDesc(p);
 }

 private List<WorkOrder> scope(String email) {

  var u = users.findByEmail(email).orElseThrow();

  if (u.getRole() == Role.TECHNICIAN) {
   return wo.findByAssigneeIdOrderByCreatedAtDesc(
           u.getId(),
           Pageable.unpaged()
   ).getContent();
  }

  if (u.getRole() == Role.CUSTOMER) {
   return wo.findByCustomerIdOrderByCreatedAtDesc(
           u.getCustomer().getId(),
           Pageable.unpaged()
   ).getContent();
  }

  return wo.findAll();
 }

 @Transactional
 public WorkOrder assign(
         Long id,
         AssignRequest r,
         String email
 ) {

  var x = get(id);

  ensureOpen(x);

  var t = users.findById(r.technicianId())
          .orElseThrow();

  if (t.getRole() != Role.TECHNICIAN) {
   throw new IllegalArgumentException(
           "Assignee must be technician"
   );
  }

  var actor = users.findByEmail(email)
          .orElseThrow();

  if (x.getStatus() != WorkOrderStatus.NEW
          && x.getStatus() != WorkOrderStatus.ASSIGNED) {

   throw new IllegalStateException(
           "Only NEW or ASSIGNED jobs can be assigned"
   );
  }

  if (x.getStatus() == WorkOrderStatus.NEW) {
   transition(
           x,
           WorkOrderStatus.ASSIGNED,
           null,
           actor
   );
  }

  x.setAssignee(t);
  x.setUpdatedAt(Instant.now());

  notifications.save(
          notification(
                  t,
                  "Work order "
                          + x.getCode()
                          + " was assigned to you."
          )
  );

  WorkOrder saved = wo.save(x);

  Hibernate.initialize(saved.getCustomer());
  Hibernate.initialize(saved.getSite());
  Hibernate.initialize(saved.getAssignee());

  return saved;
 }

 @Transactional
 public WorkOrder changeStatus(
         Long id,
         StatusRequest r,
         String email
 ) {

  var x = get(id);

  var actor = users.findByEmail(email)
          .orElseThrow();

  if (actor.getRole() == Role.TECHNICIAN
          && (x.getAssignee() == null
          || !x.getAssignee().getId().equals(actor.getId()))) {

   throw new SecurityException(
           "Not assigned to this technician"
   );
  }

  if (r.status() == WorkOrderStatus.CLOSED
          && actor.getRole() != Role.MANAGER) {

   throw new SecurityException(
           "Only manager can close jobs"
   );
  }

  if (r.status() == WorkOrderStatus.CANCELLED
          && actor.getRole() != Role.MANAGER
          && actor.getRole() != Role.DISPATCHER) {

   throw new SecurityException(
           "Only manager or dispatcher can cancel jobs"
   );
  }

  if (!allowed(x.getStatus(), r.status())) {

   throw new IllegalStateException(
           "Illegal transition: "
                   + x.getStatus()
                   + " -> "
                   + r.status()
   );
  }

  transition(
          x,
          r.status(),
          r.note(),
          actor
  );

  WorkOrder saved = wo.save(x);

  Hibernate.initialize(saved.getCustomer());
  Hibernate.initialize(saved.getSite());
  Hibernate.initialize(saved.getAssignee());

  return saved;
 }

 boolean allowed(
         WorkOrderStatus a,
         WorkOrderStatus b
 ) {

  return switch (a) {
   case NEW ->
           b == WorkOrderStatus.ASSIGNED
                   || b == WorkOrderStatus.CANCELLED;

   case ASSIGNED ->
           b == WorkOrderStatus.IN_PROGRESS
                   || b == WorkOrderStatus.CANCELLED;

   case IN_PROGRESS ->
           b == WorkOrderStatus.ON_HOLD
                   || b == WorkOrderStatus.COMPLETED
                   || b == WorkOrderStatus.CANCELLED;

   case ON_HOLD ->
           b == WorkOrderStatus.IN_PROGRESS
                   || b == WorkOrderStatus.CANCELLED;

   case COMPLETED ->
           b == WorkOrderStatus.CLOSED;

   default ->
           false;
  };
 }

 void transition(
         WorkOrder x,
         WorkOrderStatus to,
         String note,
         AppUser actor
 ) {

  var h = new StatusHistory();

  h.setWorkOrder(x);
  h.setFromStatus(x.getStatus());
  h.setToStatus(to);
  h.setChangedBy(actor);
  h.setChangedAt(Instant.now());
  h.setNote(note);

  history.save(h);

  x.setStatus(to);
  x.setUpdatedAt(Instant.now());
 }

 @Transactional
 public void addPart(
         Long id,
         PartRequest r,
         String email
 ) {

  var x = getScoped(id, email);

  var p = parts.findById(r.partId())
          .orElseThrow();

  if (p.getStock() < r.quantity()) {
   throw new IllegalArgumentException(
           "Insufficient stock"
   );
  }

  p.setStock(
          p.getStock() - r.quantity()
  );

  var u = new PartUsage();

  u.setWorkOrder(x);
  u.setPart(p);
  u.setQuantity(r.quantity());

  usage.save(u);
  parts.save(p);
 }

 @Transactional
 public void addTime(
         Long id,
         TimeRequest r,
         String email
 ) {

  var t = users.findByEmail(email)
          .orElseThrow();

  var x = getScoped(id, email);

  var l = new TimeLog();

  l.setWorkOrder(x);
  l.setTechnician(t);
  l.setMinutes(r.minutes());
  l.setNote(r.note());
  l.setLoggedAt(Instant.now());

  times.save(l);
 }

 public WorkOrder get(Long id) {

  return wo.findById(id)
          .orElseThrow(() ->
                  new NoSuchElementException(
                          "Work order not found"
                  ));
 }

 public WorkOrder getScoped(
         Long id,
         String email
 ) {

  var x = get(id);

  var u = users.findByEmail(email)
          .orElseThrow();

  if (u.getRole() == Role.CUSTOMER
          && (u.getCustomer() == null
          || !u.getCustomer().getId()
          .equals(x.getCustomer().getId()))) {

   throw new SecurityException(
           "Access denied"
   );
  }

  if (u.getRole() == Role.TECHNICIAN
          && (x.getAssignee() == null
          || !x.getAssignee().getId()
          .equals(u.getId()))) {

   throw new SecurityException(
           "Access denied"
   );
  }

  return x;
 }

 public List<StatusHistory> history(
         Long id,
         String email
 ) {

  getScoped(id, email);

  return history.findByWorkOrderIdOrderByChangedAtAsc(id);
 }

 public List<Part> parts() {
  return parts.findAll();
 }

 public List<AppUser> technicians() {
  return users.findByRole(Role.TECHNICIAN);
 }

 public List<Notification> notifications(
         String email
 ) {

  return notifications
          .findTop50ByUserIdOrderByCreatedAtDesc(
                  users.findByEmail(email)
                          .orElseThrow()
                          .getId()
          );
 }

 @Transactional
 public void addAttachment(
         Long id,
         String original,
         String stored,
         String type,
         long size,
         String email
 ) {

  var x = getScoped(id, email);

  var a = new Attachment();

  a.setWorkOrder(x);
  a.setUploadedBy(
          users.findByEmail(email)
                  .orElseThrow()
  );
  a.setOriginalName(original);
  a.setStoredName(stored);
  a.setContentType(type);
  a.setSizeBytes(size);

  attachments.save(a);
 }

 public List<Attachment> attachments(
         Long id,
         String email
 ) {

  getScoped(id, email);

  return attachments
          .findByWorkOrderIdOrderByUploadedAtDesc(id);
 }

 private Notification notification(
         AppUser u,
         String msg
 ) {

  var n = new Notification();

  n.setUser(u);
  n.setMessage(msg);
  n.setCreatedAt(Instant.now());

  return n;
 }

 private void ensureOpen(WorkOrder x) {

  if (x.getStatus() == WorkOrderStatus.CLOSED
          || x.getStatus() == WorkOrderStatus.CANCELLED) {

   throw new IllegalStateException(
           "Terminal work order cannot be changed"
   );
  }
 }
}