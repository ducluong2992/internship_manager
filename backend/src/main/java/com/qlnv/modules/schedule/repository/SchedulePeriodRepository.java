package com.qlnv.modules.schedule.repository;

import com.qlnv.modules.schedule.entity.SchedulePeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchedulePeriodRepository extends JpaRepository<SchedulePeriod, Integer> {
    Optional<SchedulePeriod> findByMonthAndYear(Integer month, Integer year);
    Optional<SchedulePeriod> findFirstByStatusOrderByIdDesc(String status);
    List<SchedulePeriod> findAllByOrderByYearDescMonthDesc();
}
