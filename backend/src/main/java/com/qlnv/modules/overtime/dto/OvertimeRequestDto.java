package com.qlnv.modules.overtime.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OvertimeRequestDto {

    @NotNull(message = "Ngày OT không được để trống")
    @JsonProperty("work_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate workDate;

    @JsonProperty("end_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @NotBlank(message = "Giờ bắt đầu không được để trống")
    @JsonProperty("start_time")
    private String startTime;

    @NotBlank(message = "Giờ kết thúc không được để trống")
    @JsonProperty("end_time")
    private String endTime;

    private String project;
    private String reason;

    @Builder.Default
    @JsonProperty("is_holiday")
    private Boolean isHoliday = false;

    private Double factor;
}
