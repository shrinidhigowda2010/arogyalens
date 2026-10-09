package com.arogyalens.model;

import java.util.List;

/** General information about a medicine read from its package. */
public record MedicineInfo(
        String name,
        String strength,
        String dosageForm,
        String manufacturer,
        String generalUse,
        List<String> commonSideEffects,
        List<String> precautions,
        List<String> warnings,
        double confidence,
        List<TrustedSource> sources) {}
