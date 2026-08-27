package com.qlnv.modules.overtime.repository;

import com.qlnv.modules.overtime.entity.OvertimeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OvertimeRequestRepository extends JpaRepository<OvertimeRequest, Integer>, JpaSpecificationExecutor<OvertimeRequest> {

    // ─── Single & User queries ───
    List<OvertimeRequest> findByUserIdOrderByWorkDateDescStartTimeDesc(Integer userId);
    List<OvertimeRequest> findByUserIdAndStatus(Integer userId, String status);

    // ─── Range queries ───
    List<OvertimeRequest> findByUserIdAndWorkDateBetween(Integer userId, LocalDate fromDate, LocalDate toDate);
    List<OvertimeRequest> findByWorkDateBetween(LocalDate fromDate, LocalDate toDate);
    List<OvertimeRequest> findByStatusAndWorkDateBetween(String status, LocalDate fromDate, LocalDate toDate);
    List<OvertimeRequest> findByWeightedHoursBetween(Double minHours, Double maxHours);
    List<OvertimeRequest> findByStatus(String status);
    List<OvertimeRequest> findByUserIdAndWorkDate(Integer userId, LocalDate workDate);

    @Transactional
    @Modifying
    @Query("DELETE FROM OvertimeRequest o WHERE o.userId = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
}
