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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CheckinService {

    private final UserRepository userRepository;
    private final CheckinRepository checkinRepository;
    private final SkinRoutineRepository skinRoutineRepository;
    private final SkinAnalysisService skinAnalysisService;
    private final TransactionTemplate transactionTemplate;

    @Transactional(readOnly = true)
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

    public CheckinResponse create(
            Long userId,
            CheckinCreateRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BaseException(ErrorCode.RESOURCE_NOT_FOUND)
                );

        if (checkinRepository.existsByUserIdAndDate(userId, request.date())) {
            throw new BaseException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "해당 날짜의 체크인은 이미 존재합니다."
            );
        }

        int tightnessScore = request.tightness().toScore();
        int rednessScore = request.redness().toScore();

        String photoUrl = null;

        // 외부 AI 분석 (근무 정보, 사진 정량 분석, GPT 코멘트/태그 통합 처리)
        SkinAnalysisService.AnalysisResult analysisResult =
                skinAnalysisService.analyze(
                        userId,
                        request.date(),
                        request.photo(),
                        request.fatigue(),
                        tightnessScore,
                        rednessScore
                );

        return transactionTemplate.execute(status ->
                saveCheckinLogic(user, request, tightnessScore, rednessScore, photoUrl, analysisResult)
        );
    }

    private CheckinResponse saveCheckinLogic(
            User user,
            CheckinCreateRequest request,
            int tightnessScore,
            int rednessScore,
            String photoUrl,
            SkinAnalysisService.AnalysisResult analysisResult
    ) {
        try {
            Checkin checkin = Checkin.create(
                    user,
                    request.date(),
                    request.fatigue(),
                    tightnessScore,
                    rednessScore,
                    photoUrl
            );

            checkin.updateAnalysis(
                    analysisResult.analyzedRedness(),
                    analysisResult.analyzedMoisture(),
                    analysisResult.analyzedOiliness(),
                    analysisResult.analyzedTrouble(),
                    analysisResult.aiComment(),
                    analysisResult.rednessComment(),
                    analysisResult.moistureComment(),
                    analysisResult.troubleComment(),
                    analysisResult.tags()
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
                    savedCheckin.getCreatedAt()
            );
        } catch (DataIntegrityViolationException e) {
            throw new BaseException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "해당 날짜의 체크인은 이미 존재합니다."
            );
        }
    }

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

    private SkinAnalysisLevel defaultUnknown(
            SkinAnalysisLevel value
    ) {
        return value == null ? SkinAnalysisLevel.UNKNOWN : value;
    }
}