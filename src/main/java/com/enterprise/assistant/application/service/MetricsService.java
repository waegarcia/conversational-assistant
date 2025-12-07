package com.enterprise.assistant.application.service;

import com.enterprise.assistant.domain.model.Intent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final Counter conversationsCreated;
    private final Counter messagesProcessed;
    private final Counter externalApiCalls;
    private final Counter externalApiFailures;
    private final AtomicInteger activeConversations;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.conversationsCreated = Counter.builder("conversations.created").register(meterRegistry);
        this.messagesProcessed = Counter.builder("messages.processed").register(meterRegistry);
        this.externalApiCalls = Counter.builder("external.api.calls").register(meterRegistry);
        this.externalApiFailures = Counter.builder("external.api.failures").register(meterRegistry);
        this.activeConversations = meterRegistry.gauge("conversations.active", new AtomicInteger(0));
    }

    public void incrementConversationsCreated() {
        conversationsCreated.increment();
        if (activeConversations != null) {
            activeConversations.incrementAndGet();
        }
    }

    public void incrementMessagesProcessed() {
        messagesProcessed.increment();
    }

    public void recordIntentDetected(Intent intent) {
        Counter.builder("intents.detected")
                .tag("intent", intent.name())
                .register(meterRegistry)
                .increment();
    }

    public void incrementExternalApiCall() {
        externalApiCalls.increment();
    }

    public void recordExternalApiFailure() {
        externalApiFailures.increment();
    }

    public void decrementActiveConversations() {
        if (activeConversations != null) {
            activeConversations.decrementAndGet();
        }
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordResponseTime(Timer.Sample sample, Intent intent) {
        sample.stop(Timer.builder("response.time")
                .tag("intent", intent.name())
                .register(meterRegistry));
    }

}
