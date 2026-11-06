package com.example.Service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.DTO.UserRequestDTO;
import com.example.DTO.UserResponseDTO;
import com.example.Mapper.UserMapper;
import com.example.Exception.DuplicateResourceException;
import com.example.Exception.ResourceNotFoundException;
import com.example.Model.UserModel;
import com.example.Repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;

/**
 * Couche Service : contient la LOGIQUE METIER.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserMapper userMapper;


    // CREATE

    public UserResponseDTO createUser(UserRequestDTO requestDTO) {
        if (userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new DuplicateResourceException(
                    "Un utilisateur avec cet email existe deja : " + requestDTO.getEmail());
        }

        UserModel user = userMapper.toEntity(requestDTO);
        user.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        UserModel savedUser = userRepository.save(user);
        return userMapper.toResponseDTO(savedUser);
    
    }

    // READ

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public UserResponseDTO getUserById(Long id) {
        UserModel user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable avec id=" + id));
        return userMapper.toResponseDTO(user);
    
    }

    public UserModel getUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable avec id=" + id));
    }

    // UPDATE

    public UserResponseDTO updateUser(Long id, UserRequestDTO requestDTO) {

        // 1. Récupérer l'utilisateur existant (ou 404)
        UserModel existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable avec id=" + id));

        // 2. Vérifier l'email : seulement s'il a changé ET qu'il est déjà pris
        boolean emailHasChanged = !existingUser.getEmail().equals(requestDTO.getEmail());
        if (emailHasChanged && userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new DuplicateResourceException(
                    "Un utilisateur avec cet email existe déjà : " + requestDTO.getEmail());
        }

        // 3. Appliquer les modifications
        existingUser.setName(requestDTO.getName());
        existingUser.setEmail(requestDTO.getEmail());
        existingUser.setPassword(passwordEncoder.encode(requestDTO.getPassword()));

        // 4. Sauvegarder
        UserModel updatedUser = userRepository.save(existingUser);
        return userMapper.toResponseDTO(updatedUser);
    
    }

    // DELETE

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Utilisateur introuvable avec id=" + id);
        }
        userRepository.deleteById(id);
    }
}