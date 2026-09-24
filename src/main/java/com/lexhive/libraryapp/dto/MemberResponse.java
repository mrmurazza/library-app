package com.lexhive.libraryapp.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.lexhive.libraryapp.entity.Member;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MemberResponse(Long id, String name, String email) {

    public static MemberResponse fromEntity(Member member) {
        return new MemberResponse(member.getId(), member.getName(), member.getEmail());
    }
}
