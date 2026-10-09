package com.arogyalens.history;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** "My history": list and delete PII-masked entries for the calling device. */
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    /** Header carrying the anonymous device id generated in the browser. */
    public static final String DEVICE_HEADER = "X-Device-Id";

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public List<HistoryService.HistoryItem> list(
            @RequestHeader(value = DEVICE_HEADER, required = false) String deviceId) {
        return historyService.list(deviceId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = DEVICE_HEADER, required = false) String deviceId,
            @PathVariable long id) {
        historyService.delete(deviceId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public Map<String, Long> deleteAll(
            @RequestHeader(value = DEVICE_HEADER, required = false) String deviceId) {
        return Map.of("deleted", historyService.deleteAll(deviceId));
    }
}
