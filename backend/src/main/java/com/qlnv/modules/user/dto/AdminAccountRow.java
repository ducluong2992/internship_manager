package com.qlnv.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAccountRow {

    @JsonProperty("user_id")
    private Integer userId;

    @JsonProperty("employee_code")
    private String employeeCode;

    @JsonProperty("full_name")
    private String fullName;

    private String username;

    private String password;

    @JsonProperty("account_status")
    private Integer accountStatus;

    private String role;

    @JsonProperty("working_status")
    private String workingStatus;

    @JsonProperty("user_type")
    private String userType;
}
