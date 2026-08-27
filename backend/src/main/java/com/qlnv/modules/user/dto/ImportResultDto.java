package com.qlnv.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportResultDto {

    @Builder.Default
    @JsonProperty("imported_count")
    private Integer importedCount = 0;

    @Builder.Default
    @JsonProperty("skipped_count")
    private Integer skippedCount = 0;

    @Builder.Default
    @JsonProperty("updated_count")
    private Integer updatedCount = 0;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<Object> details = new ArrayList<>();
}
