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
import org.springframework.web.bind.annotation.RestController;

import com.example.DTO.TaskRequestDTO;
import com.example.DTO.TaskResponseDTO;
import com.example.Service.TaskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/boards/{boardId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // CREATE

    @PostMapping
    public ResponseEntity<TaskResponseDTO> createTask(
            @PathVariable Long boardId,
            @Valid @RequestBody TaskRequestDTO requestDTO) {

        TaskResponseDTO created = taskService.createTask(boardId, requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // READ

    @GetMapping
    public ResponseEntity<List<TaskResponseDTO>> getTasks(@PathVariable Long boardId) {
        return ResponseEntity.ok(taskService.getTasksByBoard(boardId));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponseDTO> getTaskById(
            @PathVariable Long boardId,
            @PathVariable Long taskId) {

        return ResponseEntity.ok(taskService.getTaskById(taskId));
    }

    // UPDATE

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponseDTO> updateTask(
            @PathVariable Long boardId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskRequestDTO requestDTO) {

        return ResponseEntity.ok(taskService.updateTask(taskId, requestDTO));
    }

    // DELETE

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long boardId,
            @PathVariable Long taskId) {

        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }
}
