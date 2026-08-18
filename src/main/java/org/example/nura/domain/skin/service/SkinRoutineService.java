package org.example.nura.domain.skin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nura.domain.cosmetics.entity.RegisteredCosmetic;
import org.example.nura.domain.cosmetics.entity.enums.CosmeticType;
import org.example.nura.domain.cosmetics.repository.RegisteredCosmeticRepository;
import org.example.nura.domain.skin.dto.response.SkinRoutineResponse;
import org.example.nura.domain.skin.dto.response.SkinRoutineStepResponse;
import org.example.nura.domain.skin.entity.Checkin;
import org.example.nura.domain.skin.entity.RoutineStep;
import org.example.nura.domain.skin.entity.SkinRoutine;
import org.example.nura.domain.skin.entity.SkinRoutineFeedback;
import org.example.nura.domain.skin.entity.enums.RecoveryLevel;
import org.example.nura.domain.skin.entity.enums.SkinAnalysisLevel;
import org.example.nura.domain.skin.entity.enums.SkinCareType;
import org.example.nura.domain.skin.repository.RoutineStepRepository;
import org.example.nura.domain.skin.repository.SkinRoutineFeedbackRepository;
import org.example.nura.domain.skin.repository.SkinRoutineRepository;
import org.example.nura.domain.user.entity.UserSkin;
import org.example.nura.domain.user.entity.UserSkinConcern;
import org.example.nura.domain.user.repository.UserSkinConcernRepository;
import org.example.nura.domain.user.repository.UserSkinRepository;
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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkinRoutineService {

    private final SkinRoutineRepository skinRoutineRepository;
    private final RoutineStepRepository routineStepRepository;
    private final RegisteredCosmeticRepository registeredCosmeticRepository;
    private final UserSkinRepository userSkinRepository;
    private final UserSkinConcernRepository userSkinConcernRepository;
    private final SkinRoutineFeedbackRepository skinRoutineFeedbackRepository;
    private final TransactionTemplate transactionTemplate;

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

    public SkinRoutineResponse generateTodayRoutine(Long userId) {
        SkinRoutine routine = findTodayRoutineEntity(userId);
        Checkin checkin = routine.getCheckin();

        // 1. 유저 온보딩 체질 프로필 데이터 조회
        UserSkin userSkin = userSkinRepository.findByUserId(userId).orElse(null);
        List<UserSkinConcern> concerns = (userSkin != null)
                ? userSkinConcernRepository.findAllByUserSkinId(userSkin.getId())
                : List.of();

        // 2. 유저의 최신 3분 회복 모드 피드백 3건 조회
        List<SkinRoutineFeedback> recentFeedbacks = skinRoutineFeedbackRepository.findTop3ByUserIdOrderByCreatedAtDesc(userId);

        List<RegisteredCosmetic> cosmetics = registeredCosmeticRepository.findByUserId(userId);

        int stepCount = resolveStepCount(routine.getRecoveryLevel());
        List<SkinCareType> careTypes = suggestCareTypes(checkin, stepCount);

        List<StepSaveItem> stepSaveItems = new ArrayList<>();

        for (int i = 0; i < stepCount; i++) {
            int stepOrder = i + 1;
            SkinCareType careType = careTypes.get(i);
            RegisteredCosmetic cosmetic = pickCosmetic(cosmetics, careType);

            LlmStepContent content = generateStepContent(
                    checkin,
                    userSkin,
                    concerns,
                    recentFeedbacks,
                    careType,
                    cosmetic,
                    stepOrder
            );

            stepSaveItems.add(new StepSaveItem(stepOrder, careType, cosmetic, content));
        }

        return transactionTemplate.execute(status ->
                saveRoutineStepsTransaction(routine, stepSaveItems)
        );
    }

    private SkinRoutineResponse saveRoutineStepsTransaction(
            SkinRoutine routine,
            List<StepSaveItem> stepSaveItems
    ) {
        routineStepRepository.deleteAllByRoutineId(routine.getId());

        List<RoutineStep> steps = stepSaveItems.stream()
                .map(item -> RoutineStep.create(
                        routine,
                        item.cosmetic(),
                        item.stepOrder(),
                        item.careType(),
                        item.content().title(),
                        item.content().description(),
                        item.content().precautions(),
                        formatToJsonArray(item.content().recommendedIngredients()),
                        item.content().reason()
                ))
                .toList();

        routineStepRepository.saveAll(steps);

        return toResponse(routine, steps, stepSaveItems);
    }

    private String formatToJsonArray(String rawIngredients) {
        if (rawIngredients == null || rawIngredients.isBlank()) {
            return "[\"기본 보습 성분\"]";
        }

        String trimmed = rawIngredients.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return trimmed;
        }

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
        return toResponse(routine, steps, null);
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

        return toResponse(routine, steps, null);
    }

    private SkinRoutine findTodayRoutineEntity(Long userId) {
        return skinRoutineRepository.findByCheckinUserIdAndCheckinDate(
                        userId,
                        LocalDate.now(ZoneId.of("Asia/Seoul"))
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

        if (isHigh(checkin.getAnalyzedTrouble()) || isHigh(checkin.getAnalyzedRedness())) {
            careTypes.add(SkinCareType.SOOTHING);
        }

        if (isLow(checkin.getAnalyzedMoisture())) {
            careTypes.add(SkinCareType.HYDRATION);
        }

        if (isHigh(checkin.getAnalyzedOiliness())) {
            careTypes.add(SkinCareType.OIL_CONTROL);
        }

        careTypes.add(SkinCareType.MOISTURIZING);

        if (!careTypes.contains(SkinCareType.SOOTHING)) {
            careTypes.addFirst(SkinCareType.SOOTHING);
        }

        while (careTypes.size() < stepCount) {
            if (!careTypes.contains(SkinCareType.HYDRATION)) {
                careTypes.add(SkinCareType.HYDRATION);
            } else {
                careTypes.add(SkinCareType.MOISTURIZING);
            }
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
            case SOOTHING -> List.of(CosmeticType.TONER, CosmeticType.SERUM, CosmeticType.CREAM);
            case HYDRATION -> List.of(CosmeticType.TONER, CosmeticType.LOTION, CosmeticType.SERUM);
            case MOISTURIZING -> List.of(CosmeticType.CREAM, CosmeticType.LOTION);
            case BARRIER_CARE -> List.of(CosmeticType.CREAM, CosmeticType.LOTION, CosmeticType.SERUM);
            case OIL_CONTROL -> List.of(CosmeticType.CLEANSER, CosmeticType.TONER, CosmeticType.SERUM);
            case TROUBLE_CARE -> List.of(CosmeticType.SERUM, CosmeticType.TONER, CosmeticType.CLEANSER);
        };
    }

    private LlmStepContent generateStepContent(
            Checkin checkin,
            UserSkin userSkin,
            List<UserSkinConcern> concerns,
            List<SkinRoutineFeedback> recentFeedbacks,
            SkinCareType careType,
            RegisteredCosmetic cosmetic,
            int stepOrder
    ) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return fallbackStepContent(careType, cosmetic);
        }

        try {
            String cosmeticName = (cosmetic != null) ? cosmetic.getCosmeticName() : "미등록 (" + careType.name() + " 제품)";
            String cosmeticType = (cosmetic != null) ? cosmetic.getCosmeticType().name() : "자유 선택";
            String cosmeticIngredients = (cosmetic != null) ? cosmetic.getCoreIngredients() : "정보 없음";

            String skinTypeStr = (userSkin != null) ? userSkin.getSkinType().name() : "정보 없음";
            String sensitivityStr = (userSkin != null) ? userSkin.getSensitivityLevel().name() : "정보 없음";
            String concernsStr = concerns.isEmpty()
                    ? "없음"
                    : concerns.stream().map(c -> c.getConcernType().name()).collect(Collectors.joining(", "));

            String feedbackStr = recentFeedbacks.isEmpty()
                    ? "이전 피드백 없음"
                    : recentFeedbacks.stream()
                      .map(f -> "- " + f.getContents())
                      .collect(Collectors.joining("\n"));

            String prompt = "다음 정보를 기반으로 3분 회복 루틴의 한 단계를 JSON으로 작성하세요."
                    + "\n반환 형식: {\"title\":\"...\",\"description\":\"...\",\"precautions\":\"...\",\"recommended_ingredients\":\"...\",\"product_features\":[\"...\",\"...\"],\"reason\":\"...\"}"
                    + "\n- title: 현재 진행하는 케어 단계(" + careType + ")의 목표를 나타내는 친근하고 부드러운 1문장 (예: 진정이면 '피부 자극을 진정시켜볼게요', 보습이면 '수분을 가득 채워볼게요', 영양이면 '피부에 영양을 더해볼게요' 등). 절대로 다른 단계와 동일한 제목을 반복하지 말고 care_type에 맞게 다르게 작성할 것."
                    + "\n- description: 체크인 상태, 유저 피부타입, 과거 3분 회복모드 피드백을 반영하여 왜 이 케어가 필요한지 설명하는 2~3단락 문장 (줄바꿈 \\n 포함)."
                    + "\n- precautions: 체크리스트용 2~3개 문장을 줄바꿈(\\n)으로 구분하여 작성."
                    + "\n- recommended_ingredients: 해당 케어 단계에 적합한 3개 성분을 쉼표로 구분."
                    + "\n- product_features: '사용할 제품' 카드의 체크포인트에 들어갈 2문장을 배열로 작성."                    + "\n\n[유저 기본 체질 데이터 (온보딩)]"
                    + "\n- 피부 타입: " + skinTypeStr
                    + "\n- 민감도: " + sensitivityStr
                    + "\n- 주요 피부 고민: " + concernsStr
                    + "\n\n[유저의 과거 3분 회복 모드 피드백 (개선 반영 요구사항)]"
                    + "\n" + feedbackStr
                    + "\n\n[오늘의 체크인 상태]"
                    + "\n- step_order: " + stepOrder
                    + "\n- care_type: " + careType
                    + "\n- cosmetic_name: " + cosmeticName
                    + "\n- cosmetic_type: " + cosmeticType
                    + "\n- cosmetic_ingredients: " + cosmeticIngredients
                    + "\n- trouble_level: " + safeLevel(checkin.getAnalyzedTrouble())
                    + "\n- redness_level: " + safeLevel(checkin.getAnalyzedRedness())
                    + "\n- moisture_level: " + safeLevel(checkin.getAnalyzedMoisture())
                    + "\n- oiliness_level: " + safeLevel(checkin.getAnalyzedOiliness());

            Map<String, Object> payload = Map.of(
                    "model", openAiModel,
                    "temperature", 0.4,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", "너는 스킨케어 루틴 코치다. 유저의 피부 체질 데이터와 과거 3분 회복모드 피드백을 반드시 고려하여 맞춤형 문구와 성분을 제시하라. 반드시 요청된 JSON 포맷으로만 한국어로 답변한다."),
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
                return fallbackStepContent(careType, cosmetic);
            }

            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText("")
                    .trim();

            if (content.isBlank()) {
                return fallbackStepContent(careType, cosmetic);
            }

            JsonNode contentJson = objectMapper.readTree(content);

            List<String> productFeatures = new ArrayList<>();
            JsonNode featuresNode = contentJson.path("product_features");
            if (featuresNode.isArray()) {
                for (JsonNode f : featuresNode) {
                    productFeatures.add(f.asText());
                }
            }
            if (productFeatures.isEmpty()) {
                productFeatures = fallbackProductFeatures(careType, cosmetic);
            }

            return new LlmStepContent(
                    readOrDefault(contentJson, "title", fallbackTitle(careType)),
                    readOrDefault(contentJson, "description", fallbackDescription(careType)),
                    readOrDefault(contentJson, "precautions", fallbackPrecautions(careType)),
                    readOrDefault(contentJson, "recommended_ingredients", fallbackRecommendedIngredients(careType)),
                    productFeatures,
                    readOrDefault(contentJson, "reason", fallbackReason(careType))
            );
        } catch (Exception e) {
            log.warn("[SkinRoutine] OpenAI 루틴 스텝 생성 실패: {}", e.getMessage());
            return fallbackStepContent(careType, cosmetic);
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
            RegisteredCosmetic cosmetic
    ) {
        return new LlmStepContent(
                fallbackTitle(careType),
                fallbackDescription(careType),
                fallbackPrecautions(careType),
                fallbackRecommendedIngredients(careType),
                fallbackProductFeatures(careType, cosmetic),
                fallbackReason(careType)
        );
    }

    private String fallbackTitle(SkinCareType careType) {
        return switch (careType) {
            case SOOTHING -> "먼저 피부 자극을 진정시켜볼게요.";
            case HYDRATION -> "수분을 채워 피부를 보호해주세요.";
            case MOISTURIZING -> "영양을 더해 피부 장벽을 강화할게요.";
            case BARRIER_CARE -> "피부 장벽을 촘촘하게 메워줄게요.";
            case OIL_CONTROL -> "유수분 밸런스를 잡아 유분을 조절할게요.";
            case TROUBLE_CARE -> "트러블 부위를 집중 케어해 줄게요.";
        };
    }

    private String fallbackDescription(SkinCareType careType) {
        if (careType == SkinCareType.SOOTHING) {
            return "오늘 체크인에서 피부 당김과 붉은기가 함께 기록되었어요.\n" +
                    "또한 피로도가 높아 피부 장벽이 일시적으로 약해졌을 가능성이 있어요.\n\n" +
                    "오늘은 피부에 자극을 최소화하면서\n진정 중심의 케어를 먼저 진행하는 것을 추천드려요.";
        }

        return "진정 단계를 마쳤다면 이제 피부에 수분을 공급할 차례예요.\n" +
                "수분이 쉽게 빠져나가지 않도록 보호하는 단계예요.\n\n" +
                "피부 표면을 촉촉하게 유지하고 편안한 상태를\n오래 유지할 수 있도록 보습 중심의 케어를 추천드려요.";
    }

    private String fallbackPrecautions(SkinCareType careType) {
        return switch (careType) {
            case SOOTHING, TROUBLE_CARE -> "피부가 많이 예민한 날에는 문지르기보다 가볍게 눌러 흡수시켜주세요.\n세안 후 3분 이내에 사용하면 수분 손실을 줄이는 데 도움이 됩니다.\n붉은기가 심한 부위는 얇게 한 번 더 레이어링해도 좋아요.";
            case HYDRATION, MOISTURIZING, BARRIER_CARE -> "피부가 아직 촉촉할 때 바르면 보습 효과를 오래 유지할 수 있어요.\n양 볼과 입가처럼 당김이 심한 부위는 한 번 더 얇게 덧발라주세요.\n손바닥으로 가볍게 눌러주면 흡수에 도움이 됩니다.";
            case OIL_CONTROL -> "T존과 같이 유분이 많은 부위 위주로 가볍게 흡수시켜주세요.\n과도한 마찰은 피하고 가볍게 두드려 마무리합니다.";
        };
    }

    private String fallbackRecommendedIngredients(SkinCareType careType) {
        return switch (careType) {
            case SOOTHING -> "병풀추출물(CICA), 판테놀, 알란토인";
            case HYDRATION -> "세라마이드, 히알루론산, 스쿠알란";
            case MOISTURIZING -> "쉐어버터, 히알루론산, 세라마이드";
            case BARRIER_CARE -> "세라마이드, 콜레스테롤, 지방산";
            case OIL_CONTROL -> "티트리, 나이아신아마이드, BHA";
            case TROUBLE_CARE -> "어성초추출물, 칼라민, 아연";
        };
    }

    private String getCareTypeEmoji(SkinCareType careType) {
        if (careType == null) return "✨";
        return switch (careType) {
            case SOOTHING -> "🌿";
            case HYDRATION -> "💧";
            case MOISTURIZING -> "🧴";
            case BARRIER_CARE -> "🛡️";
            case OIL_CONTROL -> "🍃";
            case TROUBLE_CARE -> "🚨";
        };
    }

    private List<String> fallbackProductFeatures(SkinCareType careType, RegisteredCosmetic cosmetic) {
        if (cosmetic != null && cosmetic.getCoreIngredients() != null && !cosmetic.getCoreIngredients().isBlank()) {
            return List.of(
                    "민감성 피부에 적합한 저자극 " + getCareTypeKr(careType) + " 케어 제품",
                    cosmetic.getCoreIngredients() + " 함유"
            );
        }
        return List.of(
                getCareTypeKr(careType) + " 효과가 뛰어난 저자극 케어 제형",
                "피부 장벽을 유연하게 유지하도록 지원"
        );
    }

    private String fallbackReason(SkinCareType careType) {
        return getCareTypeKr(careType) + " 케어가 집중적으로 필요한 피부 상태로 분석되었습니다.";
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
            List<RoutineStep> steps,
            List<StepSaveItem> memoryStepItems
    ) {
        List<SkinRoutineStepResponse> stepResponses = new ArrayList<>();

        for (int i = 0; i < steps.size(); i++) {
            RoutineStep step = steps.get(i);
            RegisteredCosmetic cosmetic = step.getRegisteredCosmetic();

            List<String> productFeatures = (memoryStepItems != null && memoryStepItems.size() > i)
                    ? memoryStepItems.get(i).content().productFeatures()
                    : fallbackProductFeatures(step.getCareType(), cosmetic);

            stepResponses.add(new SkinRoutineStepResponse(
                    step.getStepOrder(),
                    step.getCareType(),
                    getCareTypeKr(step.getCareType()),
                    getCareTypeEmoji(step.getCareType()),
                    step.getTitle(),
                    step.getDescription(),
                    getRecommendedIngredientDescription(step.getCareType()),
                    parseJsonToList(step.getRecommendedIngredients()),
                    getCategoryColor(step.getCareType()),
                    cosmetic != null ? cosmetic.getId() : null,
                    cosmetic != null ? cosmetic.getCosmeticBrand() : null,
                    cosmetic != null ? cosmetic.getCosmeticName() : "추천 제품 사용",
                    cosmetic != null ? cosmetic.getCosmeticType() : null,
                    cosmetic != null ? cosmetic.getCosmeticUrl() : null,
                    cosmetic != null ? cosmetic.getCoreIngredients() : "진정/보습 성분 함유",
                    parseTextToList(step.getPrecautions()),
                    step.getReason(),
                    productFeatures
            ));
        }

        String summaryComment = generateSummaryComment(routine.getCheckin(), steps.size());

        return new SkinRoutineResponse(
                routine.getId(),
                routine.getCheckin().getId(),
                routine.getCheckin().getDate(),
                routine.getRecoveryLevel(),
                steps.size(),
                summaryComment,
                routine.isCompleted(),
                stepResponses,
                routine.getCreatedAt()
        );
    }

    private String getRecommendedIngredientDescription(SkinCareType careType) {
        if (careType == null) return "피부 상태 개선에 도움을 주는 추천 성분이에요.";
        return switch (careType) {
            case SOOTHING -> "자극을 완화하고 피부를 편안하게 진정시키는 데 도움이 되는 성분이에요.";
            case HYDRATION -> "피부 속 수분을 유지하고 당김을 완화하는 데 도움이 되는 성분이에요.";
            case MOISTURIZING -> "영양을 공급하고 피부 장벽을 단단하게 메워주는 추천 성분이에요.";
            case BARRIER_CARE -> "손상된 피부 장벽을 회복하고 보호막을 형성해주는 성분이에요.";
            case OIL_CONTROL -> "과도한 피지를 조절하고 유수분 밸런스를 잡아주는 성분이에요.";
            case TROUBLE_CARE -> "트러블 부위를 빠르게 진정시키고 케어해주는 추천 성분이에요.";
        };
    }

    private String generateSummaryComment(Checkin checkin, int stepCount) {
        StringBuilder sb = new StringBuilder();
        boolean hasFatigue = checkin.getFatigue() != null && checkin.getFatigue() >= 3;
        boolean hasTightness = checkin.getTightness() != null && checkin.getTightness() >= 3;

        if (hasFatigue && hasTightness) {
            sb.append("피로도가 높고 피부 당김이 심하게 기록됐어요.\n");
        } else if (hasFatigue) {
            sb.append("피로도가 높게 기록되어 피부 쉬어가기가 필요해요.\n");
        } else if (hasTightness) {
            sb.append("피부 당김이 심하게 기록되어 수분 충전이 필요해요.\n");
        } else {
            sb.append("피부 상태가 전반적으로 양호하고 안정적이에요.\n");
        }

        sb.append("오늘은 피부 컨디션 유지를 위한 최소 ").append(stepCount).append("단계만 진행할게요.");
        return sb.toString();
    }
    private List<String> parseJsonToList(String json) {
        if (json == null || json.isBlank()) return List.of("기본 보습 성분");
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of(json.replace("[", "").replace("]", "").replace("\"", "").split(","));
        }
    }

    private List<String> parseTextToList(String text) {
        if (text == null || text.isBlank()) return List.of("자극 없이 부드럽게 사용해주세요.");
        return List.of(text.split("\n"));
    }

    private String getCareTypeKr(SkinCareType careType) {
        if (careType == null) return "케어";
        return switch (careType) {
            case SOOTHING -> "진정";
            case HYDRATION -> "보습";
            case MOISTURIZING -> "영양";
            case BARRIER_CARE -> "장벽 케어";
            case OIL_CONTROL -> "피지 조절";
            case TROUBLE_CARE -> "트러블 케어";
        };
    }

    private String getCategoryColor(SkinCareType careType) {
        if (careType == null) return "GREEN";
        return switch (careType) {
            case SOOTHING -> "GREEN";
            case OIL_CONTROL -> "LIGHT_GREEN";
            case TROUBLE_CARE -> "YELLOW";
            case HYDRATION, MOISTURIZING, BARRIER_CARE -> "BLUE";
        };
    }

    private record LlmStepContent(
            String title,
            String description,
            String precautions,
            String recommendedIngredients,
            List<String> productFeatures,
            String reason
    ) {
    }

    private record StepSaveItem(
            int stepOrder,
            SkinCareType careType,
            RegisteredCosmetic cosmetic,
            LlmStepContent content
    ) {
    }
}