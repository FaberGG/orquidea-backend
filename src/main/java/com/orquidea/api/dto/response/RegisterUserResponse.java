package com.orquidea.api.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterUserResponse {

    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
}