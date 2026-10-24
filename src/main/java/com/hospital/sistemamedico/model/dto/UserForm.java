package com.hospital.sistemamedico.model.dto;

import lombok.Data;

@Data
public class UserForm {
    private String fullName;
    private String email;
    private String username;
    private String password;
    private String dpi;
    private String phone;
    private Integer roleId;
    private String nit;
    private String insuranceNumber;
    private Integer branchId;
    private Integer specialtyId;
    private boolean active = true;
}
