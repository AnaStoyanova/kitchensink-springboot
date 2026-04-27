package com.example.kitchensink.repository;

import com.example.kitchensink.domain.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import static org.assertj.core.api.Assertions.*;

@DataMongoTest
class MemberRepositoryTest {

    @Autowired
    private MemberRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void save_andFindByEmail_returnsCorrectMember() {
        repository.save(new Member(null, "John Doe", "john@example.com", "1234567890"));

        var found = repository.findByEmail("john@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Doe");
    }

    @Test
    void findAllByOrderByNameAsc_returnsMembersAlphabetically() {
        repository.save(new Member(null, "Zara", "zara@example.com", "1234567890"));
        repository.save(new Member(null, "Alice", "alice@example.com", "0987654321"));

        var members = repository.findAllByOrderByNameAsc();
        assertThat(members).extracting(Member::getName)
            .containsExactly("Alice", "Zara");
    }

    @Test
    void save_duplicateEmail_throwsException() {
        repository.save(new Member(null, "John", "john@example.com", "1234567890"));

        assertThatThrownBy(() ->
            repository.save(new Member(null, "Jane", "john@example.com", "0987654321"))
        ).isInstanceOf(Exception.class);
    }

    @Test
    void findByEmail_nonexistent_returnsEmpty() {
        var result = repository.findByEmail("nobody@example.com");
        assertThat(result).isEmpty();
    }
}
