package com.lexhive.libraryapp.dto;

import com.lexhive.libraryapp.entity.Member;

public record MemberResponse(Long id, String name, String email) {

    public static MemberResponse fromEntity(Member member) {
        return new MemberResponse(member.getId(), member.getName(), member.getEmail());
    }
}
