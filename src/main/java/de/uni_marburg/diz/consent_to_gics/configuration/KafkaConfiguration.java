package de.uni_marburg.diz.consent_to_gics.configuration;

import ca.uhn.hl7v2.model.v25.message.MDM_T02;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
import de.uni_marburg.diz.consent_to_gics.serde.Hl7Deserializer;
import de.uni_marburg.diz.consent_to_gics.serde.Hl7Serializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer;

@Slf4j
@Configuration
@EnableKafka
public class KafkaConfiguration {

    @Bean
    public StreamsBuilderFactoryBeanConfigurer streamsBuilderCustomizer() {

        return fb -> {
            fb.setKafkaStreamsCustomizer(
                kafkaStreams -> kafkaStreams.setUncaughtExceptionHandler(e -> {
                    log.error("Uncaught exception occurred.", e);
                    // default handler response
                    return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_CLIENT;
                }));
        };
    }

    @Bean
    public Serde<ORU_R01> oruSerde() {
        return Serdes.serdeFrom(new Hl7Serializer<>(), new Hl7Deserializer<>());
    }

    @Bean
    public Serde<MDM_T02> mdmSerde() {
        return Serdes.serdeFrom(new Hl7Serializer<>(), new Hl7Deserializer<>());
    }
}
