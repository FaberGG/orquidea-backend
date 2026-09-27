package com.orquidea.api.service;


import com.orquidea.api.dto.request.RegisterUserRequest;
import com.orquidea.api.dto.response.RegisterUserResponse;
import com.orquidea.api.model.User;

public interface IUserService {

    RegisterUserResponse registerUser(RegisterUserRequest request);


}
