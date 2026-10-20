package com.zidio.keystone.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "time_log")
@Getter
@Setter
@NoArgsConstructor
public class TimeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    AppUser technician;

    @Column(nullable = false)
    int minutes;

    String note;

    @Column(nullable = false)
    Instant loggedAt;
}