package org.example.nura.domain.user.entity;

import org.example.nura.domain.user.entity.enums.SkinConcernType;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "user_skin_concern",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_skin_concern_skin_type",
                        columnNames = {
                                "user_skin_id",
                                "concern_type"
                        }
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSkinConcern extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_skin_concern_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_skin_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_user_skin_concern_user_skin"
            )
    )
    private UserSkin userSkin;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "concern_type",
            nullable = false,
            length = 30
    )
    private SkinConcernType concernType;

    private UserSkinConcern(
            UserSkin userSkin,
            SkinConcernType concernType
    ) {
        this.userSkin = userSkin;
        this.concernType = concernType;
    }

    public static UserSkinConcern create(
            UserSkin userSkin,
            SkinConcernType concernType
    ) {
        return new UserSkinConcern(
                userSkin,
                concernType
        );
    }
}
