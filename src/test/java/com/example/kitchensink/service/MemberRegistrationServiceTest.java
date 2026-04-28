package com.example.kitchensink.service;

import com.example.kitchensink.domain.Member;
import com.example.kitchensink.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class MemberRegistrationServiceTest {

    @Autowired
    private MemberRegistrationService service;

    @Autowired
    private MemberRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void register_validMember_persistsAndReturnsWithId() {
        var member = new Member("Jane Doe", "jane@example.com", "1234567890");
        var saved = service.register(member);

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findByEmail("jane@example.com")).isPresent();
    }

    @Test
    void register_duplicateEmail_throwsEmailAlreadyExistsException() {
        service.register(new Member("Jane", "jane@example.com", "1234567890"));

        assertThatThrownBy(() ->
            service.register(new Member("Joan", "jane@example.com", "0987654321"))
        ).isInstanceOf(EmailAlreadyExistsException.class)
         .hasMessageContaining("jane@example.com");
    }

    @Test
    void findAllOrderedByName_returnsMembersAlphabetically() {
        service.register(new Member("Zara", "zara@example.com", "1234567890"));
        service.register(new Member("Alice", "alice@example.com", "0987654321"));

        var members = service.findAllOrderedByName();
        assertThat(members).extracting(Member::getName)
            .containsExactly("Alice", "Zara");
    }
}
