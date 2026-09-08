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
public class ReconcilePreviewDto {
    private List<Map<String, Object>> updated;
    private List<Map<String, Object>> added;
    private List<Map<String, Object>> removed;

    @JsonProperty("unchanged_count")
    private int unchangedCount;

    @JsonProperty("format_warnings")
    private List<String> formatWarnings;
}
