package com.arogyalens.model;

public record PrescriptionItem(
        String medicineName,
        String strength,
        String frequency,
        String timing,
        String foodRelation,
        boolean morning,
        boolean afternoon,
        boolean night,
        boolean confident,
        String note
) {
}
