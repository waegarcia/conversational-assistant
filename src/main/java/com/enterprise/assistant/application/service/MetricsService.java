package com.enterprise.assistant.application.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final Counter conversationsCreated;
    private final Counter messagesProcessed;
    private final Counter externalApiCalls;
    private final Counter externalApiSuccesses;
    private final Counter externalApiFailures;
    private final AtomicInteger activeConversations;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.conversationsCreated = Counter.builder("conversations.created")
                .description("Total number of conversations created")
                .register(meterRegistry);

        this.messagesProcessed = Counter.builder("messages.processed")
                .description("Total number of messages processed")
                .register(meterRegistry);

        this.externalApiCalls = Counter.builder("external.api.calls")
                .description("Total number of external API calls")
                .tag("service", "weather")
                .register(meterRegistry);

        this.externalApiSuccesses = Counter.builder("external.api.success")
                .description("Total number of successful external API calls")
                .tag("service", "weather")
                .register(meterRegistry);

        this.externalApiFailures = Counter.builder("external.api.failures")
                .description("Total number of failed external API calls")
                .tag("service", "weather")
                .register(meterRegistry);

        this.activeConversations = meterRegistry.gauge(
                "conversations.active",
                new AtomicInteger(0)
        );

        log.info("MetricsService initialized with custom metrics");
    }

    public void incrementConversationsCreated() {
        conversationsCreated.increment();
        if (activeConversations != null) {
            activeConversations.incrementAndGet();
        }
        log.debug("Conversation created metric incremented");
    }

    public void incrementMessagesProcessed() {
        messagesProcessed.increment();
        log.debug("Message processed metric incremented");
    }

    public void recordIntentDetected(Intent intent) {
        Counter.builder("intents.detected")
                .description("Number of intents detected by type")
                .tag("intent", intent.name())
                .register(meterRegistry)
                .increment();
        log.debug("Intent detected: {}", intent);
    }

    public void incrementExternalApiCall() {
        externalApiCalls.increment();
        log.debug("External API call metric incremented");
    }

    public void recordExternalApiSuccess() {
        externalApiSuccesses.increment();
        log.debug("External API success metric incremented");
    }

    public void recordExternalApiFailure() {
        externalApiFailures.increment();
        log.debug("External API failure metric incremented");
    }

    public void decrementActiveConversations() {
        if (activeConversations != null) {
            activeConversations.decrementAndGet();
        }
        log.debug("Active conversations decremented");
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordResponseTime(Timer.Sample sample, Intent intent) {
        sample.stop(Timer.builder("response.time")
                .description("Response time by intent type")
                .tag("intent", intent.name())
                .register(meterRegistry));
        log.debug("Response time recorded for intent: {}", intent);
    }

    public void recordError(String errorType) {
        Counter.builder("errors.occurred")
                .description("Number of errors by type")
                .tag("type", errorType)
                .register(meterRegistry)
                .increment();
        log.debug("Error recorded: {}", errorType);
    }
}
