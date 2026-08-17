package org.example.nura.domain.cosmetics.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.nura.domain.cosmetics.entity.RegisteredCosmetic;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class RegisteredCosmeticListResponse {

    private int totalCount;
    private List<CosmeticDetail> cosmetics;

    @Getter
    @Builder
    public static class CosmeticDetail {
        private Long cosmeticId;
        private String cosmeticBrand;
        private String cosmeticName;
        private String cosmeticType;
        private String coreIngredients;
        private String cosmeticUrl;
        private LocalDate registeredDate;

        public static CosmeticDetail from(RegisteredCosmetic cosmetic) {
            return CosmeticDetail.builder()
                    .cosmeticId(cosmetic.getId())
                    .cosmeticBrand(cosmetic.getCosmeticBrand())
                    .cosmeticName(cosmetic.getCosmeticName())
                    .cosmeticType(cosmetic.getCosmeticType().name())
                    .coreIngredients(cosmetic.getCoreIngredients())
                    .cosmeticUrl(cosmetic.getCosmeticUrl())
                    .registeredDate(cosmetic.getCreatedAt().toLocalDate())
                    .build();
        }
    }
}