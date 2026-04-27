package com.example.kitchensink.web;

import com.example.kitchensink.domain.Member;
import com.example.kitchensink.service.EmailAlreadyExistsException;
import com.example.kitchensink.service.MemberRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;


@WebMvcTest(MemberWebController.class)
class MemberWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberRegistrationService service;

    @Test
    void GET_root_returnsIndexViewWithFormAndMembers() throws Exception {
        when(service.findAllOrderedByName()).thenReturn(List.of(
            new Member("1", "Alice", "alice@example.com", "1234567890")
        ));

        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andExpect(model().attributeExists("form", "members"));
    }

    @Test
    void POST_validMember_redirectsToRoot() throws Exception {
        when(service.register(any())).thenReturn(
            new Member("1", "Alice", "alice@example.com", "1234567890"));

        mockMvc.perform(post("/")
                .param("name", "Alice")
                .param("email", "alice@example.com")
                .param("phoneNumber", "1234567890"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
    }

    @Test
    void POST_invalidEmail_returnsFormWithErrors() throws Exception {
        when(service.findAllOrderedByName()).thenReturn(List.of());

        mockMvc.perform(post("/")
                .param("name", "Alice")
                .param("email", "not-an-email")
                .param("phoneNumber", "1234567890"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andExpect(model().attributeHasFieldErrors("form", "email"));
    }

    @Test
    void POST_nameWithNumbers_returnsFormWithErrors() throws Exception {
        when(service.findAllOrderedByName()).thenReturn(List.of());

        mockMvc.perform(post("/")
                .param("name", "Alice123")
                .param("email", "alice@example.com")
                .param("phoneNumber", "1234567890"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andExpect(model().attributeHasFieldErrors("form", "name"));
    }

    @Test
    void POST_duplicateEmail_returnsFormWithEmailError() throws Exception {
        when(service.register(any())).thenThrow(
            new EmailAlreadyExistsException("Email already registered: alice@example.com"));
        when(service.findAllOrderedByName()).thenReturn(List.of());

        mockMvc.perform(post("/")
                .param("name", "Alice")
                .param("email", "alice@example.com")
                .param("phoneNumber", "1234567890"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andExpect(model().attributeHasFieldErrors("form", "email"));
    }
}
