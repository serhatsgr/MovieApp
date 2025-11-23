package com.serhatsgr.controller;

import com.serhatsgr.dto.*;
import org.springframework.http.ResponseEntity;

import jakarta.validation.Valid;

public interface IUserController {

    ResponseEntity<ApiSuccess<AuthResponse>> login(@Valid AuthRequest request); // login method

    ResponseEntity<ApiSuccess<CreateUserResponse>> register(@Valid CreateUserRequest request); // register method

    ResponseEntity<ApiSuccess<AuthResponse>> refreshToken(@Valid RefreshTokenRequest request); // refresh token method

}
