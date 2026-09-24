package com.lexhive.libraryapp.exception;

public record ErrorResponse(String code, String message) {

  public static ErrorResponse fromErrorCode(CustomException.Code code, String message) {
    return new ErrorResponse(code.name(), message);
  }
}
