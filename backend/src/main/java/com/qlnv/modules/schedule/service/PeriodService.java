package com.qlnv.modules.schedule.service;

import com.qlnv.common.exception.ApiException;
import com.qlnv.modules.schedule.dto.PeriodRequestDto;
import com.qlnv.modules.schedule.dto.PeriodResponse;
import com.qlnv.modules.schedule.entity.SchedulePeriod;
import com.qlnv.modules.schedule.repository.SchedulePeriodRepository;
import com.qlnv.modules.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PeriodService {

    private final SchedulePeriodRepository periodRepository;
    private final ScheduleRepository scheduleRepository;

    public List<PeriodResponse> getAllPeriods() {
        return periodRepository.findAllByOrderByYearDescMonthDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public SchedulePeriod getEntityById(Integer id) {
        return periodRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy kỳ đăng ký lịch"));
    }

    public PeriodResponse getById(Integer id) {
        return toResponse(getEntityById(id));
    }

    @Transactional
    public PeriodResponse createPeriod(PeriodRequestDto req) {
        if (periodRepository.findByMonthAndYear(req.getMonth(), req.getYear()).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Kỳ lịch tháng " + req.getMonth() + "/" + req.getYear() + " đã tồn tại");
        }

        SchedulePeriod period = SchedulePeriod.builder()
                .month(req.getMonth())
                .year(req.getYear())
                .openDate(req.getOpenDate())
                .closeDate(req.getCloseDate())
                .status(req.getStatus() != null ? req.getStatus() : "open")
                .build();

        return toResponse(periodRepository.save(period));
    }

    @Transactional
    public PeriodResponse updatePeriod(Integer id, PeriodRequestDto req) {
        SchedulePeriod period = getEntityById(id);

        if (req.getMonth() != null) period.setMonth(req.getMonth());
        if (req.getYear() != null) period.setYear(req.getYear());
        if (req.getOpenDate() != null) period.setOpenDate(req.getOpenDate());
        if (req.getCloseDate() != null) period.setCloseDate(req.getCloseDate());
        if (req.getStatus() != null) period.setStatus(req.getStatus());

        return toResponse(periodRepository.save(period));
    }

    @Transactional
    public void deletePeriod(Integer id) {
        SchedulePeriod period = getEntityById(id);
        scheduleRepository.deleteByPeriodId(id);
        periodRepository.delete(period);
    }

    public PeriodResponse toResponse(SchedulePeriod p) {
        if (p == null) return null;
        return PeriodResponse.builder()
                .id(p.getId())
                .month(p.getMonth())
                .year(p.getYear())
                .openDate(p.getOpenDate())
                .closeDate(p.getCloseDate())
                .status(p.getStatus())
                .build();
    }
}
