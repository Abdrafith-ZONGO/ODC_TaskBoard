package com.example.Mapper;

import org.springframework.stereotype.Component;

import com.example.DTO.UserRequestDTO;
import com.example.DTO.UserResponseDTO;
import com.example.Model.UserModel;

@Component
public class UserMapper {

    public UserModel toEntity(UserRequestDTO dto) {
        UserModel user = new UserModel();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        return user;
    }

    public UserResponseDTO toResponseDTO(UserModel user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}