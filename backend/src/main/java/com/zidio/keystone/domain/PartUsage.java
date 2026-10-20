package com.zidio.keystone.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "part_usage")
@Getter
@Setter
@NoArgsConstructor
public class PartUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    Part part;

    @Column(nullable = false)
    int quantity;
}