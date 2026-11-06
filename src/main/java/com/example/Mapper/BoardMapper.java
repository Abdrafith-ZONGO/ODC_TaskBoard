package com.example.Mapper;

import org.springframework.stereotype.Component;

import com.example.DTO.BoardRequestDTO;
import com.example.DTO.BoardResponseDTO;
import com.example.Model.BoardModel;

@Component
public class BoardMapper {

    /**
     * Convertit un DTO de création/mise à jour en Entité JPA.
     */
    public BoardModel toEntity(BoardRequestDTO dto) {
        BoardModel board = new BoardModel();
        board.setTitle(dto.getTitle());
        return board;
    }

    /**
     * Convertit une Entité JPA en DTO de réponse.
     */
    public BoardResponseDTO toResponseDTO(BoardModel board) {
        return BoardResponseDTO.builder()
                .id(board.getId())
                .title(board.getTitle())
                .createdAt(board.getCreatedAt())
                .ownerId(board.getOwner() != null ? board.getOwner().getId() : null)
                .build();
    }
}

