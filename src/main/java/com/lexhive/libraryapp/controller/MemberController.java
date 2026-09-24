package com.lexhive.libraryapp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lexhive.libraryapp.dto.ApiResponse;
import com.lexhive.libraryapp.dto.CreateMemberRequest;
import com.lexhive.libraryapp.dto.MemberResponse;
import com.lexhive.libraryapp.service.MemberService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/members")
@RolesAllowed("ADMIN")
@Tag(name = "Members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    @Operation(summary = "List library members")
    public ApiResponse<List<MemberResponse>> getMemberList() {
        List<MemberResponse> members = memberService.getMembers();

        return ApiResponse.of(members);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one member")
    public ApiResponse<MemberResponse> getMember(@PathVariable long id) {
        MemberResponse member = memberService.getMember(id);

        return ApiResponse.of(member);
    }

    @PostMapping
    @Operation(summary = "Register a member")
    public ResponseEntity<ApiResponse<MemberResponse>> createMember(@Valid @RequestBody CreateMemberRequest request) {
        MemberResponse created = memberService.createMember(request);
        ApiResponse<MemberResponse> body = ApiResponse.of(created);

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
