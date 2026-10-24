package com.hospital.sistemamedico.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** Resultado paginado en memoria (usuarios, catálogos). */
@Data
@AllArgsConstructor
public class PageResult<T> {
    private List<T> content;
    private int page;       // base 0
    private int size;
    private int total;
    private int totalPages;

    public boolean isFirst() { return page <= 0; }
    public boolean isLast() { return page >= totalPages - 1; }

    public static <T> PageResult<T> of(List<T> all, int page, int size) {
        int total = all.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) size));
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = Math.min(safePage * size, total);
        int to = Math.min(from + size, total);
        return new PageResult<>(all.subList(from, to), safePage, size, total, totalPages);
    }
}
