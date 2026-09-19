package com.lifeadmin.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AiObligationDto {

    private String type;
    private String description;
    private String relatedDateType;
    private String evidence;
    private Double confidence;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRelatedDateType() { return relatedDateType; }
    public void setRelatedDateType(String relatedDateType) { this.relatedDateType = relatedDateType; }

    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
}
