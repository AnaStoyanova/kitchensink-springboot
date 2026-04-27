package com.example.kitchensink.contract;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberApiContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.getDb().drop();
    }

    @Test
    void GET_members_empty_returns200WithEmptyList() {
        var response = restTemplate.getForEntity("/api/members", List.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void POST_validMember_returns200() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var body = """
            {"name":"Alice","email":"alice@example.com","phoneNumber":"1234567890"}
            """;
        var response = restTemplate.postForEntity(
            "/api/members", new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void POST_invalidEmail_returns400() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var body = """
            {"name":"Alice","email":"not-an-email","phoneNumber":"1234567890"}
            """;
        var response = restTemplate.postForEntity(
            "/api/members", new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void POST_duplicateEmail_returns409() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var body = """
            {"name":"Alice","email":"alice@example.com","phoneNumber":"1234567890"}
            """;
        restTemplate.postForEntity("/api/members", new HttpEntity<>(body, headers), Map.class);
        var response = restTemplate.postForEntity(
            "/api/members", new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void POST_missingName_returns400() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var body = """
            {"email":"alice@example.com","phoneNumber":"1234567890"}
            """;
        var response = restTemplate.postForEntity(
            "/api/members", new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void POST_shortPhoneNumber_returns400() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var body = """
            {"name":"Alice","email":"alice@example.com","phoneNumber":"123"}
            """;
        var response = restTemplate.postForEntity(
            "/api/members", new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void GET_members_afterTwoPosts_returnsMembersOrderedByName() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/members",
            new HttpEntity<>("""
                {"name":"Zara","email":"zara@example.com","phoneNumber":"1234567890"}
                """, headers), Map.class);
        restTemplate.postForEntity("/api/members",
            new HttpEntity<>("""
                {"name":"Alice","email":"alice@example.com","phoneNumber":"0987654321"}
                """, headers), Map.class);

        var response = restTemplate.getForEntity("/api/members", List.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var names = ((List<Map<String, Object>>) response.getBody())
            .stream().map(m -> (String) m.get("name")).toList();
        assertThat(names).containsExactly("Alice", "Zara");
    }
}
