package org.example.nura.domain.report.repository;

import org.example.nura.domain.report.entity.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

    void deleteAllByUserId(Long userId);
}
