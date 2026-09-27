package com.orquidea.api.mapper;

import com.orquidea.api.dto.request.RegisterUserRequest;
import com.orquidea.api.dto.response.RegisterUserResponse;
import com.orquidea.api.model.User;
import org.mapstruct.control.MappingControl;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(RegisterUserRequest request){
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());

        return user;
    }

    public RegisterUserResponse toResponse(User user) {
        RegisterUserResponse response = new RegisterUserResponse();

        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());

        return response;
    }
}
