package com.example.kitchensink.web;

import com.example.kitchensink.domain.Member;
import com.example.kitchensink.service.MemberRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberRegistrationService service;

    public MemberController(MemberRegistrationService service) {
        this.service = service;
    }

    @GetMapping
    public List<Member> listAll() {
        return service.findAllOrderedByName();
    }

    @PostMapping
    public ResponseEntity<Member> register(@Valid @RequestBody Member member) {
        return ResponseEntity.ok(service.register(member));
    }
}
