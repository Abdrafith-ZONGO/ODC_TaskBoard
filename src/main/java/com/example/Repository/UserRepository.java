package com.example.Repository;

import com.example.Model.UserModel;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserModel, Long> {

    Optional<UserModel > findByEmail(String email);
    boolean existsByEmail(String email);
}
