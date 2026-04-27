package com.example.kitchensink.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MemberResponse(
    @Schema(description = "Unique identifier (MongoDB ObjectId)", example = "60d5ecb3b1f3b0001f8e4d56")
    String id,

    @Schema(description = "Full name of the member", example = "Jane Doe")
    String name,

    @Schema(description = "Unique email address", example = "jane.doe@example.com")
    String email,

    @Schema(description = "Phone number (10-12 digits)", example = "1234567890")
    String phoneNumber
) {}
