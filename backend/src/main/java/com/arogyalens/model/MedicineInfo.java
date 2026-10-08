package com.arogyalens.model;

import java.util.List;

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
        List<TrustedSource> sources
) {
}
