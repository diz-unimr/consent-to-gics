package de.uni_marburg.diz.consent_to_gics;

import ca.uhn.fhir.context.FhirContext;
import de.uni_marburg.diz.consent_to_gics.configuration.ConsentProperties;
import de.uni_marburg.diz.consent_to_gics.configuration.KafkaConfiguration;
import de.uni_marburg.diz.consent_to_gics.mapping.Mapper;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
        props.setOut(new ConsentProperties.Out(new ConsentProperties.Gics(new ConsentProperties.Fhir("test-url"))));

        var mapper = new Mapper();
        var fhirContext = FhirContext.forR4();
        var fhirClient = new FhirClient(fhirContext, props);

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

            // todo assert consent sent
            assert (true);
        }
    }
}
