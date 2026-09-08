package com.qlnv.modules.overtime.repository;

import com.qlnv.modules.overtime.entity.OvertimeRequest;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OvertimeSpecification {

    public static Specification<OvertimeRequest> filter(
            Integer userId,
            String status,
            String project,
            LocalDate fromDate,
            LocalDate toDate,
            String keyword) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }

            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status.trim()));
            }

            if (project != null && !project.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("project")), "%" + project.trim().toLowerCase() + "%"));
            }

            // Date range queries
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("workDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("workDate"), toDate));
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                var userJoin = root.join("user", JoinType.LEFT);
                Predicate codeMatch = cb.like(cb.lower(userJoin.get("employeeCode")), pattern);
                Predicate nameMatch = cb.like(cb.lower(userJoin.get("fullName")), pattern);
                predicates.add(cb.or(codeMatch, nameMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
