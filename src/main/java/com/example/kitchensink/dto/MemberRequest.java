package com.example.kitchensink.dto;

import com.example.kitchensink.validation.PhoneNumber;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MemberRequest(
    @NotNull
    @Size(min = 1, max = 25)
    @Schema(description = "Full name of the member", example = "Jane Doe")
    String name,

    @NotNull
    @NotEmpty
    @Email
    @Schema(description = "Unique email address", example = "jane.doe@example.com")
    String email,

    @NotNull
    @PhoneNumber
    @Schema(description = "Phone number (10-12 digits)", example = "1234567890")
    String phoneNumber
) {}
