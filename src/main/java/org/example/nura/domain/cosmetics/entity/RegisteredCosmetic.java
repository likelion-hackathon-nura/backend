package org.example.nura.domain.cosmetics.entity
;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.nura.domain.cosmetics.entity.enums.CosmeticType;
import org.example.nura.domain.user.entity.User;
import org.example.nura.global.common.BaseTimeEntity;

@Getter
@Entity
@Table(name = "registered_cosmetic")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegisteredCosmetic extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registered_cosmetic_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_registered_cosmetic_user"
            )
    )
    private User user;

    @Column(name = "cosmetic_brand", length = 50)
    private String cosmeticBrand;

    @Column(name = "cosmetic_name", nullable = false, length = 100)
    private String cosmeticName;

    @Enumerated(EnumType.STRING)
    @Column(name = "cosmetic_type", nullable = false, length = 20)
    private CosmeticType cosmeticType;

    @Column(
            name = "cosmetic_ingredients",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String cosmeticIngredients;

    @Column(
            name = "core_ingredients",
            columnDefinition = "TEXT"
    )
    private String coreIngredients;

    @Column(name = "cosmetic_url", length = 500)
    private String cosmeticUrl;

    private RegisteredCosmetic(
            User user,
            String cosmeticBrand,
            String cosmeticName,
            CosmeticType cosmeticType,
            String cosmeticIngredients,
            String coreIngredients,
            String cosmeticUrl
    ) {
        this.user = user;
        this.cosmeticBrand = cosmeticBrand;
        this.cosmeticName = cosmeticName;
        this.cosmeticType = cosmeticType;
        this.cosmeticIngredients = cosmeticIngredients;
        this.coreIngredients = coreIngredients;
        this.cosmeticUrl = cosmeticUrl;
    }

    public static RegisteredCosmetic create(
            User user,
            String cosmeticBrand,
            String cosmeticName,
            CosmeticType cosmeticType,
            String cosmeticIngredients,
            String coreIngredients,
            String cosmeticUrl
    ) {
        return new RegisteredCosmetic(
                user,
                cosmeticBrand,
                cosmeticName,
                cosmeticType,
                cosmeticIngredients,
                coreIngredients,
                cosmeticUrl
        );
    }

    public void update(
            String cosmeticBrand,
            String cosmeticName,
            CosmeticType cosmeticType,
            String cosmeticIngredients,
            String coreIngredients,
            String cosmeticUrl
    ) {
        this.cosmeticBrand = cosmeticBrand;
        this.cosmeticName = cosmeticName;
        this.cosmeticType = cosmeticType;
        this.cosmeticIngredients = cosmeticIngredients;
        this.coreIngredients = coreIngredients;
        this.cosmeticUrl = cosmeticUrl;
    }
}
