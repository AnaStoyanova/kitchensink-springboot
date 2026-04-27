package com.example.kitchensink.dto;

public record MemberResponse(
    String id,
    String name,
    String email,
    String phoneNumber
) {}
