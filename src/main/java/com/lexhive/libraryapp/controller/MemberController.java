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

import com.lexhive.libraryapp.dto.CreateMemberRequest;
import com.lexhive.libraryapp.dto.MemberResponse;
import com.lexhive.libraryapp.service.MemberService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/members")
@Tag(name = "Members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    @Operation(summary = "List library members")
    public List<MemberResponse> getMemberList() {
        return memberService.getMembers();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one member")
    public MemberResponse getMember(@PathVariable long id) {
        return memberService.getMember(id);
    }

    @PostMapping
    @Operation(summary = "Register a member")
    public ResponseEntity<MemberResponse> createMember(@Valid @RequestBody CreateMemberRequest request) {
        MemberResponse body = memberService.createMember(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
