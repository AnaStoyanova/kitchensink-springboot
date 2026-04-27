package com.example.kitchensink.service;

import com.example.kitchensink.domain.Member;
import com.example.kitchensink.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MemberRegistrationService {

    private final MemberRepository repository;

    public MemberRegistrationService(MemberRepository repository) {
        this.repository = repository;
    }

    public Member register(Member member) {
        if (repository.findByEmail(member.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException(
                "Email already registered: " + member.getEmail());
        }
        return repository.save(member);
    }

    public Optional<Member> findById(String id) {
        return repository.findById(id);
    }

    public List<Member> findAllOrderedByName() {
        return repository.findAllByOrderByNameAsc();
    }
}
