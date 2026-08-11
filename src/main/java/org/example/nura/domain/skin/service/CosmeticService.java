package org.example.nura.domain.skin.service;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.skin.dto.request.RegisteredCosmeticCreateRequest;
import org.example.nura.domain.skin.dto.response.CosmeticOcrResponse;
import org.example.nura.domain.skin.dto.response.FastApiOcrResponse;
import org.example.nura.domain.skin.dto.response.RegisteredCosmeticResponse;
import org.example.nura.domain.skin.entity.RegisteredCosmetic;
import org.example.nura.domain.skin.repository.RegisteredCosmeticRepository;
import org.example.nura.domain.user.entity.User;
import org.example.nura.domain.user.repository.UserRepository;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.example.nura.global.infra.fastapi.FastApiClient;
import org.example.nura.global.infra.s3.S3Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CosmeticService {

    private final RegisteredCosmeticRepository registeredCosmeticRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final FastApiClient fastApiClient; // FastAPI 클라이언트 주입

    /**
     * 화장품 뒷면 사진 OCR 분석
     */
    public CosmeticOcrResponse analyzeOcr(MultipartFile photo) {
        // 1. S3에 화장품 사진 업로드 (진짜 S3 URL 생성)
        String photoUrl = s3Service.upload(photo, "cosmetics");

        // 2. FastAPI OCR 서버 호출하여 텍스트 추출 (서버 미기동 시 안전하게 폴백 반환)
        FastApiOcrResponse ocrResult = fastApiClient.analyzeCosmeticOcr(photoUrl);

        // 3. 최종 분석 결과와 S3 URL 반환
        return new CosmeticOcrResponse(
                ocrResult.extractedBrand(),
                ocrResult.extractedName(),
                ocrResult.extractedType(),
                ocrResult.extractedIngredient(),
                ocrResult.extractedCoreIngredient(),
                photoUrl
        );
    }

    /**
     * 화장품 최종 등록
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
                request.cosmeticIngredient(),
                request.cosmeticCoreIngredient(),
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