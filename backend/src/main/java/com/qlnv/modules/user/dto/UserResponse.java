package com.qlnv.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Integer id;

    @JsonProperty("employee_code")
    private String employeeCode;

    @JsonProperty("full_name")
    private String fullName;

    private String role;

    @JsonProperty("user_type")
    private String userType;

    private String gender;
    private String ethnicity;

    @JsonProperty("viettel_email")
    private String viettelEmail;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    private String hometown;
    private String phone;
    private String cccd;

    @JsonProperty("bank_name")
    private String bankName;

    @JsonProperty("bank_account")
    private String bankAccount;

    private String project;
    private String position;

    @JsonProperty("position_id")
    private Integer positionId;

    @JsonProperty("position_name")
    private String positionName;

    @JsonProperty("join_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate joinDate;

    private String allowance;

    @JsonProperty("employee_type")
    private String employeeType;

    @JsonProperty("working_status")
    private String workingStatus;

    @JsonProperty("employment_type")
    private String employmentType;

    @JsonProperty("direct_manager")
    private String directManager;

    @JsonProperty("computer_serial")
    private String computerSerial;

    @JsonProperty("employment_status")
    private String employmentStatus;

    @JsonProperty("use_company_mac")
    private String useCompanyMac;

    @JsonProperty("staff_category")
    private String staffCategory;

    @JsonProperty("seat_position")
    private String seatPosition;

    @JsonProperty("borrow_end_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate borrowEndDate;

    @JsonProperty("borrow_project")
    private String borrowProject;

    @JsonProperty("borrow_pm")
    private String borrowPm;

    @JsonProperty("borrow_center")
    private String borrowCenter;

    @JsonProperty("account_status")
    private Integer accountStatus;

    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    private String username;
}
