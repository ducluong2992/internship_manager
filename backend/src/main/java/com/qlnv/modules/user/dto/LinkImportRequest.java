package com.qlnv.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkImportRequest {
    private String url;

    @JsonProperty("sheet_name")
    private String sheetName;
}
