package com.hospital.sistemamedico.model.dto;

import com.hospital.sistemamedico.model.entity.Prescription;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DispensePreview {
    private Prescription prescription;
    private List<DispensePreviewLine> lines;
    private long daysSinceIssued;
    private String branchName;
}
