package com.qlnv.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmImportRequest {
    private List<Map<String, Object>> updates;
    private List<Map<String, Object>> additions;

    @JsonProperty("delete_ids")
    private List<Integer> deleteIds;
}
