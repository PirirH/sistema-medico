package com.hospital.sistemamedico.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PrescriptionForm {
    private List<PrescriptionItemForm> items = new ArrayList<>();
    private String notes;
}
