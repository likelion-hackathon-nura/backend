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
import org.example.nura.global.infra.ocr.CosmeticOcrClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
}