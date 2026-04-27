package com.example.kitchensink.dto;

import com.example.kitchensink.validation.PhoneNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MemberRequest(
    @NotNull
    @Size(min = 1, max = 25)
    String name,

    @NotNull
    @NotEmpty
    @Email
    String email,

    @NotNull
    @PhoneNumber
    String phoneNumber
) {}
