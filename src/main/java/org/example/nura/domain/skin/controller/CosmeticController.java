package org.example.nura.domain.skin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.nura.domain.skin.dto.request.RegisteredCosmeticCreateRequest;
import org.example.nura.domain.skin.dto.response.CosmeticOcrResponse;
import org.example.nura.domain.skin.dto.response.RegisteredCosmeticResponse;
import org.example.nura.domain.skin.service.CosmeticService;
import org.example.nura.global.common.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "사용 화장품 관리", description = "사용자 등록 화장품 OCR 분석 및 등록 API")
@RestController
@RequestMapping("/api/v1/cosmetics")
@RequiredArgsConstructor
public class CosmeticController {

    private final CosmeticService cosmeticService;

    @Operation(summary = "화장품 OCR 사진 분석", description = "화장품 뒷면 사진을 업로드하여 성분 및 상품 정보를 추출합니다.")
    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CosmeticOcrResponse> analyzeOcr(
            @RequestPart("photo") MultipartFile photo
    ) {
        CosmeticOcrResponse response = cosmeticService.analyzeOcr(photo);
        return ApiResponse.success("화장품 사진 OCR 분석에 성공했습니다.", response);
    }

    @Operation(summary = "사용 화장품 등록", description = "OCR 분석 결과 또는 직접 입력한 화장품 정보를 등록합니다.")
    @PostMapping
    public ApiResponse<RegisteredCosmeticResponse> register(
            @AuthenticationPrincipal Long userId,
            @RequestBody RegisteredCosmeticCreateRequest request
    ) {
        RegisteredCosmeticResponse response = cosmeticService.registerCosmetic(userId, request);
        return ApiResponse.success("화장품 등록에 성공했습니다.", response);
    }
}