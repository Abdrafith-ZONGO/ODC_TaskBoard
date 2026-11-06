package com.example.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.DTO.BoardRequestDTO;
import com.example.DTO.BoardResponseDTO;
import com.example.Service.BoardService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    // CREATE

    @PostMapping
    public ResponseEntity<BoardResponseDTO> createBoard(
            @RequestParam Long ownerId,
            @Valid @RequestBody BoardRequestDTO requestDTO) {

        BoardResponseDTO created = boardService.createBoard(ownerId, requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // READ

    @GetMapping
    public ResponseEntity<List<BoardResponseDTO>> getBoards(
            @RequestParam(required = false) Long ownerId) {

        if (ownerId != null) {
            return ResponseEntity.ok(boardService.getBoardsByOwner(ownerId));
        }
        return ResponseEntity.ok(boardService.getAllBoards());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoardResponseDTO> getBoardById(@PathVariable Long id) {
        return ResponseEntity.ok(boardService.getBoardById(id));
    }

    // UPDATE

    @PutMapping("/{id}")
    public ResponseEntity<BoardResponseDTO> updateBoard(
            @PathVariable Long id,
            @Valid @RequestBody BoardRequestDTO requestDTO) {

        return ResponseEntity.ok(boardService.updateBoard(id, requestDTO));
    }

    // DELETE

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long id) {
        boardService.deleteBoard(id);
        return ResponseEntity.noContent().build();
    }
}
