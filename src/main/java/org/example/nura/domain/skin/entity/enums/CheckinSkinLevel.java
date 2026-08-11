package org.example.nura.domain.skin.entity.enums;

public enum CheckinSkinLevel {
    VERY_LOW,
    LOW,
    MODERATE,
    HIGH;

    public int toScore() {
        return switch (this) {
            case VERY_LOW -> 1;
            case LOW -> 2;
            case MODERATE -> 3;
            case HIGH -> 4;
        };
    }

    public static CheckinSkinLevel fromScore(Integer score) {
        if (score == null) {
            return null;
        }

        return switch (score) {
            case 1 -> VERY_LOW;
            case 2 -> LOW;
            case 3 -> MODERATE;
            case 4 -> HIGH;
            default -> throw new IllegalArgumentException(
                    "체크인 피부 단계 값이 유효하지 않습니다: " + score
            );
        };
    }
}

