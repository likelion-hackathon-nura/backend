package org.example.nura.domain.skin.service;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.skin.dto.request.RegisteredCosmeticCreateRequest;
import org.example.nura.domain.skin.dto.response.CosmeticOcrResponse;
import org.example.nura.domain.skin.dto.response.RegisteredCosmeticResponse;
import org.example.nura.domain.skin.entity.RegisteredCosmetic;
import org.example.nura.domain.skin.repository.RegisteredCosmeticRepository;
import org.example.nura.domain.user.entity.User;
import org.example.nura.domain.user.repository.UserRepository;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.example.nura.global.infra.clova.ClovaOcrClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CosmeticService {

    private final RegisteredCosmeticRepository registeredCosmeticRepository;
    private final UserRepository userRepository;
    private final ClovaOcrClient clovaOcrClient; // S3 대신 MultipartFile을 전달받는 Client 사용
    private final OpenAiService openAiService;

    /**
     * 화장품 뒷면 사진 OCR 분석 (Clova OCR + OpenAI 파싱)
     * S3 저장 없이 MultipartFile을 일회성 메모리 데이터로 직접 분석
     */
    public CosmeticOcrResponse analyzeOcr(MultipartFile photo) {
        // 1. Clova OCR로 MultipartFile 직접 전달하여 Raw 텍스트 추출
        String rawText = clovaOcrClient.analyze(photo);

        // 2. GPT-4o-mini로 전성분/핵심성분 추출 및 정제
        OpenAiService.IngredientParseResult parseResult = openAiService.parseIngredients(rawText);

        // 3. 결과 반환
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
}