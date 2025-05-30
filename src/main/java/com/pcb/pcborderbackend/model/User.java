package com.pcb.pcborderbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String phone;
    private String email;
    private String password;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private Boolean deleted;
}
