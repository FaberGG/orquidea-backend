package com.orquidea.api.service;

import com.orquidea.api.dto.request.RegisterUserRequest;
import com.orquidea.api.dto.response.RegisterUserResponse;
import com.orquidea.api.exception.EmailAlreadyExistsException;
import com.orquidea.api.mapper.UserMapper;
import com.orquidea.api.model.Role;
import com.orquidea.api.model.User;
import com.orquidea.api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements IUserService{
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,UserMapper userMapper , PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public RegisterUserResponse registerUser(RegisterUserRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException();
        }
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.USER);
        User savedUser = userRepository.save(user);


       return userMapper.toResponse(savedUser);

    }
}
