package com.qlnv.modules.schedule.controller;

import com.qlnv.common.security.UserPrincipal;
import com.qlnv.modules.schedule.dto.PeriodResponse;
import com.qlnv.modules.schedule.dto.ScheduleEntryDto;
import com.qlnv.modules.schedule.service.PeriodService;
import com.qlnv.modules.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final PeriodService periodService;

    @GetMapping("/periods")
    public ResponseEntity<List<PeriodResponse>> getPeriods() {
        return ResponseEntity.ok(periodService.getAllPeriods());
    }

    @GetMapping("/me")
    public ResponseEntity<List<ScheduleEntryDto.Response>> getMySchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, name = "period_id") Integer periodId) {
        return ResponseEntity.ok(scheduleService.getMySchedule(principal.getId(), periodId));
    }

    @PostMapping("/me")
    public ResponseEntity<Map<String, Object>> submitMySchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ScheduleEntryDto.Submit submitDto) {
        return ResponseEntity.ok(scheduleService.submitMySchedule(principal.getId(), submitDto));
    }
}
