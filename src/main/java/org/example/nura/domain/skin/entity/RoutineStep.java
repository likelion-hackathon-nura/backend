package org.example.nura.domain.skin.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.nura.domain.cosmetics.entity.RegisteredCosmetic;
import org.example.nura.domain.skin.entity.enums.SkinCareType;
import org.example.nura.global.common.BaseTimeEntity;

@Getter
@Entity
@Table(
        name = "routine_step",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_routine_step_order",
                        columnNames = {
                                "routine_id",
                                "step_order"
                        }
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineStep extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "routine_step_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "routine_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_routine_step_skin_routine"
            )
    )
    private SkinRoutine routine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "registered_cosmetic_id",
            nullable = true,
            foreignKey = @ForeignKey(
                    name = "fk_routine_step_registered_cosmetic"
            )
    )
    private RegisteredCosmetic registeredCosmetic;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "care_type", nullable = false, length = 20)
    private SkinCareType careType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "precautions", columnDefinition = "TEXT")
    private String precautions;

    @Column(
            name = "recommended_ingredients",
            columnDefinition = "JSON"
    )
    private String recommendedIngredients;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    private RoutineStep(
            SkinRoutine routine,
            RegisteredCosmetic registeredCosmetic,
            Integer stepOrder,
            SkinCareType careType,
            String title,
            String description,
            String precautions,
            String recommendedIngredients,
            String reason
    ) {
        this.routine = routine;
        this.registeredCosmetic = registeredCosmetic;
        this.stepOrder = stepOrder;
        this.careType = careType;
        this.title = title;
        this.description = description;
        this.precautions = precautions;
        this.recommendedIngredients = recommendedIngredients;
        this.reason = reason;
    }

    public static RoutineStep create(
            SkinRoutine routine,
            RegisteredCosmetic registeredCosmetic,
            Integer stepOrder,
            SkinCareType careType,
            String title,
            String description,
            String precautions,
            String recommendedIngredients,
            String reason
    ) {
        return new RoutineStep(
                routine,
                registeredCosmetic,
                stepOrder,
                careType,
                title,
                description,
                precautions,
                recommendedIngredients,
                reason
        );
    }
}
