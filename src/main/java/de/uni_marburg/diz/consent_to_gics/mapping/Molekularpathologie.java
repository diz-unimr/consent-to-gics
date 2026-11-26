package de.uni_marburg.diz.consent_to_gics.mapping;

import ca.uhn.hl7v2.HL7Exception;
import de.uni_marburg.diz.consent_to_gics.Consent;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Base64BinaryType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Signature;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.codesystems.DataAbsentReason;

import java.util.List;
import java.util.stream.Stream;


public class Molekularpathologie {

    private static final QuestionnaireResponse.QuestionnaireResponseItemComponent mandatoryPolicy = new QuestionnaireResponse.QuestionnaireResponseItemComponent()
        .setLinkId("Molekulargenetische_Untersuchung|1.0")
        .setAnswer(List.of(new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
            .setValue(new Coding("urn:oid:2.16.840.1.113883.3.1937.777.24.5.2", "2.16.840.1.113883.3.1937.777.24.5.2.1", "gültig"))));

    public static Parameters map(Consent consent) throws HL7Exception {
        var params = new Parameters();
        var patient = new Patient()
            .setIdentifier(List.of(new Identifier()
                .setType(new CodeableConcept()
                    .setCoding(List.of(new Coding("http://terminology.hl7.org/CodeSystem/v2-0203", "ANON", "Anonymous identifier"))))
                .setSystem("https://ths-greifswald.de/fhir/gics/identifiers/Patienten-ID")
                .setValue(consent.patientId())));
        patient.setId("1").setMeta(new Meta().setProfile(List.of(new CanonicalType("http://fhir.de/ConsentManagement/StructureDefinition/Patient"))));

        var patientRef = new Reference(String.format("Patient/%s", patient.getId()));

        var questionnaire = new QuestionnaireResponse()
            .setQuestionnaire("https://ths-greifswald.de/fhir/gics/QuestionnaireComposed/molekularpathologie/Patienteneinwilligung+Molekularpathologie|1.0")
            .setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED)
            .setSubject(patientRef)
            .setAuthored(consent.date())
            .setAuthor(patientRef)
            .setSource(patientRef)
            .setItem(Stream.concat(Stream.of(mandatoryPolicy),
                consent
                    .policies()
                    .stream()
                    .map(i -> new QuestionnaireResponse.QuestionnaireResponseItemComponent()
                        .setLinkId(parsePolicyId(i))
                        .setAnswer(List.of(new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
                            .setValue(Mapper.parsePolicyValue(i)))))
            ).toList());
        questionnaire.setId("2").setLanguage("de");

        var docData = new Base64BinaryType();
        docData.setValueAsString(consent.documentData());
        var docRef = new DocumentReference()
            .setStatus(Enumerations.DocumentReferenceStatus.CURRENT)
            .setSubject(patientRef)
            .setDescription(consent.documentFileNme())
            .setContent(List.of(new DocumentReference.DocumentReferenceContentComponent()
                .setAttachment(
                    new Attachment()
                        .setContentType(consent.documentContentType())
                        .setData(docData.getValue())
                ))
            );
        docRef.setId("3").setMeta(new Meta().setProfile(List.of(new CanonicalType("http://fhir.de/ConsentManagement/StructureDefinition/DocumentReference"))));

        var dataAbsent = new Base64BinaryType();
        dataAbsent.setExtension(List.of(
            new Extension(
                "http://hl7.org/fhir/StructureDefinition/data-absent-reason",
                new CodeType(DataAbsentReason.UNSUPPORTED.toCode()))));
        // patient signature
        var consentSignature = new Signature()
            .setType(List.of(Mapper.consentSignatureCoding))
            // todo which is the signature date?
            .setWhen(consent.date())
            .setWho(patientRef)
            .setDataElement(dataAbsent);
        consentSignature.setExtension(List.of(
            new Extension()
                .setUrl("http://fhir.de/ConsentManagement/StructureDefinition/SignatureLocation")
                .setValue(new StringType("Marburg"))));

        // informing person signature
        var verificationSignature = new Signature()
            .setType(List.of(Mapper.verificationSignatureCoding))
            // todo which is the signature date?
            .setWhen(consent.date())
            .setWho(new Reference().setDisplay(consent.informingPerson()))
            .setDataElement(dataAbsent);
        verificationSignature.setExtension(List.of(
            new Extension()
                .setUrl("http://fhir.de/ConsentManagement/StructureDefinition/SignatureLocation")
                .setValue(new StringType("Marburg"))));

        var provenance = new Provenance()
            .setTarget(List.of(new Reference(String.format("QuestionnaireResponse/%s", questionnaire.getId()))))
            .setRecorded(consent.date())
            .setAgent(List.of(new Provenance.ProvenanceAgentComponent()
                .setWho(new Reference().setDisplay(consent.sendingApplication()))))
            .setEntity(List.of(new Provenance.ProvenanceEntityComponent()
                .setRole(Provenance.ProvenanceEntityRole.SOURCE)
                .setWhat(new Reference(String.format("DocumentReference/%s", docRef.getId())))))
            .setSignature(List.of(consentSignature, verificationSignature));
        provenance.setMeta(new Meta()
            .setProfile(List.of(new CanonicalType("http://fhir.de/ConsentManagement/StructureDefinition/Provenance"))));

        params.addParameter().setName("patient")
            .setResource(patient);
        params.addParameter().setName("questionnaireResponse")
            .setResource(questionnaire);
        params.addParameter().setName("documentReference")
            .setResource(docRef);
        params.addParameter().setName("domain")
            .setValue(new StringType("molekularpathologie"));
        params.addParameter().setName("provenance")
            .setResource(provenance);
        return params;
    }

    static String parsePolicyId(String policyItem) {
        var policyId = policyItem.split("_")[0];
        return switch (policyId) {
            case "custom.RB-consent-Rekontaktierung" -> "Kontaktaufnahme_persoenlich|1.0";
            case "custom.RB-consent-Rekontaktierung-Arzt" -> "Kontaktaufnahme_Arzt|1.0";
            case "custom.RB-consent-Probenverwendung" -> "Auswertung_Projekte|1.0";
            case "custom.RB-consent-Ergebnisauswertung" -> "Auswertung_MTB|1.0";
            default -> throw new IllegalArgumentException(String.format("Unexpected policy id: %s", policyId));
        };
    }
}
