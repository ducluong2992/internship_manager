package com.qlnv.modules.overtime.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qlnv.common.exception.ApiException;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class OvertimeCalculator {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Segment {
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

        @JsonProperty("shift_type")
        private String shiftType;

        @JsonProperty("shift_name")
        private String shiftName;
    }

    public int parseTimeMinutes(String t) {
        try {
            String[] parts = t.trim().split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Định dạng giờ không hợp lệ. Dùng HH:MM");
        }
    }

    public String minutesToHhmm(int mins) {
        int h = (mins / 60) % 24;
        int m = mins % 60;
        return String.format("%02d:%02d", h, m);
    }

    public double calcRawHours(int startMin, int endMin) {
        return Math.round(((double) (endMin - startMin) / 60.0) * 100.0) / 100.0;
    }

    public double getFactor(boolean isHoliday, boolean isWeekend, boolean before22) {
        if (isHoliday) {
            return before22 ? 3.0 : 3.9;
        }
        if (isWeekend) {
            return before22 ? 2.0 : 2.7;
        }
        return before22 ? 1.5 : 2.1;
    }

    public List<Segment> splitSegments(int startMin, int endMin, boolean isHoliday, boolean isWeekend, LocalDate workDate) {
        if (workDate == null) {
            workDate = LocalDate.now();
        }

        int dayStart = (isWeekend || isHoliday) ? (8 * 60) : (18 * 60 + 30);
        List<Segment> segments = new ArrayList<>();

        // 1. Segments on workDate (0 - 1440 mins)
        int sCurr = Math.max(0, startMin);
        int eCurr = Math.min(endMin, 1440);

        if (sCurr < eCurr) {
            // 1.1 Early morning (00:00 - 08:00)
            int s1 = Math.max(sCurr, 0);
            int e1 = Math.min(eCurr, 8 * 60);
            if (s1 < e1) {
                double raw = calcRawHours(s1, e1);
                double factor = getFactor(isHoliday, isWeekend, false);
                segments.add(Segment.builder()
                        .workDate(workDate)
                        .startTime(minutesToHhmm(s1))
                        .endTime(minutesToHhmm(e1))
                        .rawHours(raw)
                        .factor(factor)
                        .weightedHours(Math.round(raw * factor * 100.0) / 100.0)
                        .shiftType("night")
                        .shiftName("Ban đêm")
                        .build());
            }

            // 1.2 Daytime (dayStart - 22:00)
            int s2 = Math.max(sCurr, dayStart);
            int e2 = Math.min(eCurr, 22 * 60);
            if (s2 < e2) {
                double raw = calcRawHours(s2, e2);
                double factor = getFactor(isHoliday, isWeekend, true);
                segments.add(Segment.builder()
                        .workDate(workDate)
                        .startTime(minutesToHhmm(s2))
                        .endTime(minutesToHhmm(e2))
                        .rawHours(raw)
                        .factor(factor)
                        .weightedHours(Math.round(raw * factor * 100.0) / 100.0)
                        .shiftType("day")
                        .shiftName("Ban ngày")
                        .build());
            }

            // 1.3 Night before midnight (22:00 - 24:00)
            int s3 = Math.max(sCurr, 22 * 60);
            int e3 = Math.min(eCurr, 24 * 60);
            if (s3 < e3) {
                double raw = calcRawHours(s3, e3);
                double factor = getFactor(isHoliday, isWeekend, false);
                segments.add(Segment.builder()
                        .workDate(workDate)
                        .startTime(minutesToHhmm(s3))
                        .endTime(e3 == 1440 ? "24:00" : minutesToHhmm(e3))
                        .rawHours(raw)
                        .factor(factor)
                        .weightedHours(Math.round(raw * factor * 100.0) / 100.0)
                        .shiftType("night")
                        .shiftName("Ban đêm")
                        .build());
            }
        }

        // 2. Segments next day if endMin > 1440
        if (endMin > 1440) {
            LocalDate nextDate = workDate.plusDays(1);
            boolean nextIsWeekend = nextDate.getDayOfWeek() == DayOfWeek.SATURDAY || nextDate.getDayOfWeek() == DayOfWeek.SUNDAY;
            int nextDayStart = nextIsWeekend ? (8 * 60) : (18 * 60 + 30);

            int remStart = 0;
            int remEnd = endMin - 1440;

            // Early morning next day (00:00 - 08:00)
            int s4 = Math.max(remStart, 0);
            int e4 = Math.min(remEnd, 8 * 60);
            if (s4 < e4) {
                double raw = calcRawHours(s4, e4);
                double factor = getFactor(false, nextIsWeekend, false);
                segments.add(Segment.builder()
                        .workDate(nextDate)
                        .startTime(minutesToHhmm(s4))
                        .endTime(minutesToHhmm(e4))
                        .rawHours(raw)
                        .factor(factor)
                        .weightedHours(Math.round(raw * factor * 100.0) / 100.0)
                        .shiftType("night")
                        .shiftName("Ban đêm")
                        .build());
            }

            // Next day daytime
            int s5 = Math.max(remStart, nextDayStart);
            int e5 = Math.min(remEnd, 22 * 60);
            if (s5 < e5) {
                double raw = calcRawHours(s5, e5);
                double factor = getFactor(false, nextIsWeekend, true);
                segments.add(Segment.builder()
                        .workDate(nextDate)
                        .startTime(minutesToHhmm(s5))
                        .endTime(minutesToHhmm(e5))
                        .rawHours(raw)
                        .factor(factor)
                        .weightedHours(Math.round(raw * factor * 100.0) / 100.0)
                        .shiftType("day")
                        .shiftName("Ban ngày")
                        .build());
            }
        }

        return segments;
    }

    public List<Segment> generateContinuousSegments(
            LocalDate startDate,
            LocalDate endDate,
            String startTime,
            String endTime,
            boolean isHoliday) {

        int startMin = parseTimeMinutes(startTime);
        int endMin = parseTimeMinutes(endTime);

        if (endDate == null || !endDate.isAfter(startDate)) {
            if (endMin <= startMin) {
                endMin += 24 * 60;
            }
            boolean isWk = startDate.getDayOfWeek() == DayOfWeek.SATURDAY || startDate.getDayOfWeek() == DayOfWeek.SUNDAY;
            return splitSegments(startMin, endMin, isHoliday, isWk, startDate);
        }

        List<LocalDate> dateList = new ArrayList<>();
        LocalDate cur = startDate;
        while (!cur.isAfter(endDate)) {
            dateList.add(cur);
            cur = cur.plusDays(1);
        }

        List<Segment> allSegments = new ArrayList<>();
        for (int idx = 0; idx < dateList.size(); idx++) {
            LocalDate dt = dateList.get(idx);
            boolean isWk = dt.getDayOfWeek() == DayOfWeek.SATURDAY || dt.getDayOfWeek() == DayOfWeek.SUNDAY;
            int regStart = 8 * 60;
            int regEnd = 18 * 60 + 30;

            if (idx == 0) {
                allSegments.addAll(splitSegments(startMin, 1440, isHoliday, isWk, dt));
            } else if (idx == dateList.size() - 1) {
                int earlyEnd = Math.min(endMin, regStart);
                if (earlyEnd > 0) {
                    allSegments.addAll(splitSegments(0, earlyEnd, isHoliday, isWk, dt));
                }
                int resumeMin = (isWk || isHoliday) ? regStart : regEnd;
                if (endMin > resumeMin) {
                    allSegments.addAll(splitSegments(resumeMin, endMin, isHoliday, isWk, dt));
                }
            } else {
                allSegments.addAll(splitSegments(0, regStart, isHoliday, isWk, dt));
                int resumeMin = (isWk || isHoliday) ? regStart : regEnd;
                allSegments.addAll(splitSegments(resumeMin, 1440, isHoliday, isWk, dt));
            }
        }

        return allSegments;
    }

    public void validateOtTime(String start, String end, LocalDate workDate, boolean isHoliday) {
        int startMin = parseTimeMinutes(start);
        int endMin = parseTimeMinutes(end);

        if (endMin <= startMin) {
            endMin += 24 * 60;
        }

        boolean isWeekend = workDate.getDayOfWeek() == DayOfWeek.SATURDAY || workDate.getDayOfWeek() == DayOfWeek.SUNDAY;
        if (!isWeekend && !isHoliday) {
            if (startMin >= 8 * 60 && startMin < 18 * 60 + 30) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Từ 08:00 đến 18:30 là giờ làm việc chính thức (T2-T6). OT chỉ tính ngoài giờ hành chính.");
            }
        }

        double raw = calcRawHours(startMin, endMin);
        if (raw <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Thời gian OT phải lớn hơn 0.");
        }
        if (raw > 24) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OT không được quá 24 giờ.");
        }
    }
}
