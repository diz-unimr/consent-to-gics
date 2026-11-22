package de.uni_marburg.diz.consent_to_gics;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

@EmbeddedKafka(partitions = 1, ports = 9092)
@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
    properties = {
        "consent.in.topic.policies=policies",
        "consent.in.topic.documents=documents"
    })
public class ApplicationTests {

    @Test
    void contextLoads() {
    }

}
