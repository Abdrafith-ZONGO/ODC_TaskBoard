package com.example.Repository;

import com.example.Model.BoardModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardRepository extends JpaRepository<BoardModel, Long> {
    List<BoardModel> findByOwnerId(Long ownerId);
}
