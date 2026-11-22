package de.uni_marburg.diz.consent_to_gics;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.message.MDM_T02;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
import lombok.ToString;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;

import java.util.Date;
import java.util.List;

@ToString
public class Consent {

    private final ORU_R01 policies;
    private final MDM_T02 document;

    Consent(ORU_R01 policies, MDM_T02 document) {
        this.policies = policies;
        this.document = document;
    }

    public String consentTemplateName() {
        return policies.getPATIENT_RESULT()
            .getORDER_OBSERVATION()
            .getOBR()
            .getObr4_UniversalServiceIdentifier()
            .getIdentifier()
            .getValue();
    }


    public String patientId() throws HL7Exception {
        return policies.getPATIENT_RESULT().getPATIENT().getPID().getPid3_PatientIdentifierList(0).encode();
    }

    public Date date() throws HL7Exception {
        return policies.getPATIENT_RESULT().getORDER_OBSERVATION().getOBR()
            .getObr14_SpecimenReceivedDateTime().getTime().getValueAsDate();
    }

    public Reference author() {
        throw new NotImplementedException();
    }

    public List<QuestionnaireResponse.QuestionnaireResponseItemComponent> items() throws HL7Exception {
        return policies.getPATIENT_RESULT().getORDER_OBSERVATIONAll()
            .stream().map(o ->
                new QuestionnaireResponse.QuestionnaireResponseItemComponent(
                    new StringType(
                        o.getOBR().getObr4_UniversalServiceIdentifier().getCe1_Identifier().getValue())))
            .toList();
    }
}
