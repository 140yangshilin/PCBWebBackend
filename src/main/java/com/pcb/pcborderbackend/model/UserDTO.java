package com.pcb.pcborderbackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String phone;
    private String email;
    private LocalDateTime createdAt;
    private Boolean deleted;


}
