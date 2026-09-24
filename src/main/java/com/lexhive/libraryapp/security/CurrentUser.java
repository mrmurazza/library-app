package com.lexhive.libraryapp.security;

public record CurrentUser(String username, boolean isAdmin, Long memberId) {
}
