package com.qlnv.modules.user.service;

import com.qlnv.common.exception.ApiException;
import com.qlnv.modules.user.dto.PositionResponse;
import com.qlnv.modules.user.entity.Position;
import com.qlnv.modules.user.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionRepository positionRepository;

    public List<PositionResponse> getAllPositions() {
        return positionRepository.findAllByOrderByNameAsc().stream()
                .map(p -> PositionResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .isManager(p.getIsManager())
                        .build())
                .collect(Collectors.toList());
    }

    public Position getById(Integer id) {
        return positionRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy chức danh"));
    }

    public Position getByName(String name) {
        return positionRepository.findByName(name).orElse(null);
    }
}
