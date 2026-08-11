package org.example.nura.domain.skin.service;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.skin.dto.request.CheckinCreateRequest;
import org.example.nura.domain.skin.dto.response.CheckinResponse;
import org.example.nura.domain.skin.dto.response.CheckinStatusResponse;
import org.example.nura.domain.skin.entity.Checkin;
import org.example.nura.domain.skin.entity.SkinRoutine;
import org.example.nura.domain.skin.entity.enums.CheckinSkinLevel;
import org.example.nura.domain.skin.entity.enums.RecoveryLevel;
import org.example.nura.domain.skin.entity.enums.SkinAnalysisLevel;
import org.example.nura.domain.skin.repository.CheckinRepository;
import org.example.nura.domain.skin.repository.SkinRoutineRepository;
import org.example.nura.domain.user.entity.User;
import org.example.nura.domain.user.repository.UserRepository;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckinService {

    private final UserRepository userRepository;
    private final CheckinRepository checkinRepository;
    private final SkinRoutineRepository skinRoutineRepository;
    private final SkinAnalysisService skinAnalysisService;

    public CheckinStatusResponse getStatus(
            Long userId,
            LocalDate date
    ) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;

        Checkin existing = checkinRepository.findByUserIdAndDate(
                        userId,
                        targetDate
                )
                .orElse(null);

        if (existing == null) {
            return new CheckinStatusResponse(
                    targetDate,
                    true,
                    null,
                    null
            );
        }

        return new CheckinStatusResponse(
                targetDate,
                false,
                "ALREADY_CHECKED_IN",
                existing.getId()
        );
    }

    @Transactional
    public CheckinResponse create(
            Long userId,
            CheckinCreateRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BaseException(ErrorCode.RESOURCE_NOT_FOUND)
                );

        if (checkinRepository.existsByUserIdAndDate(
                userId,
                request.date()
        )) {
            throw new BaseException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "해당 날짜의 체크인은 이미 존재합니다."
            );
        }

        int tightnessScore = request.tightness().toScore();
        int rednessScore = request.redness().toScore();

        Checkin checkin = Checkin.create(
                user,
                request.date(),
                request.fatigue(),
                tightnessScore,
                rednessScore,
                buildTemporaryPhotoUrl(request.photo())
        );

        Checkin savedCheckin = checkinRepository.save(checkin);

        RecoveryLevel recoveryLevel = deriveRecoveryLevel(
                request.fatigue(),
                tightnessScore,
                rednessScore
        );

        SkinRoutine skinRoutine = SkinRoutine.create(
                savedCheckin,
                recoveryLevel
        );

        skinRoutineRepository.save(skinRoutine);

        SkinAnalysisService.AnalysisResult analysisResult =
                skinAnalysisService.analyze(
                        request.photo(),
                        request.fatigue(),
                        tightnessScore,
                        rednessScore
                );

        savedCheckin.updateAnalysis(
                analysisResult.analyzedRedness(),
                analysisResult.analyzedMoisture(),
                analysisResult.analyzedOiliness(),
                analysisResult.analyzedTrouble(),
                analysisResult.aiComment()
        );

        return new CheckinResponse(
                savedCheckin.getId(),
                user.getId(),
                savedCheckin.getDate(),
                savedCheckin.getFatigue(),
                CheckinSkinLevel.fromScore(savedCheckin.getTightness()),
                CheckinSkinLevel.fromScore(savedCheckin.getRedness()),
                recoveryLevel,
                defaultUnknown(savedCheckin.getAnalyzedRedness()),
                defaultUnknown(savedCheckin.getAnalyzedMoisture()),
                defaultUnknown(savedCheckin.getAnalyzedOiliness()),
                defaultUnknown(savedCheckin.getAnalyzedTrouble()),
                savedCheckin.getAiComment(),
                savedCheckin.getPhotoUrl(),
                savedCheckin.getCreatedAt()
        );
    }

    /**
     * 피로도(1~5) + 피부당김(1~4) + 붉은기(1~4) 합산으로 회복 단계를 결정합니다.
     * 점수가 낮을수록 피부 상태가 좋아 더 많은 단계를 수행합니다.
     * - 합계 ≤ 5  → LEVEL_3 (상태 양호, 풀 루틴)
     * - 합계 ≤ 9  → LEVEL_2
     * - 합계 > 9  → LEVEL_1 (상태 나쁨, 짧은 루틴)
     */
    private RecoveryLevel deriveRecoveryLevel(
            int fatigue,
            int tightnessScore,
            int rednessScore
    ) {
        int total = fatigue + tightnessScore + rednessScore;

        if (total <= 5) {
            return RecoveryLevel.LEVEL_3;
        } else if (total <= 9) {
            return RecoveryLevel.LEVEL_2;
        } else {
            return RecoveryLevel.LEVEL_1;
        }
    }

    private String buildTemporaryPhotoUrl(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            return null;
        }

        String originalFilename = photo.getOriginalFilename();
        String safeFilename =
                originalFilename == null
                        ? "image"
                        : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");

        // TODO: AI 연동 및 실제 파일 저장 연동 전까지 임시 URL 포맷만 유지합니다.
        return "pending://ai-analysis/" + LocalDate.now() + "/"
                + UUID.randomUUID() + "-" + safeFilename;
    }

    private SkinAnalysisLevel defaultUnknown(
            SkinAnalysisLevel value
    ) {
        return value == null ? SkinAnalysisLevel.UNKNOWN : value;
    }
}

