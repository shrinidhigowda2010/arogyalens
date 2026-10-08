package com.arogyalens.source;

import com.arogyalens.model.TrustedSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SourceService {

    private final Map<String, TrustedSource> catalog = new ConcurrentHashMap<>();

    public SourceService() {
        seed();
    }

    private void seed() {
        put(new TrustedSource(
                "who-lab",
                "World Health Organization",
                "Laboratory and diagnostic information",
                "Official global health guidance for interpreting common health topics.",
                "https://www.who.int/health-topics",
                "GENERAL_HEALTH_INFORMATION"
        ));
        put(new TrustedSource(
                "mohfw",
                "Ministry of Health and Family Welfare, Government of India",
                "National health resources",
                "Government of India public health information for patients and caregivers.",
                "https://www.mohfw.gov.in/",
                "GENERAL_HEALTH_INFORMATION"
        ));
        put(new TrustedSource(
                "aiims",
                "AIIMS",
                "Patient education resources",
                "Institutional information from a leading Indian medical institute.",
                "https://www.aiims.edu/",
                "GENERAL_HEALTH_INFORMATION"
        ));
        put(new TrustedSource(
                "ipc",
                "Indian Pharmacopoeia Commission",
                "Official medicine information",
                "Authoritative pharmaceutical standards and medicine information in India.",
                "https://www.ipc.gov.in/",
                "GENERAL_HEALTH_INFORMATION"
        ));
        put(new TrustedSource(
                "who-diabetes",
                "World Health Organization",
                "Diabetes overview",
                "General information about blood sugar related health topics.",
                "https://www.who.int/health-topics/diabetes",
                "GENERAL_HEALTH_INFORMATION"
        ));
        put(new TrustedSource(
                "cdsco",
                "Central Drugs Standard Control Organization",
                "Drug regulatory information",
                "Official Indian drug regulatory authority resources.",
                "https://cdsco.gov.in/",
                "GENERAL_HEALTH_INFORMATION"
        ));
    }

    private void put(TrustedSource source) {
        catalog.put(source.id(), source);
    }

    public Optional<TrustedSource> findById(String id) {
        return Optional.ofNullable(catalog.get(id));
    }

    public List<TrustedSource> forTopic(String topic) {
        if (topic == null) {
            return List.of(catalog.get("who-lab"), catalog.get("mohfw"));
        }
        String t = topic.toLowerCase(Locale.ROOT);
        List<TrustedSource> result = new ArrayList<>();
        if (t.contains("hba1c") || t.contains("glucose") || t.contains("sugar") || t.contains("diabetes")) {
            result.add(catalog.get("who-diabetes"));
            result.add(catalog.get("mohfw"));
        } else if (t.contains("medicine") || t.contains("metformin") || t.contains("drug")) {
            result.add(catalog.get("ipc"));
            result.add(catalog.get("cdsco"));
        } else {
            result.add(catalog.get("who-lab"));
            result.add(catalog.get("aiims"));
            result.add(catalog.get("mohfw"));
        }
        return result.stream().distinct().toList();
    }

    public List<TrustedSource> all() {
        return List.copyOf(catalog.values());
    }
}
