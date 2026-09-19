package com.lifeadmin.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a relationship between dates identified by the AI.
 * The computedDate is the AI's interpretation — Phase 5 performs
 * deterministic validation/calculation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiDateRelationshipDto {

    private String type;
    private String anchorDateType;
    private Integer offsetDays;
    private String computedDate;
    private String evidence;
    private Double confidence;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAnchorDateType() { return anchorDateType; }
    public void setAnchorDateType(String anchorDateType) { this.anchorDateType = anchorDateType; }

    public Integer getOffsetDays() { return offsetDays; }
    public void setOffsetDays(Integer offsetDays) { this.offsetDays = offsetDays; }

    public String getComputedDate() { return computedDate; }
    public void setComputedDate(String computedDate) { this.computedDate = computedDate; }

    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
}
