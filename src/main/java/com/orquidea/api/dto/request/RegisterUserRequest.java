package com.orquidea.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterUserRequest {
    @NotBlank
    @Size(min = 2)
    @Pattern(
            regexp = "^[\\p{L}]+(?:[ ]+[\\p{L}]+)*$",
            message = "Only letters and spaces are allowed"
    )
    private String firstName;

    @NotBlank
    @Size(min = 2)
    @Pattern(
            regexp = "^[\\p{L}]+(?:[ ]+[\\p{L}]+)*$",
            message = "Only letters and spaces are allowed"
    )
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}
