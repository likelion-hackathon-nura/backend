package org.example.nura.domain.user.entity;

import org.example.nura.domain.user.entity.enums.MealPattern;
import org.example.nura.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_email",
                        columnNames = "email"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "nickname", nullable = false, length = 15)
    private String nickname;

    @Column(name = "shift_d_start")
    private LocalTime shiftDStart;

    @Column(name = "shift_d_end")
    private LocalTime shiftDEnd;

    @Column(name = "shift_e_start")
    private LocalTime shiftEStart;

    @Column(name = "shift_e_end")
    private LocalTime shiftEEnd;

    @Column(name = "shift_n_start")
    private LocalTime shiftNStart;

    @Column(name = "shift_n_end")
    private LocalTime shiftNEnd;

    @Column(name = "target_sleep_minutes")
    private Integer targetSleepMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_pattern", length = 30)
    private MealPattern mealPattern;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;

    @Column(name = "my_adjustment", nullable = false)
    private Integer myAdjustment = 0;

    @Column(name = "refresh_adjustment", nullable = false)
    private Integer refreshAdjustment = 0;

    private User(
            String email,
            String passwordHash,
            String nickname
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.onboardingCompleted = false;
    }

    public static User create(
            String email,
            String passwordHash,
            String nickname
    ) {
        return new User(
                email,
                passwordHash,
                nickname
        );
    }

    public void completeOnboarding(
            LocalTime shiftDStart,
            LocalTime shiftDEnd,
            LocalTime shiftEStart,
            LocalTime shiftEEnd,
            LocalTime shiftNStart,
            LocalTime shiftNEnd,
            Integer targetSleepMinutes,
            MealPattern mealPattern
    ) {
        this.shiftDStart = shiftDStart;
        this.shiftDEnd = shiftDEnd;
        this.shiftEStart = shiftEStart;
        this.shiftEEnd = shiftEEnd;
        this.shiftNStart = shiftNStart;
        this.shiftNEnd = shiftNEnd;
        this.targetSleepMinutes = targetSleepMinutes;
        this.mealPattern = mealPattern;
        this.onboardingCompleted = true;
    }

    public void applyScheduleFeedback(
            Integer myDelta,
            Integer refreshDelta
    ) {
        if (myDelta != null) {
            this.myAdjustment =
                    clampAdjustment(
                            getSafeMyAdjustment() + myDelta
                    );
        }

        if (refreshDelta != null) {
            this.refreshAdjustment =
                    clampAdjustment(
                            getSafeRefreshAdjustment() + refreshDelta
                    );
        }
    }

    public void updatePreferences(
            Integer targetSleepMinutes,
            MealPattern mealPattern
    ) {
        if (targetSleepMinutes != null) {
            this.targetSleepMinutes = targetSleepMinutes;
        }

        if (mealPattern != null) {
            this.mealPattern = mealPattern;
        }
    }

    public void updateShiftTimes(
            LocalTime shiftDStart,
            LocalTime shiftDEnd,
            LocalTime shiftEStart,
            LocalTime shiftEEnd,
            LocalTime shiftNStart,
            LocalTime shiftNEnd
    ) {
        this.shiftDStart = shiftDStart;
        this.shiftDEnd = shiftDEnd;
        this.shiftEStart = shiftEStart;
        this.shiftEEnd = shiftEEnd;
        this.shiftNStart = shiftNStart;
        this.shiftNEnd = shiftNEnd;
    }

    public void updateNickname(
            String nickname
    ) {
        this.nickname = nickname;
    }

    public void updatePasswordHash(
            String passwordHash
    ) {
        this.passwordHash = passwordHash;
    }

    private int clampAdjustment(int value) {
        return Math.max(
                -2,
                Math.min(2, value)
        );
    }

    private int getSafeMyAdjustment() {
        return myAdjustment == null ? 0 : myAdjustment;
    }

    private int getSafeRefreshAdjustment() {
        return refreshAdjustment == null ? 0 : refreshAdjustment;
    }
}
