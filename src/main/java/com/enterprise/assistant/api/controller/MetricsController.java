package com.enterprise.assistant.api.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@Tag(name = "Metrics", description = "Custom metrics API for monitoring")
public class MetricsController {

    private final MeterRegistry meterRegistry;

    @GetMapping("/summary")
    @Operation(summary = "Get metrics summary", description = "Returns a summary of key metrics for monitoring")
    public ResponseEntity<Map<String, Object>> getMetricsSummary() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("conversations", getConversationMetrics());
        metrics.put("messages", getMessageMetrics());
        metrics.put("intents", getIntentMetrics());
        metrics.put("externalApi", getExternalApiMetrics());
        metrics.put("performance", getPerformanceMetrics());
        metrics.put("errors", getErrorMetrics());

        return ResponseEntity.ok(metrics);
    }

    private Map<String, Object> getConversationMetrics() {
        Map<String, Object> conversationMetrics = new HashMap<>();

        Counter conversationsCreated = meterRegistry.find("conversations.created").counter();
        conversationMetrics.put("totalCreated", conversationsCreated != null ? conversationsCreated.count() : 0);

        Gauge activeConversations = meterRegistry.find("conversations.active").gauge();
        conversationMetrics.put("currentlyActive", activeConversations != null ? activeConversations.value() : 0);

        return conversationMetrics;
    }

    private Map<String, Object> getMessageMetrics() {
        Map<String, Object> messageMetrics = new HashMap<>();

        Counter messagesProcessed = meterRegistry.find("messages.processed").counter();
        messageMetrics.put("totalProcessed", messagesProcessed != null ? messagesProcessed.count() : 0);

        return messageMetrics;
    }

    private Map<String, Object> getIntentMetrics() {
        Map<String, Object> intentMetrics = new HashMap<>();

        String[] intents = {"GREETING", "WEATHER_QUERY", "HELP", "FAREWELL", "UNKNOWN"};

        for (String intent : intents) {
            Counter intentCounter = meterRegistry.find("intents.detected")
                    .tag("intent", intent)
                    .counter();
            intentMetrics.put(intent.toLowerCase(), intentCounter != null ? intentCounter.count() : 0);
        }

        return intentMetrics;
    }

    private Map<String, Object> getExternalApiMetrics() {
        Map<String, Object> apiMetrics = new HashMap<>();

        Counter apiCalls = meterRegistry.find("external.api.calls").counter();
        Counter apiSuccesses = meterRegistry.find("external.api.success").counter();
        Counter apiFailures = meterRegistry.find("external.api.failures").counter();

        double totalCalls = apiCalls != null ? apiCalls.count() : 0;
        double successes = apiSuccesses != null ? apiSuccesses.count() : 0;
        double failures = apiFailures != null ? apiFailures.count() : 0;

        apiMetrics.put("totalCalls", totalCalls);
        apiMetrics.put("successes", successes);
        apiMetrics.put("failures", failures);
        apiMetrics.put("successRate", totalCalls > 0 ? (successes / totalCalls) * 100 : 0);

        return apiMetrics;
    }

    private Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> performanceMetrics = new HashMap<>();

        String[] intents = {"GREETING", "WEATHER_QUERY", "HELP", "FAREWELL", "UNKNOWN"};

        for (String intent : intents) {
            Timer timer = meterRegistry.find("response.time")
                    .tag("intent", intent)
                    .timer();

            if (timer != null && timer.count() > 0) {
                Map<String, Object> intentPerformance = new HashMap<>();
                intentPerformance.put("count", timer.count());
                intentPerformance.put("avgMs", timer.mean(TimeUnit.MILLISECONDS));
                intentPerformance.put("maxMs", timer.max(TimeUnit.MILLISECONDS));
                performanceMetrics.put(intent.toLowerCase(), intentPerformance);
            }
        }

        return performanceMetrics;
    }

    private Map<String, Object> getErrorMetrics() {
        Map<String, Object> errorMetrics = new HashMap<>();

        Counter weatherApiErrors = meterRegistry.find("errors.occurred")
                .tag("type", "weather_api_error")
                .counter();

        errorMetrics.put("weatherApiErrors", weatherApiErrors != null ? weatherApiErrors.count() : 0);

        return errorMetrics;
    }
}
