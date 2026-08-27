package com.qlnv.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @Builder.Default
    @JsonProperty("token_type")
    private String tokenType = "bearer";

    private String role;

    @JsonProperty("user_id")
    private Integer userId;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("user_type")
    private String userType;
}
