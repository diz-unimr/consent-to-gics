package de.uni_marburg.diz.consent_to_gics;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.uni_marburg.diz.consent_to_gics.configuration.ConsentProperties;
import de.uni_marburg.diz.consent_to_gics.configuration.KafkaConfiguration;
import de.uni_marburg.diz.consent_to_gics.mapping.Mapper;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TopologyTestDriver;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
public class ProcessorTests {

    @Value("classpath:consent_policies.hl7")
    private Resource hl7Policies;

    @Value("classpath:consent_document.hl7")
    private Resource hl7Document;

    @Test
    public void buildPipelineConfiguresProcessor() throws IOException {

        var kafkaConfig = new KafkaConfiguration();

        var policies = "policies";
        var documents = "documents";

        var props = new ConsentProperties();
        props.setIn(new ConsentProperties.In(new ConsentProperties.Topic(policies, documents)));
        props.setOut(new ConsentProperties.Out(new ConsentProperties.Gics(new ConsentProperties.Fhir("http://ths/ttp-fhir/fhir/gics/"))));

        var mapper = new Mapper();
        var fhirContext = FhirContext.forR4();

        // mock REST client
        var mockClient = mock(IGenericClient.class, RETURNS_DEEP_STUBS);
        when(mockClient.operation().onServer().named("addConsent")
            .withParameters(any()).returnResourceType(Bundle.class)
            .execute())
            .thenReturn(new Bundle());
        var fhirClient = new FhirClient(mockClient);

        // build stream
        var builder = new StreamsBuilder();
        new Processor(props, kafkaConfig.oruSerde(), kafkaConfig.mdmSerde(), mapper, fhirClient, fhirContext).buildPipeline(builder);

        try (var driver = new TopologyTestDriver(builder.build())) {

            var policiesTopic =
                driver.createInputTopic(policies, new StringSerializer(),
                    new StringSerializer());
            var documentsTopic =
                driver.createInputTopic(documents, new StringSerializer(),
                    new StringSerializer());

            // create input records
            policiesTopic.pipeInput("test", hl7Policies.getContentAsString(StandardCharsets.UTF_8));
            documentsTopic.pipeInput("test", hl7Document.getContentAsString(StandardCharsets.UTF_8));

            // 'addConsent' operation called
            verify(mockClient, times(1));
        }
    }
}
