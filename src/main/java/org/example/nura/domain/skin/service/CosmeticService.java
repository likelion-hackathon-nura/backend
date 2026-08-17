package org.example.nura.domain.skin.service;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.skin.dto.request.RegisteredCosmeticCreateRequest;
import org.example.nura.domain.skin.dto.response.CosmeticOcrResponse;
import org.example.nura.domain.skin.dto.response.RegisteredCosmeticListResponse;
import org.example.nura.domain.skin.dto.response.RegisteredCosmeticListResponse.CosmeticDetail;
import org.springframework.util.StringUtils;
import org.example.nura.domain.skin.dto.response.RegisteredCosmeticResponse;
import org.example.nura.domain.skin.entity.RegisteredCosmetic;
import org.example.nura.domain.skin.repository.RegisteredCosmeticRepository;
import org.example.nura.domain.skin.repository.RoutineStepRepository;
import org.example.nura.domain.user.entity.User;
import org.example.nura.domain.user.repository.UserRepository;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.example.nura.global.infra.ocr.CosmeticOcrClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CosmeticService {

    private final RegisteredCosmeticRepository registeredCosmeticRepository;
    private final UserRepository userRepository;
    private final CosmeticOcrClient cosmeticOcrClient;
    private final OpenAiService openAiService;

    /**
     * 화장품 뒷면 사진 OCR 분석 (Clova OCR + OpenAI 파싱)
     */
    public CosmeticOcrResponse analyzeOcr(MultipartFile photo) {
        String rawText = cosmeticOcrClient.analyze(photo);

        OpenAiService.IngredientParseResult parseResult = openAiService.parseIngredients(rawText);

        return new CosmeticOcrResponse(
                parseResult.cosmeticIngredients(),
                parseResult.coreIngredients()
        );
    }

    /**
     * 화장품 최종 등록 (수기 입력 데이터 + OCR 파싱 결과)
     */
    @Transactional
    public RegisteredCosmeticResponse registerCosmetic(
            Long userId,
            RegisteredCosmeticCreateRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));

        RegisteredCosmetic cosmetic = RegisteredCosmetic.create(
                user,
                request.cosmeticBrand(),
                request.cosmeticName(),
                request.cosmeticType(),
                request.cosmeticIngredients(),
                request.coreIngredients(),
                request.cosmeticUrl()
        );

        RegisteredCosmetic savedCosmetic = registeredCosmeticRepository.save(cosmetic);

        return new RegisteredCosmeticResponse(
                savedCosmetic.getId(),
                user.getId(),
                savedCosmetic.getCosmeticBrand(),
                savedCosmetic.getCosmeticName(),
                savedCosmetic.getCosmeticType(),
                savedCosmetic.getCreatedAt()
        );
    }


    private final RoutineStepRepository routineStepRepository;

    /**
     * 등록 화장품 삭제
     */
    @Transactional
    public void deleteCosmetic(Long userId, Long cosmeticId) {
        RegisteredCosmetic cosmetic = registeredCosmeticRepository.findByIdAndUserId(cosmeticId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않거나 삭제 권한이 없는 화장품입니다."));

        routineStepRepository.bulkSetCosmeticNull(cosmeticId);

        registeredCosmeticRepository.delete(cosmetic);
    }

    /**
     * 사용자의 등록 화장품 목록 조회 및 검색
     */
    @Transactional(readOnly = true)
    public RegisteredCosmeticListResponse getMyCosmetics(Long userId, String keyword) {
        List<RegisteredCosmetic> cosmetics;

        // 검색어가 있으면 이름으로 필터링, 없으면 전체 조회
        if (StringUtils.hasText(keyword)) {
            cosmetics = registeredCosmeticRepository
                    .findByUserIdAndCosmeticNameContainingIgnoreCaseOrderByCreatedAtDesc(userId, keyword.trim());
        } else {
            cosmetics = registeredCosmeticRepository
                    .findByUserIdOrderByCreatedAtDesc(userId);
        }

        List<CosmeticDetail> list = cosmetics.stream()
                .map(CosmeticDetail::from)
                .toList();

        return RegisteredCosmeticListResponse.builder()
                .totalCount(list.size())
                .cosmetics(list)
                .build();
    }



}