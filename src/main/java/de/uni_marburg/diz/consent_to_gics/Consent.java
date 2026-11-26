package de.uni_marburg.diz.consent_to_gics;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v25.datatype.ED;
import ca.uhn.hl7v2.model.v25.group.MDM_T02_OBXNTE;
import ca.uhn.hl7v2.model.v25.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v25.message.MDM_T02;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@ToString
public class Consent {

    private final ORU_R01 policies;
    private final MDM_T02 document;

    Consent(ORU_R01 policies, MDM_T02 document) {
        this.policies = policies;
        this.document = document;
    }

    public String consentTemplateId() {
        return policies.getPATIENT_RESULT().getORDER_OBSERVATION()
            .getOBR().getObr4_UniversalServiceIdentifier().getIdentifier().getValue();
    }

    public String patientId() throws HL7Exception {
        return policies.getPATIENT_RESULT().getPATIENT().getPID().getPid3_PatientIdentifierList(0).getCx1_IDNumber().getValue();
    }

    public Date date() throws DataTypeException {
        // TODO verify date
        return document.getTXA().getTxa4_ActivityDateTime().getTime().getValueAsDate();
    }

    public List<String> policies() {
        try {
            return policies.getPATIENT_RESULT().getORDER_OBSERVATION().getOBSERVATIONAll()
                .stream().map(ORU_R01_OBSERVATION::getOBX)
                .filter(o -> !o.getObx3_ObservationIdentifier()
                    .getIdentifier()
                    .getValue().equals("Arztdaten"))
                .map(o -> o.getObx5_ObservationValue(0).getData().toString())
                .filter(Objects::nonNull)
                .toList();
        } catch (HL7Exception e) {
            return List.of();
        }
    }

    public String informingPerson() {
        try {
            return policies.getPATIENT_RESULT().getORDER_OBSERVATION().getOBSERVATIONAll()
                .stream()
                .filter(o -> o.getOBX()
                    .getObx3_ObservationIdentifier()
                    .getIdentifier()
                    .getValue().equals("Arztdaten"))
                .findAny()
                .map(o -> o.getOBX().getObx5_ObservationValue(0).getData().toString())
                .orElseThrow();
        } catch (HL7Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String documentFileNme() {
        return document.getTXA().getTxa16_UniqueDocumentFileName().getValue();
    }

    public String documentContentType() throws HL7Exception {

        return obxEDValue().getEd1_SourceApplication().getHd1_NamespaceID().getValue();
    }

    public String documentData() throws HL7Exception {

        return obxEDValue().getEd3_DataSubtype().getValue();
    }

    private ED obxEDValue() throws HL7Exception {
        return this.document
            .getOBXNTEAll()
            .stream()
            .map(MDM_T02_OBXNTE::getOBX)
            .filter(o -> "ED".equals(o.getObx2_ValueType().getValue()))
            .map(o -> (ED) o.getObx5_ObservationValue(0).getData())
            .findAny().orElseThrow();
    }

    public String sendingApplication() {
        return this.document.getMSH().getMsh3_SendingApplication().getHd1_NamespaceID().getValue();
    }
}
