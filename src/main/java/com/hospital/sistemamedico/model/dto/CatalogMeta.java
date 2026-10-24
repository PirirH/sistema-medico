package com.hospital.sistemamedico.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

/** Describe un catálogo: título, cómo se llama la columna de detalle y qué campos usa su formulario. */
@Data
@AllArgsConstructor
public class CatalogMeta {
    private String key;
    private String title;
    private String detailLabel;
    private Set<String> fields;

    public boolean has(String field) {
        return fields.contains(field);
    }
}
