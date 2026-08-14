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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.nura.domain.skin.entity.enums.RecoveryLevel;
import org.example.nura.global.common.BaseTimeEntity;

@Getter
@Entity
@Table(
        name = "skin_routine",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_skin_routine_checkin",
                        columnNames = "checkin_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SkinRoutine extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "routine_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "checkin_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_skin_routine_checkin"
            )
    )
    private Checkin checkin;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "recovery_level",
            nullable = false,
            length = 10
    )
    private RecoveryLevel recoveryLevel;

    @Column(name = "completed", nullable = false)
    private boolean completed = false;

    private SkinRoutine(
            Checkin checkin,
            RecoveryLevel recoveryLevel
    ) {
        this.checkin = checkin;
        this.recoveryLevel = recoveryLevel;
        this.completed = false;
    }

    public static SkinRoutine create(
            Checkin checkin,
            RecoveryLevel recoveryLevel
    ) {
        return new SkinRoutine(
                checkin,
                recoveryLevel
        );
    }

    public void complete() {
        this.completed = true;
    }
}
