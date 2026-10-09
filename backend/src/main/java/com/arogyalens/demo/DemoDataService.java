package com.arogyalens.demo;

import com.arogyalens.dto.AnalysisResponse;
import com.arogyalens.dto.AnalysisResponse.DashboardSummaryDto;
import com.arogyalens.dto.AnalysisResponse.PrivacyShieldDto;
import com.arogyalens.dto.AnalysisResponse.ProcessingStepDto;
import com.arogyalens.model.DischargeSummary;
import com.arogyalens.model.DocumentType;
import com.arogyalens.model.MedicalParameter;
import com.arogyalens.model.MedicineInfo;
import com.arogyalens.model.ParameterStatus;
import com.arogyalens.model.PiiFinding;
import com.arogyalens.model.PrescriptionItem;
import com.arogyalens.model.TrustedSource;
import com.arogyalens.service.OfflineLanguagePack;
import com.arogyalens.source.SourceService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DemoDataService {

    private final SourceService sourceService;
    private volatile AnalysisResponse labTemplate;
    private volatile AnalysisResponse medicineTemplate;
    private volatile AnalysisResponse prescriptionTemplate;
    private volatile AnalysisResponse dischargeTemplate;

    public DemoDataService(SourceService sourceService) {
        this.sourceService = sourceService;
    }

    public Map<String, String> hba1cTranslations() {
        return new OfflineLanguagePack().all("hba1c");
    }

    public String sampleLabText() {
        return """
                DEMO DATA — NOT A REAL PATIENT
                City Care Diagnostics
                Patient Name: Anita Sharma
                Patient ID: UHID-284751
                Phone: 9876543210
                Email: anita.sharma@example.com
                DOB: 12/05/1988
                Address: 42 MG Road, Bengaluru

                BLOOD TEST REPORT

                Hemoglobin          10.2 g/dL     12.0-15.0
                WBC                 7.8 x10^3/uL  4.0-11.0
                Platelets           245 x10^3/uL  150-450
                Fasting Glucose     118 mg/dL     70-100
                HbA1c               7.8 %         <5.7
                TSH                 2.1 uIU/mL    0.4-4.0
                Total Cholesterol   210 mg/dL     <200
                HDL                 42 mg/dL      >40
                LDL                 138 mg/dL     <100
                Triglycerides       160 mg/dL     <150
                Creatinine          0.9 mg/dL     0.6-1.2
                """;
    }

    public AnalysisResponse labReport(String sessionId) {
        return withSession(cachedLab(), sessionId);
    }

    private AnalysisResponse cachedLab() {
        AnalysisResponse cached = labTemplate;
        if (cached == null) {
            synchronized (this) {
                cached = labTemplate;
                if (cached == null) {
                    labTemplate = cached = buildLabReport("template");
                }
            }
        }
        return cached;
    }

    private AnalysisResponse buildLabReport(String sessionId) {
        List<MedicalParameter> parameters =
                List.of(
                        param(
                                "Hemoglobin",
                                "10.2",
                                "g/dL",
                                "12.0-15.0",
                                ParameterStatus.OUTSIDE_RANGE,
                                "Hemoglobin carries oxygen in the blood. This value is below the reference range shown on the report.",
                                "Hemoglobin is like the delivery truck that carries oxygen. Your report shows a lower number than the usual range printed here.",
                                0.96),
                        param(
                                "WBC",
                                "7.8",
                                "x10^3/uL",
                                "4.0-11.0",
                                ParameterStatus.WITHIN_RANGE,
                                "White blood cells help the body respond to infection. This value is within the provided reference range.",
                                "These are cells that help protect the body. This number looks inside the usual range on the report.",
                                0.95),
                        param(
                                "Platelets",
                                "245",
                                "x10^3/uL",
                                "150-450",
                                ParameterStatus.WITHIN_RANGE,
                                "Platelets help blood clot. This value is within the provided reference range.",
                                "Platelets help stop bleeding. This number is inside the usual range shown.",
                                0.94),
                        param(
                                "Fasting Glucose",
                                "118",
                                "mg/dL",
                                "70-100",
                                ParameterStatus.REQUIRES_DISCUSSION,
                                "This fasting glucose value is outside the reference range shown. Discuss it with your healthcare professional in the context of your overall health.",
                                "This checks sugar in the blood after fasting. The number is higher than the range printed on the report, so it is worth asking your doctor about.",
                                0.93),
                        param(
                                "HbA1c",
                                "7.8",
                                "%",
                                "<5.7",
                                ParameterStatus.IMPORTANT_ATTENTION,
                                "HbA1c estimates average blood sugar over the previous few months. This result is above many commonly used targets, although individual targets can differ. Discuss it with your healthcare professional.",
                                "HbA1c is like a report card for blood sugar over a few months. This number is higher than many common targets, so it is important to talk with your doctor.",
                                0.97),
                        param(
                                "TSH",
                                "2.1",
                                "uIU/mL",
                                "0.4-4.0",
                                ParameterStatus.WITHIN_RANGE,
                                "TSH is a thyroid-related hormone test. This value is within the provided reference range.",
                                "This test relates to the thyroid gland. The number is inside the usual range shown.",
                                0.95),
                        param(
                                "Total Cholesterol",
                                "210",
                                "mg/dL",
                                "<200",
                                ParameterStatus.REQUIRES_DISCUSSION,
                                "Total cholesterol is outside the target shown on the report. A healthcare professional can interpret this with other lipid values and your history.",
                                "Cholesterol is a fat-like substance in blood. This total number is a bit above the target printed here.",
                                0.92),
                        param(
                                "HDL",
                                "42",
                                "mg/dL",
                                ">40",
                                ParameterStatus.WITHIN_RANGE,
                                "HDL is often called 'good' cholesterol. This value meets the target shown.",
                                "HDL is one kind of cholesterol that is often considered helpful. This number meets the target on the report.",
                                0.91),
                        param(
                                "LDL",
                                "138",
                                "mg/dL",
                                "<100",
                                ParameterStatus.REQUIRES_DISCUSSION,
                                "LDL is outside the target shown. Discuss lipid results together with your healthcare professional.",
                                "LDL is another cholesterol number. It is higher than the target printed here, so ask your doctor what it means for you.",
                                0.93),
                        param(
                                "Triglycerides",
                                "160",
                                "mg/dL",
                                "<150",
                                ParameterStatus.OUTSIDE_RANGE,
                                "Triglycerides are outside the target shown on the report.",
                                "Triglycerides are a type of fat in the blood. This number is a little above the target shown.",
                                0.90),
                        param(
                                "Creatinine",
                                "0.9",
                                "mg/dL",
                                "0.6-1.2",
                                ParameterStatus.WITHIN_RANGE,
                                "Creatinine is commonly used when reviewing kidney-related lab results. This value is within the provided reference range.",
                                "This number is often checked when looking at kidney-related tests. It is inside the usual range shown.",
                                0.94));

        DashboardSummaryDto dashboard = summarize(parameters);
        List<TrustedSource> sources = sourceService.forTopic("hba1c");

        return new AnalysisResponse(
                sessionId == null ? UUID.randomUUID().toString() : sessionId,
                DocumentType.LAB_REPORT,
                "Blood Test Report",
                true,
                privacyShield(),
                processingSteps(),
                2,
                parameters.size(),
                parameters,
                dashboard,
                List.of(
                        "What could explain the HbA1c and fasting glucose results on this report?",
                        "Do these results need to be repeated or confirmed with another test?",
                        "Do I need any additional tests based on these values?",
                        "Could any of my current medicines or recent illness affect these results?",
                        "When should I follow up, and what targets are appropriate for me?"),
                sources,
                List.of(
                        "⚠️ Important: This information may require discussion with a qualified healthcare professional.",
                        "ArogyaLens cannot diagnose your condition or determine treatment from this document alone.",
                        "DEMO DATA — NOT A REAL PATIENT"),
                null,
                null,
                null,
                familySummaryLab(),
                hba1cTranslations(),
                "Original sample report retained for verification. DEMO DATA — NOT A REAL PATIENT",
                false,
                "We don't replace the doctor. We make healthcare easier to understand.");
    }

    public AnalysisResponse medicine(String sessionId) {
        return withSession(cachedMedicine(), sessionId);
    }

    private AnalysisResponse cachedMedicine() {
        AnalysisResponse cached = medicineTemplate;
        if (cached == null) {
            synchronized (this) {
                cached = medicineTemplate;
                if (cached == null) {
                    medicineTemplate = cached = buildMedicine("template");
                }
            }
        }
        return cached;
    }

    private AnalysisResponse buildMedicine(String sessionId) {
        MedicineInfo medicine =
                new MedicineInfo(
                        "Metformin",
                        "500 mg",
                        "Tablet",
                        "Demo Pharma Ltd",
                        "Metformin is commonly used as part of blood sugar management plans prescribed by clinicians. This is general information, not personal advice.",
                        List.of("Stomach upset", "Nausea", "Diarrhea (commonly reported)"),
                        List.of(
                                "Follow the prescription provided by your healthcare professional.",
                                "Ask a pharmacist or doctor before combining with other medicines."),
                        List.of(
                                "Do not start, stop, or change this medicine based on this scan alone."),
                        0.94,
                        sourceService.forTopic("metformin"));

        return new AnalysisResponse(
                sessionId == null ? UUID.randomUUID().toString() : sessionId,
                DocumentType.MEDICINE,
                "Medicine Package",
                true,
                privacyShieldMinimal(),
                processingSteps(),
                1,
                0,
                List.of(),
                new DashboardSummaryDto(0, 0, 0, 0),
                List.of(
                        "What is this medicine intended for in my care plan?",
                        "How and when should I take it according to my prescription?",
                        "Are there side effects I should watch for?",
                        "Does it interact with my other medicines?",
                        "When should I follow up about this medicine?"),
                medicine.sources(),
                List.of(
                        "Follow the prescription provided by your healthcare professional.",
                        "DEMO DATA — NOT A REAL PATIENT"),
                medicine,
                null,
                null,
                "FAMILY SUMMARY\n\nThe scanned medicine appears to be Metformin 500 mg tablet.\nThis is general information only and does not tell anyone how to take medicine.\nPlease confirm all instructions with a doctor or pharmacist.\n\nDEMO DATA — NOT A REAL PATIENT",
                Map.of(),
                "Original medicine image retained for verification.",
                false,
                "We don't replace the doctor. We make healthcare easier to understand.");
    }

    public AnalysisResponse prescription(String sessionId) {
        return withSession(cachedPrescription(), sessionId);
    }

    private AnalysisResponse cachedPrescription() {
        AnalysisResponse cached = prescriptionTemplate;
        if (cached == null) {
            synchronized (this) {
                cached = prescriptionTemplate;
                if (cached == null) {
                    prescriptionTemplate = cached = buildPrescription("template");
                }
            }
        }
        return cached;
    }

    private AnalysisResponse buildPrescription(String sessionId) {
        List<PrescriptionItem> items =
                List.of(
                        new PrescriptionItem(
                                "Metformin",
                                "500 mg",
                                "1-0-1",
                                "Morning and Night",
                                "After food",
                                true,
                                false,
                                true,
                                true,
                                null),
                        new PrescriptionItem(
                                "Atorvastatin",
                                "10 mg",
                                "0-0-1",
                                "Night",
                                "After food",
                                false,
                                false,
                                true,
                                true,
                                null),
                        new PrescriptionItem(
                                "Unclear medicine line",
                                null,
                                null,
                                null,
                                null,
                                false,
                                false,
                                false,
                                false,
                                "⚠️ We could not confidently read this instruction. Please confirm it with your doctor or pharmacist."));

        return new AnalysisResponse(
                sessionId == null ? UUID.randomUUID().toString() : sessionId,
                DocumentType.PRESCRIPTION,
                "Prescription",
                true,
                privacyShield(),
                processingSteps(),
                1,
                0,
                List.of(),
                new DashboardSummaryDto(0, 0, 0, 0),
                List.of(
                        "Can you confirm the medicines and timing written on this prescription?",
                        "Should any of these medicines be taken with food as shown?",
                        "How long should I continue these medicines?",
                        "What side effects should I report?",
                        "When should I return for review?"),
                sourceService.forTopic("medicine"),
                List.of(
                        "Do not invent or change dosage instructions.",
                        "DEMO DATA — NOT A REAL PATIENT"),
                null,
                items,
                null,
                "FAMILY SUMMARY\n\nThis prescription appears to list medicines with morning and night timing for at least one medicine.\nPlease verify every instruction against the original prescription with a doctor or pharmacist.\n\nDEMO DATA — NOT A REAL PATIENT",
                Map.of(),
                "Original prescription retained for side-by-side verification.",
                false,
                "We don't replace the doctor. We make healthcare easier to understand.");
    }

    public AnalysisResponse discharge(String sessionId) {
        return withSession(cachedDischarge(), sessionId);
    }

    private AnalysisResponse cachedDischarge() {
        AnalysisResponse cached = dischargeTemplate;
        if (cached == null) {
            synchronized (this) {
                cached = dischargeTemplate;
                if (cached == null) {
                    dischargeTemplate = cached = buildDischarge("template");
                }
            }
        }
        return cached;
    }

    private AnalysisResponse buildDischarge(String sessionId) {
        DischargeSummary summary =
                new DischargeSummary(
                        "Short hospital stay for evaluation of fever and dehydration (as written in the sample discharge note).",
                        "Supportive care and observation were documented. No surgical procedure is listed in the sample note.",
                        List.of(
                                "Fever settled during stay",
                                "Hydration improved",
                                "Labs reviewed before discharge"),
                        List.of(
                                "Oral rehydration guidance",
                                "Paracetamol as advised",
                                "Continue home medicines only if previously prescribed"),
                        List.of("Follow up in OPD in 5 days", "Return earlier if symptoms worsen"),
                        List.of(
                                "High fever returning",
                                "Persistent vomiting",
                                "Severe weakness or confusion"),
                        List.of(
                                "Which findings from the admission are most important for me to monitor at home?",
                                "Which medicines from the discharge list should I continue, and for how long?",
                                "What warning signs mean I should seek urgent care?",
                                "Do I need any repeat tests before the follow-up visit?",
                                "Who should I contact if symptoms return before the OPD date?"));

        return new AnalysisResponse(
                sessionId == null ? UUID.randomUUID().toString() : sessionId,
                DocumentType.DISCHARGE_SUMMARY,
                "Discharge Summary",
                true,
                privacyShield(),
                processingSteps(),
                2,
                0,
                List.of(),
                new DashboardSummaryDto(0, 0, 0, 0),
                summary.doctorQuestions(),
                sourceService.forTopic("discharge"),
                List.of(
                        "This is a simplified understanding aid, not medical advice.",
                        "DEMO DATA — NOT A REAL PATIENT"),
                null,
                null,
                summary,
                "FAMILY SUMMARY\n\nThe discharge note describes a short hospital stay and lists follow-up instructions and warning signs.\nPlease keep the original discharge summary for the doctor visit.\n\nDEMO DATA — NOT A REAL PATIENT",
                Map.of(),
                "Original discharge document retained for verification.",
                false,
                "We don't replace the doctor. We make healthcare easier to understand.");
    }

    private AnalysisResponse withSession(AnalysisResponse base, String sessionId) {
        String id = sessionId == null ? UUID.randomUUID().toString() : sessionId;
        return new AnalysisResponse(
                id,
                base.documentType(),
                base.documentLabel(),
                base.demo(),
                base.privacyShield(),
                base.processingSteps(),
                base.pages(),
                base.parametersDetected(),
                base.parameters(),
                base.dashboard(),
                base.doctorQuestions(),
                base.sources(),
                base.safetyNotes(),
                base.medicine(),
                base.prescriptionItems(),
                base.dischargeSummary(),
                base.familySummary(),
                base.translations(),
                base.originalPreviewNote(),
                base.aiUsed(),
                base.disclaimer());
    }

    private MedicalParameter param(
            String name,
            String value,
            String unit,
            String range,
            ParameterStatus status,
            String explanation,
            String simple,
            double confidence) {
        return new MedicalParameter(
                name,
                value,
                unit,
                range,
                status,
                explanation,
                simple,
                confidence,
                confidence < 0.7);
    }

    private DashboardSummaryDto summarize(List<MedicalParameter> parameters) {
        int within = 0;
        int discuss = 0;
        int important = 0;
        for (MedicalParameter p : parameters) {
            switch (p.status()) {
                case WITHIN_RANGE -> within++;
                case IMPORTANT_ATTENTION -> important++;
                default -> discuss++;
            }
        }
        return new DashboardSummaryDto(parameters.size(), within, discuss, important);
    }

    private PrivacyShieldDto privacyShield() {
        return new PrivacyShieldDto(
                List.of(
                        new PiiFinding("Patient name", "An****ma", true),
                        new PiiFinding("Phone number", "98****10", true),
                        new PiiFinding("Email", "an****om", true),
                        new PiiFinding("Address", "42****ru", true),
                        new PiiFinding("Patient ID", "UH****51", true),
                        new PiiFinding("Date of birth", "12****88", true)),
                "ArogyaLens automatically detects and masks common personal identifiers before processing.",
                true);
    }

    private PrivacyShieldDto privacyShieldMinimal() {
        return new PrivacyShieldDto(
                List.of(),
                "ArogyaLens automatically detects and masks common personal identifiers before processing.",
                false);
    }

    private List<ProcessingStepDto> processingSteps() {
        return List.of(
                new ProcessingStepDto("privacy", "Protecting personal information...", "done"),
                new ProcessingStepDto("understand", "Understanding document...", "done"),
                new ProcessingStepDto("extract", "Extracting medical information...", "done"),
                new ProcessingStepDto("simplify", "Simplifying medical terminology...", "done"),
                new ProcessingStepDto("translate", "Preparing multilingual explanation...", "done"),
                new ProcessingStepDto("sources", "Finding trusted sources...", "done"),
                new ProcessingStepDto("safety", "Running safety check...", "done"));
    }

    private String familySummaryLab() {
        return """
                FAMILY SUMMARY

                The report contains a few results that should be discussed with the doctor.
                The report does not by itself establish a diagnosis.

                Questions to ask during the appointment:
                1. What could explain the HbA1c and fasting glucose results?
                2. Do these tests need to be repeated?
                3. When should we follow up?

                DEMO DATA — NOT A REAL PATIENT
                """;
    }
}
