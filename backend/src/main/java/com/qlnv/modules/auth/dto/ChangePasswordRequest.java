package com.qlnv.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    @NotBlank(message = "Mật khẩu cũ không được để trống")
    @JsonProperty("old_password")
    private String oldPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @JsonProperty("new_password")
    private String newPassword;
}
