package com.lifeadmin.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Top-level DTO representing the structured JSON response from the LLM.
 * Jackson ignores unexpected fields for forward compatibility.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiAnalysisResponse {

    private String documentType;
    private String summary;
    private List<AiKeyDateDto> keyDates;
    private List<AiObligationDto> obligations;
    private List<AiDateRelationshipDto> dateRelationships;
    private List<String> uncertainties;

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<AiKeyDateDto> getKeyDates() { return keyDates; }
    public void setKeyDates(List<AiKeyDateDto> keyDates) { this.keyDates = keyDates; }

    public List<AiObligationDto> getObligations() { return obligations; }
    public void setObligations(List<AiObligationDto> obligations) { this.obligations = obligations; }

    public List<AiDateRelationshipDto> getDateRelationships() { return dateRelationships; }
    public void setDateRelationships(List<AiDateRelationshipDto> dateRelationships) { this.dateRelationships = dateRelationships; }

    public List<String> getUncertainties() { return uncertainties; }
    public void setUncertainties(List<String> uncertainties) { this.uncertainties = uncertainties; }
}
