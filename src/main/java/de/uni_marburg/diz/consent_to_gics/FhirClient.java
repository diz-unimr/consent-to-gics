package de.uni_marburg.diz.consent_to_gics;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.hl7v2.HL7Exception;
import de.uni_marburg.diz.consent_to_gics.configuration.ConsentProperties;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
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
import java.util.List;

@Slf4j
@Service
public class FhirClient {

    private static final long BACKOFF_INITIAL_INTERVAL = 5000;
    private static final double BACKOFF_MULTIPLIER = 1.25;
    private static final int RETRY_MAX_ATTEMPTS = 3;
    private final ConsentProperties properties;
    private final RetryTemplate retryTemplate;
    private final IGenericClient client;


    public FhirClient(FhirContext fhirContext,
                      ConsentProperties properties) {
        this.properties = properties;
        this.client = fhirContext.newRestfulGenericClient(properties.getOut().getGics().getFhir().getBaseUrl());
        this.retryTemplate = defaultTemplate();
    }

    public OperationOutcome addConsent(Consent consent) throws HL7Exception {
        log.info("Sending consent '{}' to gICS at {}", consent.consentTemplateName(),
            this.client.getServerBase());

        var param = mapConsent(consent);

        OperationOutcome result = retryTemplate.execute(
            ctx -> client.operation().onServer().named("addConsent")
                .withParameters(param).returnResourceType(OperationOutcome.class)
                .execute());

        return result;
    }

    private Parameters mapConsent(Consent consent) throws HL7Exception {
        var params = new Parameters();
        var patient = new Patient();
        patient.setMeta(new Meta().setProfile(List.of(new CanonicalType("http://fhir.de/ConsentManagement/StructureDefinition/Patient"))));
        patient.setIdentifier(List.of(new Identifier().setSystem("https://ths-greifswald.de/fhir/gics/identifiers/Patienten-ID").setValue(consent.patientId())));

        var patientRef = new Reference(String.format("Patient/%s", consent.patientId()));

        var questionnaire = new QuestionnaireResponse()
            .setQuestionnaire("https://ths-greifswald.de/fhir/gics/QuestionnaireComposed/MII/Patienteneinwilligung+MII|1.6.m")
            .setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED)
            .setSubject(patientRef)
            .setAuthored(consent.date())
            .setAuthor(consent.author())
            .setSource(patientRef)
            .setItem(
                consent.items()
//                new QuestionnaireResponse.QuestionnaireResponseItemComponent()
            );

        params.addParameter().setName("patient")
            .setResource(patient);
        params.addParameter().setName("questionnaireResponse")
            .setResource(questionnaire);
        return params;
    }

    static RetryTemplate defaultTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

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
