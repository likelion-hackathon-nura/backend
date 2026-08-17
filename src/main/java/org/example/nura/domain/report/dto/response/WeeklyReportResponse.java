package org.example.nura.domain.report.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class WeeklyReportResponse {

    private AiCommentDto aiComment;
    private RecoveryTrendDto recoveryTrend;
    private ThreeTimeBalanceDto threeTimeBalance;

    @Getter
    @Builder
    public static class AiCommentDto {
        private String headline;
        private String bullet1;
        private String bullet2;
        private String bullet3;
    }

    @Getter
    @Builder
    public static class RecoveryTrendDto {
        private String averageRefreshTimeText;
        private String comparisonText;
        private Integer comparisonMinutes;
        private List<DailyHoursDto> thisWeek;
        private List<DailyHoursDto> lastWeek;  //없으면 nulll
    }

    @Getter
    @Builder
    public static class DailyHoursDto {
        private String dayOfWeek;
        private LocalDate date;
        private Double hours;
    }

    @Getter
    @Builder
    public static class ThreeTimeBalanceDto {
        private String baseDateText;
        private int socialTimePercent;
        private int refreshTimePercent;
        private int myTimePercent;
    }
}