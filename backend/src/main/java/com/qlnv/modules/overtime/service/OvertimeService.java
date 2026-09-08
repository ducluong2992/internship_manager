package com.qlnv.modules.overtime.service;

import com.qlnv.common.exception.ApiException;
import com.qlnv.modules.overtime.dto.OvertimeRequestDto;
import com.qlnv.modules.overtime.dto.OvertimeResponseDto;
import com.qlnv.modules.overtime.entity.OvertimeRequest;
import com.qlnv.modules.overtime.repository.OvertimeRequestRepository;
import com.qlnv.modules.overtime.repository.OvertimeSpecification;
import com.qlnv.modules.user.entity.User;
import com.qlnv.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
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
public class OvertimeService {

    private final OvertimeRequestRepository overtimeRepository;
    private final UserRepository userRepository;
    private final OvertimeCalculator calculator;

    // ─── Fast Lookups ───

    public OvertimeRequest findEntityById(Integer id) {
        return overtimeRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu OT với ID: " + id));
    }

    public List<OvertimeCalculator.Segment> preview(OvertimeRequestDto req) {
        calculator.validateOtTime(req.getStartTime(), req.getEndTime(), req.getWorkDate(), Boolean.TRUE.equals(req.getIsHoliday()));
        return calculator.generateContinuousSegments(
                req.getWorkDate(),
                req.getEndDate(),
                req.getStartTime(),
                req.getEndTime(),
                Boolean.TRUE.equals(req.getIsHoliday())
        );
    }

    @Transactional
    public List<OvertimeResponseDto.Response> createOvertime(Integer userId, OvertimeRequestDto req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        calculator.validateOtTime(req.getStartTime(), req.getEndTime(), req.getWorkDate(), Boolean.TRUE.equals(req.getIsHoliday()));
        List<OvertimeCalculator.Segment> segments = calculator.generateContinuousSegments(
                req.getWorkDate(),
                req.getEndDate(),
                req.getStartTime(),
                req.getEndTime(),
                Boolean.TRUE.equals(req.getIsHoliday())
        );

        String project = req.getProject() != null && !req.getProject().trim().isEmpty() ? req.getProject() : user.getProject();
        List<OvertimeRequest> createdList = new ArrayList<>();

        for (OvertimeCalculator.Segment seg : segments) {
            OvertimeRequest ot = OvertimeRequest.builder()
                    .userId(userId)
                    .project(project)
                    .workDate(seg.getWorkDate())
                    .startTime(seg.getStartTime())
                    .endTime(seg.getEndTime())
                    .rawHours(seg.getRawHours())
                    .factor(seg.getFactor())
                    .weightedHours(seg.getWeightedHours())
                    .reason(req.getReason())
                    .status("Pending")
                    .createdAt(LocalDateTime.now())
                    .build();
            createdList.add(ot);
        }

        createdList = overtimeRepository.saveAll(createdList);

        return createdList.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<OvertimeResponseDto.Response> getMyOvertime(Integer userId, LocalDate fromDate, LocalDate toDate, String status) {
        Specification<OvertimeRequest> spec = OvertimeSpecification.filter(userId, status, null, fromDate, toDate, null);
        return overtimeRepository.findAll(spec).stream()
                .sorted(Comparator.comparing(OvertimeRequest::getWorkDate).reversed()
                        .thenComparing(OvertimeRequest::getStartTime, Comparator.reverseOrder()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getMyStats(Integer userId, Integer month, Integer year) {
        LocalDate fromDate = null;
        LocalDate toDate = null;
        if (month != null && year != null) {
            YearMonth ym = YearMonth.of(year, month);
            fromDate = ym.atDay(1);
            toDate = ym.atEndOfMonth();
        }

        Specification<OvertimeRequest> spec = OvertimeSpecification.filter(userId, null, null, fromDate, toDate, null);
        List<OvertimeRequest> list = overtimeRepository.findAll(spec);

        long total = list.size();
        long pending = list.stream().filter(o -> "Pending".equalsIgnoreCase(o.getStatus())).count();
        long approved = list.stream().filter(o -> "Approved".equalsIgnoreCase(o.getStatus())).count();
        long rejected = list.stream().filter(o -> "Rejected".equalsIgnoreCase(o.getStatus())).count();
        double totalRaw = list.stream().mapToDouble(OvertimeRequest::getRawHours).sum();
        double totalWeighted = list.stream().mapToDouble(OvertimeRequest::getWeightedHours).sum();
        double approvedWeighted = list.stream()
                .filter(o -> "Approved".equalsIgnoreCase(o.getStatus()))
                .mapToDouble(OvertimeRequest::getWeightedHours).sum();

        User user = userRepository.findById(userId).orElse(null);
        boolean isStaffOnsite = user != null && user.getStaffCategory() != null && user.getStaffCategory().toLowerCase().contains("onsite");
        boolean isAdmin = user != null && ("admin".equalsIgnoreCase(user.getRole()) || "admin".equalsIgnoreCase(user.getUserType()));
        boolean hasProject = user != null && ((user.getProject() != null && !user.getProject().trim().isEmpty()) || (user.getBorrowProject() != null && !user.getBorrowProject().trim().isEmpty()));

        Map<String, Object> stats = new HashMap<>();
        stats.put("is_onsite", isStaffOnsite || isAdmin);
        stats.put("has_project", hasProject || isAdmin);
        stats.put("project", user != null ? (user.getProject() != null ? user.getProject() : user.getBorrowProject()) : "");
        stats.put("staff_category", user != null ? user.getStaffCategory() : "");
        stats.put("total_requests", total);
        stats.put("pending_count", pending);
        stats.put("approved_count", approved);
        stats.put("rejected_count", rejected);
        stats.put("total_raw_hours", Math.round(totalRaw * 100.0) / 100.0);
        stats.put("total_weighted_hours", Math.round(totalWeighted * 100.0) / 100.0);
        stats.put("approved_weighted_hours", Math.round(approvedWeighted * 100.0) / 100.0);
        return stats;
    }

    @Transactional
    public OvertimeResponseDto.Response updateOvertime(Integer userId, Integer otId, OvertimeRequestDto req, boolean isAdmin) {
        OvertimeRequest ot = findEntityById(otId);

        if (!isAdmin && !ot.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền sửa đơn OT này");
        }
        if (!isAdmin && !"Pending".equalsIgnoreCase(ot.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Chỉ có thể sửa đơn OT đang ở trạng thái Chờ duyệt (Pending)");
        }

        if (req.getWorkDate() != null) ot.setWorkDate(req.getWorkDate());
        if (req.getStartTime() != null) ot.setStartTime(req.getStartTime());
        if (req.getEndTime() != null) ot.setEndTime(req.getEndTime());
        if (req.getProject() != null) ot.setProject(req.getProject());
        if (req.getReason() != null) ot.setReason(req.getReason());

        calculator.validateOtTime(ot.getStartTime(), ot.getEndTime(), ot.getWorkDate(), Boolean.TRUE.equals(req.getIsHoliday()));
        int startMin = calculator.parseTimeMinutes(ot.getStartTime());
        int endMin = calculator.parseTimeMinutes(ot.getEndTime());
        if (endMin <= startMin) endMin += 24 * 60;

        double raw = calculator.calcRawHours(startMin, endMin);
        ot.setRawHours(raw);

        double factor = req.getFactor() != null ? req.getFactor() :
                calculator.getFactor(
                        Boolean.TRUE.equals(req.getIsHoliday()),
                        ot.getWorkDate().getDayOfWeek().getValue() >= 6,
                        endMin <= 22 * 60
                );
        ot.setFactor(factor);
        ot.setWeightedHours(Math.round(raw * factor * 100.0) / 100.0);

        return toResponse(overtimeRepository.save(ot));
    }

    @Transactional
    public void deleteOvertime(Integer userId, Integer otId, boolean isAdmin) {
        OvertimeRequest ot = findEntityById(otId);

        if (!isAdmin && !ot.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền xóa đơn OT này");
        }
        if (!isAdmin && !"Pending".equalsIgnoreCase(ot.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Chỉ có thể xóa đơn OT đang ở trạng thái Chờ duyệt (Pending)");
        }

        overtimeRepository.delete(ot);
    }

    // ─── Admin Operations ───

    public List<OvertimeResponseDto.Response> getAdminOvertimeList(
            String status, String project, LocalDate fromDate, LocalDate toDate, String keyword) {
        return getAdminOvertimeList(status, project, null, null, fromDate, toDate, keyword);
    }

    public List<OvertimeResponseDto.Response> getAdminOvertimeList(
            String status, String project, Integer month, Integer year, LocalDate fromDate, LocalDate toDate, String keyword) {

        if (fromDate == null && toDate == null && month != null && year != null) {
            YearMonth ym = YearMonth.of(year, month);
            fromDate = ym.atDay(1);
            toDate = ym.atEndOfMonth();
        }

        Specification<OvertimeRequest> spec = OvertimeSpecification.filter(null, status, project, fromDate, toDate, keyword);
        return overtimeRepository.findAll(spec).stream()
                .sorted(Comparator.comparing(OvertimeRequest::getWorkDate).reversed()
                        .thenComparing(OvertimeRequest::getStartTime, Comparator.reverseOrder()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getAdminOvertimeSummary(Integer month, Integer year, String project) {
        return getAdminOvertimeSummary(month, year, project, null);
    }

    public Map<String, Object> getAdminOvertimeSummary(Integer month, Integer year, String project, String keyword) {
        int m = (month != null) ? month : LocalDate.now().getMonthValue();
        int y = (year != null) ? year : LocalDate.now().getYear();
        YearMonth ym = YearMonth.of(y, m);
        LocalDate fromDate = ym.atDay(1);
        LocalDate toDate = ym.atEndOfMonth();
        int numDays = ym.lengthOfMonth();

        Specification<OvertimeRequest> spec = OvertimeSpecification.filter(null, "Approved", project, fromDate, toDate, keyword);
        List<OvertimeRequest> list = overtimeRepository.findAll(spec);

        long total = list.size();
        double totalRaw = list.stream().mapToDouble(OvertimeRequest::getRawHours).sum();
        double totalWeighted = list.stream().mapToDouble(OvertimeRequest::getWeightedHours).sum();

        // Group by user
        Map<Integer, List<OvertimeRequest>> byUser = list.stream()
                .collect(Collectors.groupingBy(OvertimeRequest::getUserId));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<Integer, List<OvertimeRequest>> entry : byUser.entrySet()) {
            List<OvertimeRequest> userOts = entry.getValue();
            if (userOts.isEmpty()) continue;
            User u = userOts.get(0).getUser();
            String empCode = (u != null && u.getEmployeeCode() != null) ? u.getEmployeeCode() : "NV" + entry.getKey();
            String fullName = (u != null && u.getFullName() != null) ? u.getFullName() : "Nhân viên";

            double userRaw = userOts.stream().mapToDouble(OvertimeRequest::getRawHours).sum();
            double userWeighted = userOts.stream().mapToDouble(OvertimeRequest::getWeightedHours).sum();

            Map<Integer, List<Map<String, Object>>> daysMap = new HashMap<>();
            for (OvertimeRequest ot : userOts) {
                if (ot.getWorkDate() == null) continue;
                int day = ot.getWorkDate().getDayOfMonth();
                Map<String, Object> dayItem = new HashMap<>();
                dayItem.put("id", ot.getId());
                dayItem.put("raw_hours", ot.getRawHours());
                dayItem.put("factor", ot.getFactor());
                dayItem.put("start_time", ot.getStartTime());
                dayItem.put("end_time", ot.getEndTime());
                dayItem.put("status", ot.getStatus());
                daysMap.computeIfAbsent(day, k -> new ArrayList<>()).add(dayItem);
            }

            Map<String, Object> row = new HashMap<>();
            row.put("user_id", entry.getKey());
            row.put("employee_code", empCode);
            row.put("full_name", fullName);
            row.put("total_raw", Math.round(userRaw * 100.0) / 100.0);
            row.put("total_weighted", Math.round(userWeighted * 100.0) / 100.0);
            row.put("days", daysMap);
            rows.add(row);
        }

        // Sort rows by employee_code
        rows.sort((r1, r2) -> String.valueOf(r1.get("employee_code")).compareToIgnoreCase(String.valueOf(r2.get("employee_code"))));

        Map<String, Object> result = new HashMap<>();
        result.put("month", m);
        result.put("year", y);
        result.put("num_days", numDays);
        result.put("rows", rows);
        result.put("total_count", total);
        result.put("total_raw_hours", Math.round(totalRaw * 100.0) / 100.0);
        result.put("total_weighted_hours", Math.round(totalWeighted * 100.0) / 100.0);
        return result;
    }

    @Transactional
    public OvertimeResponseDto.Response approveOvertime(Integer otId, Integer adminId) {
        OvertimeRequest ot = findEntityById(otId);
        ot.setStatus("Approved");
        ot.setApprovedBy(adminId);
        ot.setApprovedAt(LocalDateTime.now());
        ot.setRejectReason(null);
        return toResponse(overtimeRepository.save(ot));
    }

    @Transactional
    public OvertimeResponseDto.Response rejectOvertime(Integer otId, Integer adminId, String rejectReason) {
        OvertimeRequest ot = findEntityById(otId);
        ot.setStatus("Rejected");
        ot.setApprovedBy(adminId);
        ot.setApprovedAt(LocalDateTime.now());
        ot.setRejectReason(rejectReason != null ? rejectReason : "Từ chối bởi Admin");
        return toResponse(overtimeRepository.save(ot));
    }

    @Transactional
    public OvertimeResponseDto.Response resetPendingOvertime(Integer otId) {
        OvertimeRequest ot = findEntityById(otId);
        ot.setStatus("Pending");
        ot.setApprovedBy(null);
        ot.setApprovedAt(null);
        ot.setRejectReason(null);
        return toResponse(overtimeRepository.save(ot));
    }

    @Transactional
    public Map<String, Object> approveSelected(List<Integer> ids, Integer adminId) {
        if (ids == null || ids.isEmpty()) {
            return Map.of("message", "Danh sách rỗng", "count", 0);
        }

        List<OvertimeRequest> list = overtimeRepository.findAllById(ids);
        int count = 0;
        for (OvertimeRequest ot : list) {
            ot.setStatus("Approved");
            ot.setApprovedBy(adminId);
            ot.setApprovedAt(LocalDateTime.now());
            ot.setRejectReason(null);
            count++;
        }
        overtimeRepository.saveAll(list);
        return Map.of("message", "Đã duyệt thành công " + count + " đơn OT", "approved_count", count);
    }

    @Transactional
    public Map<String, Object> approveAllPending(Integer adminId, Integer month, Integer year, String project) {
        LocalDate fromDate = null;
        LocalDate toDate = null;
        if (month != null && year != null) {
            YearMonth ym = YearMonth.of(year, month);
            fromDate = ym.atDay(1);
            toDate = ym.atEndOfMonth();
        }

        Specification<OvertimeRequest> spec = OvertimeSpecification.filter(null, "Pending", project, fromDate, toDate, null);
        List<OvertimeRequest> list = overtimeRepository.findAll(spec);
        int count = 0;
        for (OvertimeRequest ot : list) {
            ot.setStatus("Approved");
            ot.setApprovedBy(adminId);
            ot.setApprovedAt(LocalDateTime.now());
            ot.setRejectReason(null);
            count++;
        }
        overtimeRepository.saveAll(list);
        return Map.of("message", "Đã duyệt toàn bộ " + count + " đơn OT chờ duyệt", "approved_count", count);
    }

    // ─── Convert Entity to DTO ───

    public OvertimeResponseDto.Response toResponse(OvertimeRequest ot) {
        if (ot == null) return null;

        String empCode = null;
        String fullName = null;
        if (ot.getUser() != null) {
            empCode = ot.getUser().getEmployeeCode();
            fullName = ot.getUser().getFullName();
        } else if (ot.getUserId() != null) {
            Optional<User> uOpt = userRepository.findById(ot.getUserId());
            if (uOpt.isPresent()) {
                empCode = uOpt.get().getEmployeeCode();
                fullName = uOpt.get().getFullName();
            }
        }

        return OvertimeResponseDto.Response.builder()
                .id(ot.getId())
                .userId(ot.getUserId())
                .employeeCode(empCode)
                .fullName(fullName)
                .project(ot.getProject())
                .workDate(ot.getWorkDate())
                .startTime(ot.getStartTime())
                .endTime(ot.getEndTime())
                .rawHours(ot.getRawHours())
                .factor(ot.getFactor())
                .weightedHours(ot.getWeightedHours())
                .reason(ot.getReason())
                .status(ot.getStatus())
                .rejectReason(ot.getRejectReason())
                .approvedBy(ot.getApprovedBy())
                .approvedAt(ot.getApprovedAt())
                .createdAt(ot.getCreatedAt())
                .build();
    }
}
