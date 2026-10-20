package com.zidio.keystone.controller;

import com.zidio.keystone.domain.*;
import com.zidio.keystone.dto.*;
import com.zidio.keystone.repository.*;
import com.zidio.keystone.service.WorkOrderService;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReferenceController {

 private final CustomerRepository customerRepository;
 private final SiteRepository siteRepository;
 private final PartRepository partRepository;
 private final AppUserRepository userRepository;
 private final WorkOrderService workOrderService;

 public ReferenceController(
         CustomerRepository customerRepository,
         SiteRepository siteRepository,
         PartRepository partRepository,
         AppUserRepository userRepository,
         WorkOrderService workOrderService
 ) {
  this.customerRepository = customerRepository;
  this.siteRepository = siteRepository;
  this.partRepository = partRepository;
  this.userRepository = userRepository;
  this.workOrderService = workOrderService;
 }

 // =========================================================
 // CUSTOMERS
 // =========================================================

 @GetMapping("/customers")
 public List<Customer> customers(
         org.springframework.security.core.Authentication authentication
 ) {

  AppUser user = userRepository
          .findByEmail(authentication.getName())
          .orElseThrow();

  // CUSTOMER can see only their own customer record
  if (user.getRole() == Role.CUSTOMER) {

   if (user.getCustomer() == null) {
    return List.of();
   }

   Long customerId = user.getCustomer().getId();

   return customerRepository
           .findById(customerId)
           .map(List::of)
           .orElseGet(List::of);
  }

  // DISPATCHER / MANAGER can see all customers
  return customerRepository.findAll();
 }

 @PostMapping("/customers")
 @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
 public Customer customer(
         @Valid @RequestBody CustomerRequest request
 ) {

  Customer customer = new Customer();

  customer.setName(request.name());
  customer.setEmail(request.email());
  customer.setPhone(request.phone());

  return customerRepository.save(customer);
 }

 @PutMapping("/customers/{id}")
 @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
 public Customer updateCustomer(
         @PathVariable Long id,
         @Valid @RequestBody CustomerRequest request
 ) {

  Customer customer = customerRepository
          .findById(id)
          .orElseThrow();

  customer.setName(request.name());
  customer.setEmail(request.email());
  customer.setPhone(request.phone());

  return customerRepository.save(customer);
 }

 // =========================================================
 // SITES
 // =========================================================

 @GetMapping("/customers/{id}/sites")
 public List<Site> sites(
         @PathVariable Long id,
         org.springframework.security.core.Authentication authentication
 ) {

  AppUser user = userRepository
          .findByEmail(authentication.getName())
          .orElseThrow();

  // CUSTOMER can access only their own customer's sites
  if (user.getRole() == Role.CUSTOMER) {

   if (user.getCustomer() == null ||
           !user.getCustomer().getId().equals(id)) {

    throw new SecurityException("Access denied");
   }
  }

  return siteRepository.findByCustomerId(id);
 }

 @PostMapping("/customers/{id}/sites")
 @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
 public Site site(
         @PathVariable Long id,
         @Valid @RequestBody SiteRequest request
 ) {

  Customer customer = customerRepository
          .findById(id)
          .orElseThrow();

  Site site = new Site();

  site.setName(request.name());
  site.setAddress(request.address());
  site.setCustomer(customer);

  return siteRepository.save(site);
 }

 @PutMapping("/sites/{id}")
 @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
 public Site updateSite(
         @PathVariable Long id,
         @Valid @RequestBody SiteRequest request
 ) {

  Site site = siteRepository
          .findById(id)
          .orElseThrow();

  site.setName(request.name());
  site.setAddress(request.address());

  return siteRepository.save(site);
 }

 // =========================================================
 // TECHNICIANS
 // =========================================================

 @GetMapping("/technicians")
 @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
 public List<AppUser> technicians() {

  return userRepository.findByRole(Role.TECHNICIAN);
 }

 // =========================================================
 // PARTS
 // =========================================================

 @GetMapping("/parts")
 public List<Part> parts() {

  return partRepository.findAll();
 }

 @PostMapping("/parts")
 @PreAuthorize("hasRole('MANAGER')")
 public Part addPart(
         @RequestBody Part part
 ) {

  return partRepository.save(part);
 }

 // =========================================================
 // NOTIFICATIONS
 // =========================================================

 @GetMapping("/notifications")
 public List<Notification> notifications(
         org.springframework.security.core.Authentication authentication
 ) {

  return workOrderService.notifications(authentication.getName());
 }
}