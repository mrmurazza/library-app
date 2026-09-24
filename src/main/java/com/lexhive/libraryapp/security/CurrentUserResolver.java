package com.lexhive.libraryapp.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.lexhive.libraryapp.entity.Member;
import com.lexhive.libraryapp.exception.CustomException;
import com.lexhive.libraryapp.exception.CustomException.Code;
import com.lexhive.libraryapp.repository.MemberRepository;

@Component
public class CurrentUserResolver {

    private final MemberRepository memberRepository;

    public CurrentUserResolver(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public CurrentUser from(Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        if (admin) {
            return new CurrentUser(authentication.getName(), true, null);
        }

        Member member = memberRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> CustomException.forbidden(
                        Code.MEMBER_ACCOUNT_NOT_LINKED,
                        String.format("No library member is linked to %s", authentication.getName())));

        return new CurrentUser(authentication.getName(), false, member.getId());
    }
}
