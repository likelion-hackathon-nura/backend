package org.example.nura.domain.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nura.domain.report.dto.response.WeeklyReportResponse;
import org.example.nura.domain.report.repository.WeeklyReportRepository;
import org.example.nura.domain.schedule.entity.DailyTimeAllocation;
import org.example.nura.domain.schedule.entity.DutySchedule;
import org.example.nura.domain.schedule.entity.enums.ShiftType;
import org.example.nura.domain.schedule.repository.DailyTimeAllocationRepository;
import org.example.nura.domain.schedule.repository.DutyScheduleRepository;
import org.example.nura.domain.user.entity.User;
import org.example.nura.domain.user.repository.UserRepository;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final DailyTimeAllocationRepository dailyTimeAllocationRepository;
    private final DutyScheduleRepository dutyScheduleRepository;
    private final UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    @Value("${ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String openAiModel;

    private RestClient openAiClient;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.openAiClient = RestClient.builder()
                .baseUrl(openAiBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 이번 주 리포트 조회 (월요일 기준 주간)
     */
    @Transactional
    public WeeklyReportResponse getWeeklyReport(Long userId, LocalDate targetDate) {
        LocalDate now = (targetDate != null) ? targetDate : LocalDate.now();
        LocalDate weekStartDate = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEndDate = weekStartDate.plusDays(6);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 1. 이번 주 & 지난 주 시간/근무 데이터 집계
        List<DailyTimeAllocation> thisWeekAllocations =
                dailyTimeAllocationRepository.findAllByUserIdAndDateBetweenOrderByDateAsc(userId, weekStartDate, weekEndDate);

        LocalDate lastWeekStart = weekStartDate.minusWeeks(1);
        LocalDate lastWeekEnd = weekEndDate.minusWeeks(1);
        List<DailyTimeAllocation> lastWeekAllocations =
                dailyTimeAllocationRepository.findAllByUserIdAndDateBetweenOrderByDateAsc(userId, lastWeekStart, lastWeekEnd);

        List<DutySchedule> thisWeekSchedules =
                dutyScheduleRepository.findAllByUserIdAndDateBetweenOrderByDateAsc(userId, weekStartDate, weekEndDate);

        int recordedDaysCount = thisWeekAllocations.size();

        // 2. AI 코멘트 생성 (콜드 스타트 분기)
        WeeklyReportResponse.AiCommentDto aiComment = resolveAiComment(user, weekStartDate, weekEndDate, recordedDaysCount, thisWeekAllocations, thisWeekSchedules);

        // 3. 회복 추세 그래프 데이터 계산
        WeeklyReportResponse.RecoveryTrendDto recoveryTrend = buildRecoveryTrend(
                weekStartDate, thisWeekAllocations, lastWeekAllocations
        );

        // 4. 3-Time 밸런스 비율 계산
        WeeklyReportResponse.ThreeTimeBalanceDto balance = buildThreeTimeBalance(now, thisWeekAllocations);

        return WeeklyReportResponse.builder()
                .aiComment(aiComment)
                .recoveryTrend(recoveryTrend)
                .threeTimeBalance(balance)
                .build();
    }

    /**
     * 기록 일수에 따른 AI 코멘트 분기 처리
     */
    private WeeklyReportResponse.AiCommentDto resolveAiComment(
            User user,
            LocalDate weekStart,
            LocalDate weekEnd,
            int recordedDays,
            List<DailyTimeAllocation> allocations,
            List<DutySchedule> schedules
    ) {
        // [1단계: 0~2일차] 고정 웰컴 멘트 (LLM 미호출로 비용 절감 및 환각 방지)
        if (recordedDays < 3) {
            return WeeklyReportResponse.AiCommentDto.builder()
                    .headline("반가워요! NURA와 함께 피부 회복 여정을 시작해보세요.")
                    .bullet1("이번 주는 기록을 시작하는 단계예요.")
                    .bullet2("최소 3일 이상 체크인을 기록하시면 AI가 주간 라이프 패턴을 분석해 드려요.")
                    .bullet3("오늘의 피부 상태와 근무 형태를 홈 탭에서 꾸준히 기록해 보세요!")
                    .build();
        }

        // [2~3단계: 3일 이상] OpenAI 호출
        return generateAiCommentByLlm(recordedDays, allocations, schedules);
    }

    private WeeklyReportResponse.AiCommentDto generateAiCommentByLlm(
            int recordedDays,
            List<DailyTimeAllocation> allocations,
            List<DutySchedule> schedules
    ) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return fallbackAiComment();
        }

        try {
            // 주간 집계 통계 추출
            long nightShiftCount = schedules.stream()
                    .filter(s -> s.getShiftType() == ShiftType.N)
                    .count();

            int avgRefreshMinutes = (int) allocations.stream()
                    .mapToInt(DailyTimeAllocation::getRefreshTime)
                    .average()
                    .orElse(0);

            long totalSocial = allocations.stream().mapToInt(DailyTimeAllocation::getSocialTime).sum();
            long totalRefresh = allocations.stream().mapToInt(DailyTimeAllocation::getRefreshTime).sum();
            long totalMy = allocations.stream().mapToInt(DailyTimeAllocation::getMyTime).sum();
            long grandTotal = Math.max(1, totalSocial + totalRefresh + totalMy);

            int socialPct = (int) Math.round((double) totalSocial * 100 / grandTotal);
            int refreshPct = (int) Math.round((double) totalRefresh * 100 / grandTotal);
            int myPct = (int) Math.round((double) totalMy * 100 / grandTotal);

            String prompt = String.format("""
                    유저의 이번 주 주간 데이터:
                    - 수집된 기록 일수: %d일/7일
                    - Night 근무 횟수: %d회
                    - 일평균 Refresh Time: %d분 (%d시간 %d분)
                    - 3-Time 비율: Social %d%%, Refresh %d%%, My %d%%
                    
                    다음 JSON 포맷으로 코멘트를 작성하세요.
                    {"headline":"...", "bullet_1":"...", "bullet_2":"...", "bullet_3":"..."}
                    
                    작성 규칙:
                    1. headline: 이번 주 웰니스/시간 배분에 대한 핵심 피드백 1문장 (25자 내외).
                    2. bullet_1: 근무 형태나 일정 패턴 분석 (1문장).
                    3. bullet_2: 컨디션 및 리프레시 관리 상태 (1문장).
                    4. bullet_3: 3-Time 밸런스에 따른 격려 및 다음 주 가이드 (1문장).
                    3~6일 기록인 경우 남은 주간 완주를 유연하게 독려하세요.
                    """,
                    recordedDays, nightShiftCount, avgRefreshMinutes,
                    avgRefreshMinutes / 60, avgRefreshMinutes % 60,
                    socialPct, refreshPct, myPct
            );

            Map<String, Object> payload = Map.of(
                    "model", openAiModel,
                    "temperature", 0.4,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", "너는 직장인을 위한 웰니스 & 피부 회복 AI 코치 NURA다. 친절하고 전문적인 톤의 한국어로 답하라."),
                            Map.of("role", "user", "content", prompt)
                    )
            );

            String responseBody = openAiClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            if (responseBody != null && !responseBody.isBlank()) {
                JsonNode root = objectMapper.readTree(responseBody);
                String content = root.path("choices").path(0).path("message").path("content").asText("").trim();
                if (!content.isBlank()) {
                    JsonNode jsonNode = objectMapper.readTree(content);
                    return WeeklyReportResponse.AiCommentDto.builder()
                            .headline(jsonNode.path("headline").asText("이번 주 회복 리프레시를 잘 유지하셨어요."))
                            .bullet1(jsonNode.path("bullet_1").asText("근무 일정과 회복 간격을 고려하여 시간을 배치했어요."))
                            .bullet2(jsonNode.path("bullet_2").asText("체크인 기록을 바탕으로 피부 휴식 시간을 확보했어요."))
                            .bullet3(jsonNode.path("bullet_3").asText("다음 주에도 균형 잡힌 리프레시 타임을 이어가 보세요!"))
                            .build();
                }
            }
        } catch (Exception e) {
            log.warn("[WeeklyReport] AI 코멘트 생성 실패: {}", e.getMessage());
        }

        return fallbackAiComment();
    }

    private WeeklyReportResponse.AiCommentDto fallbackAiComment() {
        return WeeklyReportResponse.AiCommentDto.builder()
                .headline("이번 주 Refresh Time을 우선 고려하여 라이프를 배치했어요.")
                .bullet1("근무 간격과 컨디션 변화를 지속적으로 체크하고 있어요.")
                .bullet2("피부 피로도가 쌓이지 않도록 리프레시 시간을 적극 활용하세요.")
                .bullet3("나만을 위한 시간과 휴식의 밸런스를 차근차근 맞춰가 보세요.")
                .build();
    }

    /**
     * 회복 추세 그래프 데이터 생성
     */
    private WeeklyReportResponse.RecoveryTrendDto buildRecoveryTrend(
            LocalDate weekStart,
            List<DailyTimeAllocation> thisWeek,
            List<DailyTimeAllocation> lastWeek
    ) {
        List<WeeklyReportResponse.DailyHoursDto> thisWeekList = new ArrayList<>();
        List<WeeklyReportResponse.DailyHoursDto> lastWeekList = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            String dayName = getDayName(date.getDayOfWeek());

            Double thisHours = thisWeek.stream()
                    .filter(a -> a.getDate().equals(date))
                    .findFirst()
                    .map(a -> Math.round((a.getRefreshTime() / 60.0) * 10.0) / 10.0)
                    .orElse(null);
            thisWeekList.add(WeeklyReportResponse.DailyHoursDto.builder()
                    .dayOfWeek(dayName).date(date).hours(thisHours).build());

            LocalDate lastDate = date.minusWeeks(1);
            Double lastHours = lastWeek.stream()
                    .filter(a -> a.getDate().equals(lastDate))
                    .findFirst()
                    .map(a -> Math.round((a.getRefreshTime() / 60.0) * 10.0) / 10.0)
                    .orElse(null);
            lastWeekList.add(WeeklyReportResponse.DailyHoursDto.builder()
                    .dayOfWeek(dayName).date(lastDate).hours(lastHours).build());
        }

        // 평균 계산
        double thisAvgMin = thisWeek.stream().mapToInt(DailyTimeAllocation::getRefreshTime).average().orElse(0.0);
        int avgMinutesInt = (int) Math.round(thisAvgMin);
        String avgText = String.format("%d시간 %d분", avgMinutesInt / 60, avgMinutesInt % 60);

        String compText;
        Integer compMin = null;

        if (lastWeek.isEmpty()) {
            compText = "첫 주 기록을 시작했어요!";
        } else {
            double lastAvgMin = lastWeek.stream().mapToInt(DailyTimeAllocation::getRefreshTime).average().orElse(0.0);
            int diff = avgMinutesInt - (int) Math.round(lastAvgMin);
            compMin = diff;
            if (diff >= 0) {
                compText = String.format("지난주보다 %d분 더 늘었어요!", diff);
            } else {
                compText = String.format("지난주보다 %d분 줄었어요.", Math.abs(diff));
            }
        }

        return WeeklyReportResponse.RecoveryTrendDto.builder()
                .averageRefreshTimeText(avgText)
                .comparisonText(compText)
                .comparisonMinutes(compMin)
                .thisWeek(thisWeekList)
                .lastWeek(lastWeek.isEmpty() ? null : lastWeekList)
                .build();
    }

    /**
     * 3-Time 밸런스 비율 계산
     */
    private WeeklyReportResponse.ThreeTimeBalanceDto buildThreeTimeBalance(
            LocalDate baseDate,
            List<DailyTimeAllocation> thisWeek
    ) {
        if (thisWeek.isEmpty()) {
            return WeeklyReportResponse.ThreeTimeBalanceDto.builder()
                    .baseDateText(baseDate.format(DateTimeFormatter.ofPattern("M.d일 기준")))
                    .socialTimePercent(0)
                    .refreshTimePercent(0)
                    .myTimePercent(0)
                    .build();
        }

        long totalSocial = thisWeek.stream().mapToInt(DailyTimeAllocation::getSocialTime).sum();
        long totalRefresh = thisWeek.stream().mapToInt(DailyTimeAllocation::getRefreshTime).sum();
        long totalMy = thisWeek.stream().mapToInt(DailyTimeAllocation::getMyTime).sum();
        long grandTotal = Math.max(1, totalSocial + totalRefresh + totalMy);

        int socialPct = (int) Math.round((double) totalSocial * 100 / grandTotal);
        int refreshPct = (int) Math.round((double) totalRefresh * 100 / grandTotal);
        int myPct = 100 - (socialPct + refreshPct);

        return WeeklyReportResponse.ThreeTimeBalanceDto.builder()
                .baseDateText(baseDate.format(DateTimeFormatter.ofPattern("M.d일 기준")))
                .socialTimePercent(socialPct)
                .refreshTimePercent(refreshPct)
                .myTimePercent(myPct)
                .build();
    }

    private String getDayName(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }
}