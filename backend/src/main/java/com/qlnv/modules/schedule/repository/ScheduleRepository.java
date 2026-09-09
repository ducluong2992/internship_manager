package com.qlnv.modules.schedule.repository;

import com.qlnv.modules.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Integer>, JpaSpecificationExecutor<Schedule> {

    // ─── Single-Value Lookups ───
    Optional<Schedule> findByPeriodIdAndUserIdAndWorkDay(Integer periodId, Integer userId, LocalDate workDay);

    // ─── Range & Criteria Queries ───
    List<Schedule> findByPeriodIdAndUserId(Integer periodId, Integer userId);
    List<Schedule> findByPeriodId(Integer periodId);
    List<Schedule> findByWorkDay(LocalDate workDay);
    List<Schedule> findByWorkDayBetween(LocalDate startDate, LocalDate endDate);
    List<Schedule> findByUserIdAndWorkDayBetween(Integer userId, LocalDate startDate, LocalDate endDate);

    @Transactional
    @Modifying
    @Query("DELETE FROM Schedule s WHERE s.periodId = :periodId AND s.userId = :userId")
    void deleteByPeriodIdAndUserId(@Param("periodId") Integer periodId, @Param("userId") Integer userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Schedule s WHERE s.periodId = :periodId")
    void deleteByPeriodId(@Param("periodId") Integer periodId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Schedule s WHERE s.userId = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
}
