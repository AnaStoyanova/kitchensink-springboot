package com.example.kitchensink.contract;

import com.example.kitchensink.domain.Member;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

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
        mongoTemplate.dropCollection(Member.class);
        mongoTemplate.getCollection("members")
            .createIndex(new Document("email", 1), new IndexOptions().unique(true));
    }

    @Test
    void GET_member_byId_returns200() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var created = restTemplate.postForEntity("/rest/members",
            new HttpEntity<>("""
                {"name":"Alice","email":"alice@example.com","phoneNumber":"1234567890"}
                """, headers), Map.class);
        var id = (String) created.getBody().get("id");

        var response = restTemplate.getForEntity("/rest/members/" + id, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("name", "Alice");
    }

    @Test
    void GET_member_byLegacyId_returns200() {
        var member = new Member("Legacy Alice", "legacy@example.com", "1234567890");
        member.setLegacyId(42L);
        mongoTemplate.save(member);

        var response = restTemplate.getForEntity("/rest/members/42", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("name", "Legacy Alice");
    }

    @Test
    void GET_member_byId_notFound_returns404() {
        var response = restTemplate.getForEntity("/rest/members/000000000000000000000000", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void GET_members_empty_returns200WithEmptyList() {
        var response = restTemplate.getForEntity("/rest/members", List.class);
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
            "/rest/members", new HttpEntity<>(body, headers), Map.class);
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
            "/rest/members", new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void POST_duplicateEmail_returns409() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var body = """
            {"name":"Alice","email":"alice@example.com","phoneNumber":"1234567890"}
            """;
        restTemplate.postForEntity("/rest/members", new HttpEntity<>(body, headers), Map.class);
        var response = restTemplate.postForEntity(
            "/rest/members", new HttpEntity<>(body, headers), Map.class);
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
            "/rest/members", new HttpEntity<>(body, headers), Map.class);
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
            "/rest/members", new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void GET_members_afterTwoPosts_returnsMembersOrderedByName() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/rest/members",
            new HttpEntity<>("""
                {"name":"Zara","email":"zara@example.com","phoneNumber":"1234567890"}
                """, headers), Map.class);
        restTemplate.postForEntity("/rest/members",
            new HttpEntity<>("""
                {"name":"Alice","email":"alice@example.com","phoneNumber":"0987654321"}
                """, headers), Map.class);

        var response = restTemplate.getForEntity("/rest/members", List.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var names = ((List<Map<String, Object>>) response.getBody())
            .stream().map(m -> (String) m.get("name")).toList();
        assertThat(names).containsExactly("Alice", "Zara");
    }
}
