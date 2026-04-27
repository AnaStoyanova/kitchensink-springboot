package com.example.kitchensink.web;

import com.example.kitchensink.domain.Member;
import com.example.kitchensink.dto.MemberRequest;
import com.example.kitchensink.service.EmailAlreadyExistsException;
import com.example.kitchensink.service.MemberRegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberRegistrationService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void GET_member_byId_returns200() throws Exception {
        when(service.findById("abc123")).thenReturn(
            Optional.of(new Member("abc123", "Alice", "alice@example.com", "1234567890")));

        mockMvc.perform(get("/api/members/abc123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("abc123"))
            .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void GET_member_byId_notFound_returns404() throws Exception {
        when(service.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/members/nonexistent"))
            .andExpect(status().isNotFound());
    }

    @Test
    void GET_members_returnsJsonList() throws Exception {
        when(service.findAllOrderedByName()).thenReturn(List.of(
            new Member("1", "Alice", "alice@example.com", "1234567890")
        ));

        mockMvc.perform(get("/api/members"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Alice"))
            .andExpect(jsonPath("$[0].email").value("alice@example.com"));
    }

    @Test
    void POST_validMember_returns200() throws Exception {
        var request = new MemberRequest("Alice", "alice@example.com", "1234567890");
        when(service.register(any())).thenReturn(
            new Member("1", "Alice", "alice@example.com", "1234567890"));

        mockMvc.perform(post("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("1"))
            .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void POST_invalidEmail_returns400() throws Exception {
        var request = new MemberRequest("Alice", "not-an-email", "1234567890");

        mockMvc.perform(post("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void POST_duplicateEmail_returns409() throws Exception {
        var request = new MemberRequest("Alice", "alice@example.com", "1234567890");
        when(service.register(any())).thenThrow(
            new EmailAlreadyExistsException("Email already registered: alice@example.com"));

        mockMvc.perform(post("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    void POST_missingName_returns400() throws Exception {
        var body = """
            {"email":"alice@example.com","phoneNumber":"1234567890"}
            """;

        mockMvc.perform(post("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void POST_invalidPhone_returns400WithCustomMessage() throws Exception {
        var request = new MemberRequest("Alice", "alice@example.com", "123");

        mockMvc.perform(post("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.phoneNumber").value("Invalid phone number. Must be 10-12 digits."));
    }
}
