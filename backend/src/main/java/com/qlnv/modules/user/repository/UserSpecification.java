package com.qlnv.modules.user.repository;

import com.qlnv.modules.user.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    public static Specification<User> filter(
            String keyword,
            String userType,
            String role,
            String project,
            String workingStatus,
            String staffCategory,
            String employmentStatus,
            LocalDate joinDateFrom,
            LocalDate joinDateTo,
            LocalDate borrowEndFrom,
            LocalDate borrowEndTo) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate codeMatch = cb.like(cb.lower(root.get("employeeCode")), pattern);
                Predicate nameMatch = cb.like(cb.lower(root.get("fullName")), pattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("viettelEmail")), pattern);
                Predicate phoneMatch = cb.like(cb.lower(root.get("phone")), pattern);
                Predicate cccdMatch = cb.like(cb.lower(root.get("cccd")), pattern);
                predicates.add(cb.or(codeMatch, nameMatch, emailMatch, phoneMatch, cccdMatch));
            }

            if (userType != null && !userType.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("userType"), userType.trim()));
            }

            if (role != null && !role.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("role"), role.trim()));
            }

            if (project != null && !project.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("project"), project.trim()));
            }

            if (workingStatus != null && !workingStatus.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("workingStatus"), workingStatus.trim()));
            }

            if (staffCategory != null && !staffCategory.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("staffCategory"), staffCategory.trim()));
            }

            if (employmentStatus != null && !employmentStatus.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("employmentStatus"), employmentStatus.trim()));
            }

            // Date Range Queries
            if (joinDateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("joinDate"), joinDateFrom));
            }
            if (joinDateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("joinDate"), joinDateTo));
            }

            if (borrowEndFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("borrowEndDate"), borrowEndFrom));
            }
            if (borrowEndTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("borrowEndDate"), borrowEndTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
