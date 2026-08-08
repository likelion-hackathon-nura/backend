package org.example.nura.domain.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.example.nura.domain.user.entity.User;
import org.example.nura.global.common.BaseTimeEntity;

@Getter
@Entity
@Table(
        name = "notification_setting",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_setting_user",
                        columnNames = "user_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_setting_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_notification_setting_user"
            )
    )
    private User user;

    @Column(name = "use_enabled", nullable = false)
    private boolean useEnabled;

    @Column(name = "push_endpoint", length = 1000)
    private String pushEndpoint;

    @Column(name = "p256dh_key", length = 255)
    private String p256dhKey;

    @Column(name = "auth_key", length = 255)
    private String authKey;

    private NotificationSetting(User user) {
        this.user = user;
        this.useEnabled = false;
    }

    public static NotificationSetting create(User user) {
        return new NotificationSetting(user);
    }

    public void enable() {
        if (!hasPushSubscription()) {
            throw new IllegalStateException(
                    "푸시 구독 정보가 있어야 알림을 활성화할 수 있습니다."
            );
        }

        this.useEnabled = true;
    }

    public void disable() {
        this.useEnabled = false;
    }

    public void updatePushSubscription(
            String pushEndpoint,
            String p256dhKey,
            String authKey
    ) {
        if (isBlank(pushEndpoint)
                || isBlank(p256dhKey)
                || isBlank(authKey)) {
            throw new IllegalArgumentException(
                    "푸시 구독 정보는 모두 입력되어야 합니다."
            );
        }

        this.pushEndpoint = pushEndpoint;
        this.p256dhKey = p256dhKey;
        this.authKey = authKey;
    }

    public void clearPushSubscription() {
        this.pushEndpoint = null;
        this.p256dhKey = null;
        this.authKey = null;
    }

    public boolean hasPushSubscription() {
        return !isBlank(pushEndpoint)
                && !isBlank(p256dhKey)
                && !isBlank(authKey);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
