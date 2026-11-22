package de.uni_marburg.diz.consent_to_gics.configuration;

import lombok.Data;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.validation.annotation.Validated;

@Configuration
@EnableKafkaStreams
@ConfigurationProperties(prefix = "consent")
@Validated
@Data
public class ConsentProperties {

    private In in;
    private Out out;

    @Data
    public static class Topic {
        @NonNull
        private String policies;
        @NonNull
        private String documents;
    }

    @Data
    public static class In {
        @NonNull
        private Topic topic;
        private String filter;
    }

    @Data
    public static class Out {
        @NonNull
        private Gics gics;
    }

    @Data
    public static class Gics {
        @NonNull
        private Fhir fhir;
    }

    @Data
    public static class Fhir {
        @NonNull
        private String baseUrl;
        private Auth auth;
    }

    @Data
    public static class Auth {
        private Basic basic;
        private OAuth oauth;
    }

    @Data
    public static class Basic {
        private String user;
        private String password;
    }

    @Data
    public static class OAuth {
        private String endpoint;
        private String clientId;
        private String clientSecret;
    }
}
