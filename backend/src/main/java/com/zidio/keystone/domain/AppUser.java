package com.zidio.keystone.domain;
import jakarta.persistence.*; import lombok.*; import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity @Table(name="users") @Getter @Setter @NoArgsConstructor
public class AppUser { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @Column(nullable=false,unique=true) String email; @JsonIgnore @Column(nullable=false) String passwordHash; @Column(nullable=false) String fullName; @Enumerated(EnumType.STRING) @Column(nullable=false) Role role; @JsonIgnore @ManyToOne(fetch=FetchType.LAZY) Customer customer; }
