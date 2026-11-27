package de.uni_marburg.diz.consent_to_gics.configuration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class FhirConfiguration {


    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    @Bean
    public IGenericClient restClient(FhirContext ctx, ConsentProperties properties) {
        var clientProps = properties.getOut().getGics().getFhir();
        var client = ctx.newRestfulGenericClient(clientProps.getBaseUrl());
        var basicAuth = Optional.of(clientProps).map(ConsentProperties.Fhir::getAuth).map(ConsentProperties.Auth::getBasic);
        if (basicAuth.isPresent()) {
            var auth = new BasicAuthInterceptor(basicAuth.get().getUser(), basicAuth.get().getPassword());
            client.registerInterceptor(auth);
        }
        return client;
    }
}
