package com.example.kitchensink.web;

import com.example.kitchensink.domain.Member;
import com.example.kitchensink.dto.MemberRequest;
import com.example.kitchensink.dto.MemberResponse;
import com.example.kitchensink.service.MemberRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberRegistrationService service;

    public MemberController(MemberRegistrationService service) {
        this.service = service;
    }

    @GetMapping
    public List<MemberResponse> listAll() {
        return service.findAllOrderedByName().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<MemberResponse> register(@Valid @RequestBody MemberRequest request) {
        Member member = new Member(null, request.name(), request.email(), request.phoneNumber());
        Member saved = service.register(member);
        return ResponseEntity.ok(toResponse(saved));
    }

    private MemberResponse toResponse(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getName(),
                member.getEmail(),
                member.getPhoneNumber()
        );
    }
}
