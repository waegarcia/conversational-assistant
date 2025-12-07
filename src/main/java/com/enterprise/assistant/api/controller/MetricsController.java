package com.enterprise.assistant.api.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MeterRegistry meterRegistry;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getMetricsSummary() {
        Map<String, Object> metrics = new HashMap<>();

        Counter created = meterRegistry.find("conversations.created").counter();
        Gauge active = meterRegistry.find("conversations.active").gauge();
        metrics.put("conversationsCreated", created != null ? created.count() : 0);
        metrics.put("conversationsActive", active != null ? active.value() : 0);

        Counter messages = meterRegistry.find("messages.processed").counter();
        metrics.put("messagesProcessed", messages != null ? messages.count() : 0);

        Counter apiCalls = meterRegistry.find("external.api.calls").counter();
        Counter apiFailures = meterRegistry.find("external.api.failures").counter();
        metrics.put("externalApiCalls", apiCalls != null ? apiCalls.count() : 0);
        metrics.put("externalApiFailures", apiFailures != null ? apiFailures.count() : 0);

        return ResponseEntity.ok(metrics);
    }
}
