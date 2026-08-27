package com.qlnv.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagerResponse {
    private String username;
    
    @JsonProperty("full_name")
    private String fullName;
    
    private String position;
}
