package de.uni_marburg.diz.consent_to_gics;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.HashMap;

@Slf4j
@Service
public class FhirClient {

    private static final long BACKOFF_INITIAL_INTERVAL = 5000;
    private static final double BACKOFF_MULTIPLIER = 1.25;
    private static final int RETRY_MAX_ATTEMPTS = 3;
    private final RetryTemplate retryTemplate;
    private final IGenericClient client;
    private final FhirContext fhirContext;


    public FhirClient(IGenericClient client) {
        this.fhirContext = client.getFhirContext();
        this.client = client;
        this.retryTemplate = defaultTemplate();
    }

    public Bundle addConsent(String templateId, Parameters params) {
        log.info("Sending consent '{}' to gICS at {}", templateId,
            this.client.getServerBase());
        log.trace("{}", fhirContext.newJsonParser().encodeResourceToString(params));

        return retryTemplate.execute(
            ctx -> client.operation().onServer().named("addConsent")
                .withParameters(params).returnResourceType(Bundle.class)
                .execute());
    }

    static RetryTemplate defaultTemplate() {
        var retryTemplate = new RetryTemplate();

        var backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(BACKOFF_INITIAL_INTERVAL);
        backOffPolicy.setMultiplier(BACKOFF_MULTIPLIER);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        var retryable = new HashMap<Class<? extends Throwable>, Boolean>();
        retryable.put(HttpClientErrorException.class, false);
        retryable.put(HttpServerErrorException.class, true);
        retryable.put(ResourceAccessException.class, true);
        retryable.put(FhirClientConnectionException.class, true);

        var retryPolicy = new SimpleRetryPolicy(RETRY_MAX_ATTEMPTS, retryable);

        retryTemplate.setRetryPolicy(retryPolicy);

        retryTemplate.registerListener(new RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(RetryContext context,
                                                         RetryCallback<T, E> callback, Throwable throwable) {
                log.warn("http error occurred: {}. Retrying attempt: {}",
                    throwable.getMessage(),
                    context.getRetryCount());
            }
        });

        return retryTemplate;
    }

}
