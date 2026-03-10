package com.taskmanager.service;

import com.taskmanager.controller.dto.AuthResponse;
import com.taskmanager.controller.dto.LoginRequest;
import com.taskmanager.controller.dto.RegisterRequest;

public interface UserService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
