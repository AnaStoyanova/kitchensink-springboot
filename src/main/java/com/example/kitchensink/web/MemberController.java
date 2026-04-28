package com.example.kitchensink.web;

import com.example.kitchensink.domain.Member;
import com.example.kitchensink.dto.MemberRequest;
import com.example.kitchensink.dto.MemberResponse;
import com.example.kitchensink.service.MemberRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rest/members")
@Tag(name = "Member Resource", description = "Endpoints for managing kitchensink members")
public class MemberController {

    private final MemberRegistrationService service;

    public MemberController(MemberRegistrationService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get member by ID", description = "Returns a single member by their MongoDB ObjectId")
    public ResponseEntity<MemberResponse> getById(@PathVariable String id) {
        return service.findById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "List all members", description = "Returns a list of all registered members ordered by name alphabetically")
    public List<MemberResponse> listAll() {
        return service.findAllOrderedByName().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @PostMapping
    @Operation(summary = "Register a new member", description = "Validates and saves a new member to the MongoDB database")
    public ResponseEntity<MemberResponse> register(@Valid @RequestBody MemberRequest request) {
        Member member = new Member(request.name(), request.email(), request.phoneNumber());
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
