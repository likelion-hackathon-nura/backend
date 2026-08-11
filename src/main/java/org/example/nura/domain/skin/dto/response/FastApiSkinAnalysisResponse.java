package org.example.nura.domain.skin.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FastApiSkinAnalysisResponse(
        @JsonProperty("redness_score")
        Integer rednessScore,

        @JsonProperty("acne_count")
        Integer acneCount
) {
}