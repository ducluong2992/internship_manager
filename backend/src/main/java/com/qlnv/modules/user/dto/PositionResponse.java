package com.qlnv.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionResponse {
    private Integer id;
    private String name;
    
    @JsonProperty("is_manager")
    private Boolean isManager;
}
