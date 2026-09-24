package com.lexhive.libraryapp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class MemberControllerTest extends ControllerIntegrationTest {

    @Test
    @DisplayName("When anonymous request, must got unauthorized error")
    void anonymousRequestIsUnauthorized() throws Exception {
        var response = send(get("/api/members"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(code(response)).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("When member tries to request list members, must got forbidden error")
    void memberCannotListMembers() throws Exception {
        var response = send(get("/api/members").with(user1()));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(code(response)).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("When admin tries to register a member, must be successful")
    void adminCanRegisterAMember() throws Exception {
        var created = send(post("/api/members")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Razza","email":"razza@library.test"}
                        """));

        assertThat(created.getStatus()).isEqualTo(201);
        assertThat(read(created, "$.data.email", String.class)).isEqualTo("razza@library.test");

        var listed = send(get("/api/members").with(admin()));

        assertThat(listed.getStatus()).isEqualTo(200);
        assertThat(read(listed, "$.data.length()", Integer.class)).isEqualTo(1);
        assertThat(read(listed, "$.data[0].name", String.class)).isEqualTo("Razza");
    }

    @Test
    @DisplayName("When admin tries to get one member, must be successful")
    void adminCanGetOneMember() throws Exception {
        long memberId = populateMember("Razza", "razza@library.test");

        var response = send(get("/api/members/{id}", memberId).with(admin()));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(read(response, "$.data.id", Number.class).longValue()).isEqualTo(memberId);
        assertThat(read(response, "$.data.name", String.class)).isEqualTo("Razza");
        assertThat(read(response, "$.data.email", String.class)).isEqualTo("razza@library.test");
    }

    @Test
    @DisplayName("When admin tries to register a member with duplicate email, must be rejected")
    void duplicateEmailIsRejected() throws Exception {
        populateMember("Razza", "razza@library.test");

        var response = send(post("/api/members")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Other","email":"razza@library.test"}
                        """));

        assertThat(response.getStatus()).isEqualTo(422);
        assertThat(code(response)).isEqualTo("EMAIL_ALREADY_EXISTS");
    }
}
