package org.example.nura.domain.user.entity;

import org.example.nura.domain.user.entity.enums.SkinSensitivityLevel;
import org.example.nura.domain.user.entity.enums.SkinType;
import org.example.nura.global.common.BaseTimeEntity;
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

@Getter
@Entity
@Table(
        name = "user_skin",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_skin_user",
                        columnNames = "user_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSkin extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_skin_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_user_skin_user"
            )
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "sensitivity_level",
            nullable = false,
            length = 10
    )
    private SkinSensitivityLevel sensitivityLevel;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "skin_type",
            nullable = false,
            length = 20
    )
    private SkinType skinType;

    private UserSkin(
            User user,
            SkinSensitivityLevel sensitivityLevel,
            SkinType skinType
    ) {
        this.user = user;
        this.sensitivityLevel = sensitivityLevel;
        this.skinType = skinType;
    }

    public static UserSkin create(
            User user,
            SkinSensitivityLevel sensitivityLevel,
            SkinType skinType
    ) {
        return new UserSkin(
                user,
                sensitivityLevel,
                skinType
        );
    }

    public void update(
            SkinSensitivityLevel sensitivityLevel,
            SkinType skinType
    ) {
        if (sensitivityLevel != null) {
            this.sensitivityLevel = sensitivityLevel;
        }

        if (skinType != null) {
            this.skinType = skinType;
        }
    }
}
