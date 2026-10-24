package com.hospital.sistemamedico.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DispenseForm {
    private List<DispenseLineForm> lines = new ArrayList<>();
}
