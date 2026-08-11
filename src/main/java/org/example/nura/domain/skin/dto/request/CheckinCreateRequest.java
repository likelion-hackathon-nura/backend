package org.example.nura.domain.skin.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.example.nura.domain.skin.entity.enums.CheckinSkinLevel;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public record CheckinCreateRequest(

        @NotNull
        @PastOrPresent(message = "체크인 날짜는 오늘 이전이어야 합니다.")
        LocalDate date,

        @NotNull
        @Min(value = 1, message = "피로도는 1~5 사이로 입력해주세요.")
        @Max(value = 5, message = "피로도는 1~5 사이로 입력해주세요.")
        Integer fatigue,

        @NotNull
        CheckinSkinLevel tightness,

        @NotNull
        CheckinSkinLevel redness,

        MultipartFile photo
) {
}