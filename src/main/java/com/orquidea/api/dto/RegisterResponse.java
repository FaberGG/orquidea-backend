package com.orquidea.api.dto;

import com.orquidea.api.model.Role;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RegisterResponse {

    private UUID id;
    private String nombre;
    private String apellido;
    private String correo;
    private Role rol;
}