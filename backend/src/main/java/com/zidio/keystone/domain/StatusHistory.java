package com.zidio.keystone.domain;
import jakarta.persistence.*; import lombok.*; import java.time.*;
@Entity @Table(name="work_order_status_history") @Getter @Setter @NoArgsConstructor public class StatusHistory { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @ManyToOne(fetch=FetchType.EAGER,optional=false) WorkOrder workOrder; @Enumerated(EnumType.STRING) WorkOrderStatus fromStatus; @Enumerated(EnumType.STRING) WorkOrderStatus toStatus; @ManyToOne(fetch=FetchType.EAGER,optional=false) AppUser changedBy; @Column(nullable=false) Instant changedAt; String note; }
