package org.example.nura.domain.skin.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CosmeticType {

    SERUM("세럼"),
    CREAM("크림"),
    TONER("토너"),
    LOTION("로션"),
    CLEANSER("클렌저"),
    SUNSCREEN("선크림"),
    OIL("오일"),
    ETC("기타");

    private final String description;
}