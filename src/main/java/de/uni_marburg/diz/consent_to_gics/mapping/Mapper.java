package de.uni_marburg.diz.consent_to_gics.mapping;

import ca.uhn.hl7v2.HL7Exception;
import de.uni_marburg.diz.consent_to_gics.Consent;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Parameters;
import org.springframework.stereotype.Service;

import java.util.HashMap;


@Slf4j
@Service
public class Mapper {

    private final HashMap<String, MapFunction<Consent, Parameters>> mappings = new HashMap<>();

    public static final Coding yesAnswer = new Coding("urn:oid:2.16.840.1.113883.3.1937.777.24.5.2", "2.16.840.1.113883.3.1937.777.24.5.2.1", "gültig");
    public static final Coding noAnswer = new Coding("urn:oid:2.16.840.1.113883.3.1937.777.24.5.2", "2.16.840.1.113883.3.1937.777.24.5.2.2", "ungültig");
    public static final Coding consentSignatureCoding = new Coding("urn:iso-astm:E1762-95:2013", "1.2.840.10065.1.12.1.7", "Consent Signature");
    public static final Coding verificationSignatureCoding = new Coding("urn:iso-astm:E1762-95:2013", "1.2.840.10065.1.12.1.5", "Verification Signature");

    public Mapper() {
        mappings.put("E-EDP62_0005764449", Molekularpathologie::map);
    }

    public boolean hasMapping(String consentTemplateId) {
        return mappings.containsKey(consentTemplateId);
    }

    public Parameters map(Consent consent) throws HL7Exception {
        var templateId = consent.consentTemplateId();
        return mappings.get(templateId).apply(consent);
    }

    public static Coding parsePolicyValue(String policyItem) {
        var policyValue = policyItem.split("_")[1];
        return switch (policyValue) {
            case "yes" -> yesAnswer;
            case "no" -> noAnswer;
            default -> throw new IllegalArgumentException(String.format("Unexpected policy value: %s", policyValue));
        };
    }
}

@FunctionalInterface
interface MapFunction<T, R> {
    R apply(T t) throws HL7Exception;
}