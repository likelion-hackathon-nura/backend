package org.example.nura.domain.skin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nura.domain.skin.dto.response.SkinRoutineResponse;
import org.example.nura.domain.skin.dto.response.SkinRoutineStepResponse;
import org.example.nura.domain.skin.entity.Checkin;
import org.example.nura.domain.skin.entity.RegisteredCosmetic;
import org.example.nura.domain.skin.entity.RoutineStep;
import org.example.nura.domain.skin.entity.SkinRoutine;
import org.example.nura.domain.skin.entity.enums.CosmeticType;
import org.example.nura.domain.skin.entity.enums.RecoveryLevel;
import org.example.nura.domain.skin.entity.enums.SkinAnalysisLevel;
import org.example.nura.domain.skin.entity.enums.SkinCareType;
import org.example.nura.domain.skin.repository.RegisteredCosmeticRepository;
import org.example.nura.domain.skin.repository.RoutineStepRepository;
import org.example.nura.domain.skin.repository.SkinRoutineRepository;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkinRoutineService {

    private final SkinRoutineRepository skinRoutineRepository;
    private final RoutineStepRepository routineStepRepository;
    private final RegisteredCosmeticRepository registeredCosmeticRepository;
    private final TransactionTemplate transactionTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    @Value("${ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String openAiModel;

    private RestClient openAiClient;

    /**
     * 타임아웃을 설정한 RestClient 1회 생성
     */
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
     * 오늘 루틴 생성 (외부 AI 호출 시 DB 트랜잭션을 잡고 있지 않도록 비트랜잭션으로 처리)
     */
    public SkinRoutineResponse generateTodayRoutine(Long userId) {
        SkinRoutine routine = findTodayRoutineEntity(userId);
        Checkin checkin = routine.getCheckin();

        List<RegisteredCosmetic> cosmetics =
                registeredCosmeticRepository.findByUserId(userId);

        int stepCount = resolveStepCount(routine.getRecoveryLevel());
        List<SkinCareType> careTypes = suggestCareTypes(checkin, stepCount);

        List<StepSaveDto> stepDtos = new ArrayList<>();

        // 1. 외부 AI API 호출 (DB 트랜잭션 바깥)
        for (int i = 0; i < stepCount; i++) {
            int stepOrder = i + 1;
            SkinCareType careType = careTypes.get(i);
            RegisteredCosmetic cosmetic = pickCosmetic(cosmetics, careType);

            LlmStepContent content = generateStepContent(
                    checkin,
                    careType,
                    cosmetic,
                    stepOrder
            );

            stepDtos.add(new StepSaveDto(stepOrder, careType, cosmetic, content));
        }

        // 2. DB 저장 (TransactionTemplate을 사용하여 쓰기 트랜잭션 수행)
        return transactionTemplate.execute(status ->
                saveRoutineStepsTransaction(routine, stepDtos)
        );
    }

    /**
     * DB 저장 및 기존 스텝 갱신 로직 (TransactionTemplate 내부에서 호출됨)
     */
    public SkinRoutineResponse saveRoutineStepsTransaction(
            SkinRoutine routine,
            List<StepSaveDto> stepDtos
    ) {
        routineStepRepository.deleteAllByRoutineId(routine.getId());

        List<RoutineStep> steps = stepDtos.stream()
                .map(dto -> RoutineStep.create(
                        routine,
                        dto.cosmetic(),
                        dto.stepOrder(),
                        dto.careType(),
                        dto.content().title(),
                        dto.content().description(),
                        dto.content().precautions(),
                        formatToJsonArray(dto.content().recommendedIngredients()), // 👈 이 부분 수정!
                        dto.content().reason()
                ))
                .toList();

        routineStepRepository.saveAll(steps);

        return toResponse(routine, steps);
    }

    /**
     * 일반 문자열을 MySQL JSON 컬럼 규격에 맞는 JSON Array 문자열로 변환 (예: ["성분1", "성분2"])
     */
    private String formatToJsonArray(String rawIngredients) {
        if (rawIngredients == null || rawIngredients.isBlank()) {
            return "[\"기본 보습 성분\"]";
        }

        String trimmed = rawIngredients.trim();
        // 이미 JSON 배열 형태([ ... ])인 경우 그대로 반환
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return trimmed;
        }

        // 콤마(,)나 슬래시(/)로 분리하여 JSON 배열 형태로 변환
        String[] items = trimmed.split("[,/]");
        List<String> list = new ArrayList<>();
        for (String item : items) {
            if (!item.isBlank()) {
                list.add(item.trim());
            }
        }

        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[\"" + trimmed.replace("\"", "\\\"") + "\"]";
        }
    }

    @Transactional(readOnly = true)
    public SkinRoutineResponse getTodayRoutine(Long userId) {
        SkinRoutine routine = findTodayRoutineEntity(userId);
        List<RoutineStep> steps =
                routineStepRepository.findAllByRoutineIdOrderByStepOrderAsc(
                        routine.getId()
                );
        return toResponse(routine, steps);
    }

    @Transactional
    public SkinRoutineResponse completeTodayRoutine(Long userId) {
        SkinRoutine routine = findTodayRoutineEntity(userId);

        if (!routine.isCompleted()) {
            routine.complete();
        }

        List<RoutineStep> steps =
                routineStepRepository.findAllByRoutineIdOrderByStepOrderAsc(
                        routine.getId()
                );

        return toResponse(routine, steps);
    }

    private SkinRoutine findTodayRoutineEntity(Long userId) {
        return skinRoutineRepository.findByCheckinUserIdAndCheckinDate(
                        userId,
                        LocalDate.now()
                )
                .orElseThrow(() ->
                        new BaseException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "오늘 생성된 회복 루틴이 없습니다. 먼저 체크인을 완료해주세요."
                        )
                );
    }

    private int resolveStepCount(RecoveryLevel recoveryLevel) {
        return switch (recoveryLevel) {
            case LEVEL_1 -> 1;
            case LEVEL_2 -> 2;
            case LEVEL_3 -> 3;
        };
    }

    private List<SkinCareType> suggestCareTypes(
            Checkin checkin,
            int stepCount
    ) {
        List<SkinCareType> careTypes = new ArrayList<>();

        if (isHigh(checkin.getAnalyzedTrouble())
                || isHigh(checkin.getAnalyzedRedness())) {
            careTypes.add(SkinCareType.SOOTHING);
            careTypes.add(SkinCareType.TROUBLE_CARE);
        }

        if (isLow(checkin.getAnalyzedMoisture())) {
            careTypes.add(SkinCareType.HYDRATION);
            careTypes.add(SkinCareType.BARRIER_CARE);
        }

        if (isHigh(checkin.getAnalyzedOiliness())) {
            careTypes.add(SkinCareType.OIL_CONTROL);
        }

        careTypes.add(SkinCareType.MOISTURIZING);

        while (careTypes.size() < stepCount) {
            careTypes.add(SkinCareType.SOOTHING);
        }
        return careTypes.subList(0, stepCount);
    }

    private RegisteredCosmetic pickCosmetic(
            List<RegisteredCosmetic> cosmetics,
            SkinCareType careType
    ) {
        if (cosmetics == null || cosmetics.isEmpty()) {
            return null;
        }

        List<CosmeticType> preferredTypes = preferredCosmeticTypes(careType);

        return cosmetics.stream()
                .filter(cosmetic -> preferredTypes.contains(cosmetic.getCosmeticType()))
                .findFirst()
                .orElse(null);
    }

    private List<CosmeticType> preferredCosmeticTypes(SkinCareType careType) {
        return switch (careType) {
            case SOOTHING -> List.of(CosmeticType.TONER, CosmeticType.SERUM, CosmeticType.MASK);
            case HYDRATION -> List.of(CosmeticType.TONER, CosmeticType.LOTION, CosmeticType.SERUM);
            case MOISTURIZING -> List.of(CosmeticType.CREAM, CosmeticType.LOTION);
            case BARRIER_CARE -> List.of(CosmeticType.CREAM, CosmeticType.LOTION, CosmeticType.SERUM);
            case OIL_CONTROL -> List.of(CosmeticType.CLEANSER, CosmeticType.TONER, CosmeticType.SERUM);
            case TROUBLE_CARE -> List.of(CosmeticType.SERUM, CosmeticType.MASK, CosmeticType.TONER);
        };
    }

    private LlmStepContent generateStepContent(
            Checkin checkin,
            SkinCareType careType,
            RegisteredCosmetic cosmetic,
            int stepOrder
    ) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return fallbackStepContent(careType, cosmetic, stepOrder);
        }

        try {
            String cosmeticName = (cosmetic != null) ? cosmetic.getCosmeticName() : "소장 중인 기본 " + careType.name() + " 제품";
            String cosmeticType = (cosmetic != null) ? cosmetic.getCosmeticType().name() : "자유 선택";

            String prompt = "다음 정보를 기반으로 3분 회복 루틴의 한 단계를 JSON으로 작성하세요."
                    + "\n반환 형식: {\"title\":\"...\",\"description\":\"...\",\"precautions\":\"...\",\"recommended_ingredients\":\"...\",\"reason\":\"...\"}"
                    + "\n- step_order: " + stepOrder
                    + "\n- care_type: " + careType
                    + "\n- cosmetic_name: " + cosmeticName
                    + "\n- cosmetic_type: " + cosmeticType
                    + "\n- acne_level: " + safeLevel(checkin.getAnalyzedTrouble())
                    + "\n- redness_level: " + safeLevel(checkin.getAnalyzedRedness())
                    + "\n- moisture_level: " + safeLevel(checkin.getAnalyzedMoisture())
                    + "\n- oiliness_level: " + safeLevel(checkin.getAnalyzedOiliness());

            Map<String, Object> payload = Map.of(
                    "model", openAiModel,
                    "temperature", 0.4,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", "너는 스킨케어 루틴 코치다. 반드시 요청된 JSON 포맷으로만 한국어로 간결하게 답한다."),
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

            if (responseBody == null || responseBody.isBlank()) {
                return fallbackStepContent(careType, cosmetic, stepOrder);
            }

            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText("")
                    .trim();

            if (content.isBlank()) {
                return fallbackStepContent(careType, cosmetic, stepOrder);
            }

            JsonNode contentJson = objectMapper.readTree(content);
            String defaultIngredients = (cosmetic != null) ? cosmetic.getCoreIngredients() : "진정/보습 관련 성분";

            return new LlmStepContent(
                    readOrDefault(contentJson, "title", fallbackTitle(careType, stepOrder)),
                    readOrDefault(contentJson, "description", fallbackDescription(careType, cosmetic)),
                    readOrDefault(contentJson, "precautions", "눈가를 피해 부드럽게 사용해주세요."),
                    readOrDefault(contentJson, "recommended_ingredients", defaultIngredients),
                    readOrDefault(contentJson, "reason", fallbackReason(careType))
            );
        } catch (Exception e) {
            log.warn("[SkinRoutine] OpenAI 루틴 스텝 생성 실패: {}", e.getMessage());
            return fallbackStepContent(careType, cosmetic, stepOrder);
        }
    }

    private String readOrDefault(
            JsonNode root,
            String key,
            String defaultValue
    ) {
        String value = root.path(key).asText("").trim();
        return value.isBlank() ? defaultValue : value;
    }

    private LlmStepContent fallbackStepContent(
            SkinCareType careType,
            RegisteredCosmetic cosmetic,
            int stepOrder
    ) {
        String ingredients = (cosmetic != null) ? cosmetic.getCoreIngredients() : "수분 및 진정 성분";

        return new LlmStepContent(
                fallbackTitle(careType, stepOrder),
                fallbackDescription(careType, cosmetic),
                "눈가를 피해 자극 없이 사용해주세요.",
                ingredients,
                fallbackReason(careType)
        );
    }

    private String fallbackTitle(
            SkinCareType careType,
            int stepOrder
    ) {
        return stepOrder + "단계 " + careType.name();
    }

    private String fallbackDescription(
            SkinCareType careType,
            RegisteredCosmetic cosmetic
    ) {
        if (cosmetic != null) {
            return cosmetic.getCosmeticName() + "로 " + careType.name() + " 중심 케어를 진행합니다.";
        }
        return careType.name() + " 효과가 있는 가벼운 기본 화장품을 사용해 케어해 주세요.";
    }

    private String fallbackReason(SkinCareType careType) {
        return careType.name() + "가 필요한 피부 상태로 분석되었습니다.";
    }

    private boolean isHigh(SkinAnalysisLevel level) {
        return level == SkinAnalysisLevel.HIGH;
    }

    private boolean isLow(SkinAnalysisLevel level) {
        return level == SkinAnalysisLevel.LOW;
    }

    private SkinAnalysisLevel safeLevel(SkinAnalysisLevel level) {
        return level == null ? SkinAnalysisLevel.UNKNOWN : level;
    }

    private SkinRoutineResponse toResponse(
            SkinRoutine routine,
            List<RoutineStep> steps
    ) {
        List<SkinRoutineStepResponse> stepResponses = steps.stream()
                .map(step -> {
                    RegisteredCosmetic cosmetic = step.getRegisteredCosmetic();

                    return new SkinRoutineStepResponse(
                            step.getStepOrder(),
                            step.getCareType(),
                            step.getTitle(),
                            step.getDescription(),
                            step.getPrecautions(),
                            step.getRecommendedIngredients(),
                            step.getReason(),
                            cosmetic != null ? cosmetic.getId() : null,
                            cosmetic != null ? cosmetic.getCosmeticBrand() : null,
                            cosmetic != null ? cosmetic.getCosmeticName() : "추천 제품 사용",
                            cosmetic != null ? cosmetic.getCosmeticType() : null,
                            cosmetic != null ? cosmetic.getCosmeticUrl() : null
                    );
                })
                .toList();

        return new SkinRoutineResponse(
                routine.getId(),
                routine.getCheckin().getId(),
                routine.getCheckin().getDate(),
                routine.getRecoveryLevel(),
                routine.isCompleted(),
                stepResponses,
                routine.getCreatedAt()
        );
    }

    private record LlmStepContent(
            String title,
            String description,
            String precautions,
            String recommendedIngredients,
            String reason
    ) {
    }

    private record StepSaveDto(
            int stepOrder,
            SkinCareType careType,
            RegisteredCosmetic cosmetic,
            LlmStepContent content
    ) {
    }
}