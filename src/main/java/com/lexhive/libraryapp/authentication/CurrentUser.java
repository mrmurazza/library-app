package com.lexhive.libraryapp.authentication;

public record CurrentUser(String username, boolean isAdmin, Long memberId) {
}
