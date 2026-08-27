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

        Map<String, Object> stats = new HashMap<>();
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

        Specification<OvertimeRequest> spec = OvertimeSpecification.filter(null, status, project, fromDate, toDate, keyword);
        return overtimeRepository.findAll(spec).stream()
                .sorted(Comparator.comparing(OvertimeRequest::getWorkDate).reversed()
                        .thenComparing(OvertimeRequest::getStartTime, Comparator.reverseOrder()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getAdminOvertimeSummary(Integer month, Integer year, String project) {
        LocalDate fromDate = null;
        LocalDate toDate = null;
        if (month != null && year != null) {
            YearMonth ym = YearMonth.of(year, month);
            fromDate = ym.atDay(1);
            toDate = ym.atEndOfMonth();
        }

        Specification<OvertimeRequest> spec = OvertimeSpecification.filter(null, null, project, fromDate, toDate, null);
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

        // Project summary breakdown
        Map<String, Map<String, Object>> projectSummary = new HashMap<>();
        for (OvertimeRequest ot : list) {
            String pName = ot.getProject() != null ? ot.getProject() : "Không xác định";
            var m = projectSummary.computeIfAbsent(pName, k -> {
                Map<String, Object> item = new HashMap<>();
                item.put("project", k);
                item.put("count", 0L);
                item.put("total_raw", 0.0);
                item.put("total_weighted", 0.0);
                return item;
            });
            m.put("count", ((long) m.get("count")) + 1);
            m.put("total_raw", ((double) m.get("total_raw")) + ot.getRawHours());
            m.put("total_weighted", ((double) m.get("total_weighted")) + ot.getWeightedHours());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total_count", total);
        result.put("pending_count", pending);
        result.put("approved_count", approved);
        result.put("rejected_count", rejected);
        result.put("total_raw_hours", Math.round(totalRaw * 100.0) / 100.0);
        result.put("total_weighted_hours", Math.round(totalWeighted * 100.0) / 100.0);
        result.put("approved_weighted_hours", Math.round(approvedWeighted * 100.0) / 100.0);
        result.put("project_summary", projectSummary.values());
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
