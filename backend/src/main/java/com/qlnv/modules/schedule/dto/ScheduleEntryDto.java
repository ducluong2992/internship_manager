package com.qlnv.modules.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

public class ScheduleEntryDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Entry {
        @JsonProperty("work_day")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate workDay;

        private String shift; // S / C / SC / None
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Submit {
        @JsonProperty("period_id")
        private Integer periodId;

        private List<Entry> entries;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Integer id;

        @JsonProperty("period_id")
        private Integer periodId;

        @JsonProperty("user_id")
        private Integer userId;

        @JsonProperty("work_day")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate workDay;

        private String shift;
    }
}
