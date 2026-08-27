package com.qlnv.modules.user.repository;

import com.qlnv.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {
    
    // ─── Single-Value Fast Indexed Lookups ───
    Optional<User> findByEmployeeCode(String employeeCode);
    Optional<User> findByViettelEmail(String viettelEmail);
    boolean existsByEmployeeCode(String employeeCode);
    boolean existsByViettelEmail(String viettelEmail);

    // ─── Range & Criteria Queries ───
    List<User> findByJoinDateBetween(LocalDate fromDate, LocalDate toDate);
    List<User> findByBorrowEndDateBetween(LocalDate fromDate, LocalDate toDate);
    List<User> findByRole(String role);
    List<User> findByUserType(String userType);
    List<User> findByWorkingStatus(String workingStatus);
    List<User> findByUserTypeAndWorkingStatus(String userType, String workingStatus);
    List<User> findByDirectManager(String directManager);
    List<User> findByProject(String project);
}
