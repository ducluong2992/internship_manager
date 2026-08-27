package com.qlnv.modules.user.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_empcode", columnList = "employee_code", unique = true),
    @Index(name = "idx_users_status_type", columnList = "working_status, user_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "employee_code", unique = true, nullable = false)
    private String employeeCode;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Builder.Default
    private String role = "user"; // admin / user

    @Builder.Default
    @Column(name = "user_type")
    private String userType = "intern"; // intern / employee / admin

    private String gender;
    private String ethnicity;

    @Column(name = "viettel_email")
    private String viettelEmail;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    private String hometown;
    private String phone;
    private String cccd;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_account")
    private String bankAccount;

    private String project;
    private String position;

    @Column(name = "position_id")
    private Integer positionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", insertable = false, updatable = false)
    private Position positionRel;

    @Column(name = "join_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate joinDate;

    @Builder.Default
    private String allowance = "Không";

    @Builder.Default
    @Column(name = "employee_type")
    private String employeeType = "TTS Trung tâm";

    @Builder.Default
    @Column(name = "working_status")
    private String workingStatus = "Working"; // Working / Resigned

    @Builder.Default
    @Column(name = "employment_type")
    private String employmentType = "Fulltime"; // Fulltime / Parttime

    // ─── Employee-only fields ───
    @Column(name = "direct_manager")
    private String directManager;

    @Column(name = "computer_serial")
    private String computerSerial;

    @Builder.Default
    @Column(name = "employment_status")
    private String employmentStatus = "Thử việc";

    @Builder.Default
    @Column(name = "use_company_mac")
    private String useCompanyMac = "Không";

    @Builder.Default
    @Column(name = "staff_category")
    private String staffCategory = "NS trung tâm"; // NS trung tâm / Onsite / Cho mượn

    @Column(name = "seat_position")
    private String seatPosition;

    @Column(name = "borrow_end_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate borrowEndDate;

    @Column(name = "borrow_project")
    private String borrowProject;

    @Column(name = "borrow_pm")
    private String borrowPm;

    @Column(name = "borrow_center")
    private String borrowCenter;

    @Builder.Default
    @Column(name = "account_status")
    private Integer accountStatus = 1; // 1=active, 0=locked

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
