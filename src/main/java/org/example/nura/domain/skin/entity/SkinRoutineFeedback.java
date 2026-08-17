package org.example.nura.domain.skin.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.nura.domain.user.entity.User;
import org.example.nura.global.common.BaseTimeEntity;

@Getter
@Entity
@Table(name = "skin_routine_feedbacks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SkinRoutineFeedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skin_routine_feedback_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "contents", nullable = false, length = 500)
    private String contents;

    private SkinRoutineFeedback(User user, String contents) {
        this.user = user;
        this.contents = contents;
    }

    public static SkinRoutineFeedback create(User user, String contents) {
        return new SkinRoutineFeedback(user, contents);
    }
}