package com.arogyalens.dto;

import com.arogyalens.model.DischargeSummary;
import com.arogyalens.model.DocumentType;
import com.arogyalens.model.MedicalParameter;
import com.arogyalens.model.MedicineInfo;
import com.arogyalens.model.PiiFinding;
import com.arogyalens.model.PrescriptionItem;
import com.arogyalens.model.TrustedSource;

import java.util.List;
import java.util.Map;

public record AnalysisResponse(
        String sessionId,
        DocumentType documentType,
        String documentLabel,
        boolean demo,
        PrivacyShieldDto privacyShield,
        List<ProcessingStepDto> processingSteps,
        int pages,
        int parametersDetected,
        List<MedicalParameter> parameters,
        DashboardSummaryDto dashboard,
        List<String> doctorQuestions,
        List<TrustedSource> sources,
        List<String> safetyNotes,
        MedicineInfo medicine,
        List<PrescriptionItem> prescriptionItems,
        DischargeSummary dischargeSummary,
        String familySummary,
        Map<String, String> translations,
        String originalPreviewNote,
        boolean aiUsed,
        String disclaimer
) {
    public record PrivacyShieldDto(
            List<PiiFinding> findings,
            String message,
            boolean redacted
    ) {}

    public record ProcessingStepDto(
            String id,
            String label,
            String status
    ) {}

    public record DashboardSummaryDto(
            int total,
            int withinRange,
            int needsDiscussion,
            int importantAttention
    ) {}
}
