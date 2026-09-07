package com.qlnv.modules.schedule.service;

import com.qlnv.common.exception.ApiException;
import com.qlnv.modules.schedule.dto.ScheduleEntryDto;
import com.qlnv.modules.schedule.entity.Schedule;
import com.qlnv.modules.schedule.entity.SchedulePeriod;
import com.qlnv.modules.schedule.repository.SchedulePeriodRepository;
import com.qlnv.modules.schedule.repository.ScheduleRepository;
import com.qlnv.modules.user.entity.User;
import com.qlnv.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final SchedulePeriodRepository periodRepository;
    private final UserRepository userRepository;

    public List<ScheduleEntryDto.Response> getMySchedule(Integer userId, Integer periodId) {
        if (periodId == null) {
            Optional<SchedulePeriod> openPeriod = periodRepository.findFirstByStatusOrderByIdDesc("open");
            if (openPeriod.isEmpty()) {
                return Collections.emptyList();
            }
            periodId = openPeriod.get().getId();
        }

        return scheduleRepository.findByPeriodIdAndUserId(periodId, userId).stream()
                .map(s -> ScheduleEntryDto.Response.builder()
                        .id(s.getId())
                        .periodId(s.getPeriodId())
                        .userId(s.getUserId())
                        .workDay(s.getWorkDay())
                        .shift(s.getShift())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> submitMySchedule(Integer userId, ScheduleEntryDto.Submit submitDto) {
        if (submitDto.getPeriodId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Thiếu ID kỳ lịch");
        }

        SchedulePeriod period = periodRepository.findById(submitDto.getPeriodId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy kỳ đăng ký lịch"));

        if (!"open".equalsIgnoreCase(period.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Kỳ đăng ký lịch này hiện đang đóng.");
        }

        // Remove old entries
        scheduleRepository.deleteByPeriodIdAndUserId(submitDto.getPeriodId(), userId);

        if (submitDto.getEntries() != null) {
            List<Schedule> newEntries = new ArrayList<>();
            for (ScheduleEntryDto.Entry entry : submitDto.getEntries()) {
                if (entry.getShift() != null && !entry.getShift().trim().isEmpty() && !"None".equalsIgnoreCase(entry.getShift())) {
                    Schedule s = Schedule.builder()
                            .periodId(submitDto.getPeriodId())
                            .userId(userId)
                            .workDay(entry.getWorkDay())
                            .shift(entry.getShift().trim().toUpperCase())
                            .createdAt(LocalDateTime.now())
                            .build();
                    newEntries.add(s);
                }
            }
            scheduleRepository.saveAll(newEntries);
        }

        return Map.of("message", "Đăng ký lịch thành công", "status", "success");
    }

    public Map<String, Object> getAdminScheduleMatrix(Integer periodId, Integer month, Integer year, String project, String position, String keyword) {
        SchedulePeriod period = null;
        if (month != null && year != null) {
            period = periodRepository.findByMonthAndYear(month, year).orElse(null);
        } else if (periodId != null) {
            period = periodRepository.findById(periodId).orElse(null);
        } else {
            period = periodRepository.findFirstByStatusOrderByIdDesc("open")
                    .orElseGet(() -> periodRepository.findAllByOrderByYearDescMonthDesc().stream().findFirst().orElse(null));
        }

        if (period == null) {
            Map<String, Object> emptyRes = new HashMap<>();
            emptyRes.put("period", null);
            emptyRes.put("rows", Collections.emptyList());
            emptyRes.put("users", Collections.emptyList());
            emptyRes.put("days_in_month", 0);
            return emptyRes;
        }

        int pYear = period.getYear();
        int pMonth = period.getMonth();
        int daysInMonth = YearMonth.of(pYear, pMonth).lengthOfMonth();

        List<User> allUsers = userRepository.findByUserType("intern");
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            allUsers = allUsers.stream().filter(u ->
                (u.getFullName() != null && u.getFullName().toLowerCase().contains(kw)) ||
                (u.getEmployeeCode() != null && u.getEmployeeCode().toLowerCase().contains(kw))
            ).collect(Collectors.toList());
        }
        if (project != null && !project.trim().isEmpty()) {
            allUsers = allUsers.stream().filter(u -> project.equalsIgnoreCase(u.getProject())).collect(Collectors.toList());
        }
        if (position != null && !position.trim().isEmpty()) {
            allUsers = allUsers.stream().filter(u -> position.equalsIgnoreCase(u.getPosition())).collect(Collectors.toList());
        }

        allUsers.sort(Comparator.comparing(User::getEmployeeCode, Comparator.nullsLast(Comparator.naturalOrder())));

        // Fetch all schedules for this period
        List<Schedule> periodSchedules = scheduleRepository.findByPeriodId(period.getId());
        Map<Integer, List<Schedule>> userSchedulesMap = new HashMap<>();
        Map<Integer, Map<Integer, String>> userDayShiftMap = new HashMap<>();

        for (Schedule s : periodSchedules) {
            if (s.getUserId() != null) {
                userSchedulesMap.computeIfAbsent(s.getUserId(), k -> new ArrayList<>()).add(s);
                if (s.getWorkDay() != null) {
                    int day = s.getWorkDay().getDayOfMonth();
                    userDayShiftMap.computeIfAbsent(s.getUserId(), k -> new HashMap<>()).put(day, s.getShift());
                }
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (User u : allUsers) {
            List<Schedule> uScheds = userSchedulesMap.getOrDefault(u.getId(), Collections.emptyList());
            Map<Integer, String> dayShifts = userDayShiftMap.getOrDefault(u.getId(), Collections.emptyMap());

            List<Map<String, Object>> schedList = new ArrayList<>();
            double totalSessions = 0;
            int sCount = 0, cCount = 0, scCount = 0;

            for (Schedule s : uScheds) {
                Map<String, Object> sm = new HashMap<>();
                sm.put("id", s.getId());
                sm.put("work_day", s.getWorkDay() != null ? s.getWorkDay().toString() : "");
                sm.put("shift", s.getShift());
                schedList.add(sm);

                if ("SC".equalsIgnoreCase(s.getShift())) {
                    totalSessions += 1.0;
                    scCount++;
                } else if ("S".equalsIgnoreCase(s.getShift())) {
                    totalSessions += 0.5;
                    sCount++;
                } else if ("C".equalsIgnoreCase(s.getShift())) {
                    totalSessions += 0.5;
                    cCount++;
                }
            }

            Map<String, Object> uMap = new HashMap<>();
            uMap.put("user_id", u.getId());
            uMap.put("employee_code", u.getEmployeeCode());
            uMap.put("full_name", u.getFullName());
            uMap.put("project", u.getProject() != null ? u.getProject() : "");
            uMap.put("position", u.getPosition() != null ? u.getPosition() : "");
            uMap.put("total_sessions", totalSessions);
            uMap.put("schedules", schedList);
            uMap.put("shifts", dayShifts);
            uMap.put("total_s", sCount);
            uMap.put("total_c", cCount);
            uMap.put("total_sc", scCount);
            uMap.put("total_days", sCount + cCount + scCount);

            rows.add(uMap);
        }

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> periodMap = new HashMap<>();
        periodMap.put("id", period.getId());
        periodMap.put("month", period.getMonth());
        periodMap.put("year", period.getYear());
        periodMap.put("status", period.getStatus());
        periodMap.put("open_date", period.getOpenDate() != null ? period.getOpenDate().toString() : null);
        periodMap.put("close_date", period.getCloseDate() != null ? period.getCloseDate().toString() : null);

        result.put("period", periodMap);
        result.put("days_in_month", daysInMonth);
        result.put("rows", rows);
        result.put("users", rows);
        return result;
    }
}
