package org.example.nura.domain.user.entity;

import org.example.nura.domain.user.entity.enums.RestActivityType;
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
        name = "user_rest_activity",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_rest_activity_user_type",
                        columnNames = {"user_id", "activity_type"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRestActivity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_rest_activity_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_user_rest_activity_user"
            )
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "activity_type",
            nullable = false,
            length = 30
    )
    private RestActivityType activityType;

    private UserRestActivity(
            User user,
            RestActivityType activityType
    ) {
        this.user = user;
        this.activityType = activityType;
    }

    public static UserRestActivity create(
            User user,
            RestActivityType activityType
    ) {
        return new UserRestActivity(
                user,
                activityType
        );
    }
}