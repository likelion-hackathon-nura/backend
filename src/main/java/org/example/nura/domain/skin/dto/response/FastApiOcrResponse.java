package org.example.nura.domain.skin.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.nura.domain.skin.entity.enums.CosmeticType;

public record FastApiOcrResponse(
        @JsonProperty("extracted_brand")
        String extractedBrand,

        @JsonProperty("extracted_name")
        String extractedName,

        @JsonProperty("extracted_type")
        CosmeticType extractedType,

        @JsonProperty("extracted_ingredient")
        String extractedIngredient,

        @JsonProperty("extracted_core_ingredient")
        String extractedCoreIngredient
) {
}