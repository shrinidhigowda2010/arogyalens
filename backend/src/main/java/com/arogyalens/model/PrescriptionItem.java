package com.arogyalens.model;

/** One prescribed medicine with its dose timing. */
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
        String note) {}
