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
import org.example.nura.domain.skin.entity.enums.SkinAnalysisLevel;
import org.example.nura.domain.user.entity.User;
import org.example.nura.global.common.BaseTimeEntity;

import java.time.LocalDate;

@Getter
@Entity
@Table(
        name = "checkin",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_checkin_user_date",
                        columnNames = {"user_id", "date"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Checkin extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "checkin_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_checkin_user"
            )
    )
    private User user;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "fatigue", nullable = false)
    private Integer fatigue;

    @Column(name = "tightness", nullable = false)
    private Integer tightness;

    @Column(name = "redness", nullable = false)
    private Integer redness;

    @Enumerated(EnumType.STRING)
    @Column(name = "analyzed_redness", length = 10)
    private SkinAnalysisLevel analyzedRedness;

    @Enumerated(EnumType.STRING)
    @Column(name = "analyzed_moisture", length = 10)
    private SkinAnalysisLevel analyzedMoisture;

    @Enumerated(EnumType.STRING)
    @Column(name = "analyzed_oiliness", length = 10)
    private SkinAnalysisLevel analyzedOiliness;

    @Enumerated(EnumType.STRING)
    @Column(name = "analyzed_trouble", length = 10)
    private SkinAnalysisLevel analyzedTrouble;

    @Column(name = "ai_comment", columnDefinition = "TEXT")
    private String aiComment;

    private Checkin(
            User user,
            LocalDate date,
            Integer fatigue,
            Integer tightness,
            Integer redness
    ) {
        this.user = user;
        this.date = date;
        this.fatigue = fatigue;
        this.tightness = tightness;
        this.redness = redness;
    }

    public static Checkin create(
            User user,
            LocalDate date,
            Integer fatigue,
            Integer tightness,
            Integer redness
    ) {
        return new Checkin(
                user,
                date,
                fatigue,
                tightness,
                redness
        );
    }

    public void updateAnalysis(
            SkinAnalysisLevel analyzedRedness,
            SkinAnalysisLevel analyzedMoisture,
            SkinAnalysisLevel analyzedOiliness,
            SkinAnalysisLevel analyzedTrouble,
            String aiComment
    ) {
        this.analyzedRedness = analyzedRedness;
        this.analyzedMoisture = analyzedMoisture;
        this.analyzedOiliness = analyzedOiliness;
        this.analyzedTrouble = analyzedTrouble;
        this.aiComment = aiComment;
    }
}
