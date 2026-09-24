package com.lexhive.libraryapp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.jayway.jsonpath.JsonPath;
import com.lexhive.libraryapp.repository.BookRepository;
import com.lexhive.libraryapp.repository.LoanRepository;
import com.lexhive.libraryapp.repository.MemberRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    BookRepository bookRepository;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    LoanRepository loanRepository;

    @BeforeEach
    void clean() {
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        memberRepository.deleteAll();
    }

    MockHttpServletResponse send(RequestBuilder request) throws Exception {
        return mockMvc.perform(request).andReturn().getResponse();
    }

    long populateMember(String name, String email) throws Exception {
        MockHttpServletResponse response = send(post("/api/members")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"%s","email":"%s"}
                        """.formatted(name, email)));
        assertThat(response.getStatus()).isEqualTo(201);
        return read(response, "$.id", Number.class).longValue();
    }

    long populateBook(String title, String isbn, int copies) throws Exception {
        MockHttpServletResponse response = send(post("/api/books")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"%s","author":"Author","isbn":"%s","total_copies":%d}
                        """.formatted(title, isbn, copies)));
        assertThat(response.getStatus()).isEqualTo(201);
        return read(response, "$.id", Number.class).longValue();
    }

    static RequestPostProcessor admin() {
        return httpBasic("admin@admin.com", "admin");
    }

    static RequestPostProcessor user1() {
        return httpBasic("razza@razza.com", "razza");
    }

    static RequestPostProcessor user2() {
        return httpBasic("rafif@rafif.com", "rafif");
    }

    static String code(MockHttpServletResponse response) throws Exception {
        return read(response, "$.code", String.class);
    }

    static <T> T read(MockHttpServletResponse response, String path, Class<T> type) throws Exception {
        return JsonPath.parse(response.getContentAsString()).read(path, type);
    }
}
