package com.lexhive.libraryapp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lexhive.libraryapp.dto.CreateMemberRequest;
import com.lexhive.libraryapp.dto.MemberResponse;
import com.lexhive.libraryapp.entity.Member;
import com.lexhive.libraryapp.exception.CustomException;
import com.lexhive.libraryapp.exception.CustomException.Code;
import com.lexhive.libraryapp.repository.MemberRepository;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers() {
        return memberRepository.findAllByOrderByNameAscIdAsc().stream()
                .map(MemberResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public MemberResponse getMember(long id) {
        return MemberResponse.fromEntity(memberRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound(Code.MEMBER_NOT_FOUND, "Member " + id + " was not found")));
    }

    @Transactional
    public MemberResponse createMember(CreateMemberRequest request) {
        String email = request.email().trim().toLowerCase();

        if (memberRepository.existsByEmail(email)) {
            throw CustomException.conflict(Code.EMAIL_ALREADY_EXISTS, "Email " + email + " is already registered");
        }

        Member member = memberRepository.save(new Member(request.name().trim(), email));
        return MemberResponse.fromEntity(member);
    }
}
