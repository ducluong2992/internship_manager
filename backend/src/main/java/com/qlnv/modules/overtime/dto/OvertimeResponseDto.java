package com.qlnv.modules.overtime.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class OvertimeResponseDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Integer id;

        @JsonProperty("user_id")
        private Integer userId;

        @JsonProperty("employee_code")
        private String employeeCode;

        @JsonProperty("full_name")
        private String fullName;

        private String project;

        @JsonProperty("work_date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate workDate;

        @JsonProperty("start_time")
        private String startTime;

        @JsonProperty("end_time")
        private String endTime;

        @JsonProperty("raw_hours")
        private Double rawHours;

        private Double factor;

        @JsonProperty("weighted_hours")
        private Double weightedHours;

        private String reason;
        private String status;

        @JsonProperty("reject_reason")
        private String rejectReason;

        @JsonProperty("approved_by")
        private Integer approvedBy;

        @JsonProperty("approved_at")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime approvedAt;

        @JsonProperty("created_at")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Approve {
        @JsonProperty("reject_reason")
        private String rejectReason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApproveSelected {
        private List<Integer> ids;
    }
}
