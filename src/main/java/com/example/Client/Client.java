package com.example.Client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.DTO.EmployeeResponseDTO;

@FeignClient(name = "employee-service", url = "http://localhost:9000")

public interface Client {
    @GetMapping("/employee/{id}")
    EmployeeResponseDTO  getUserById(@PathVariable("id") Long userId);
}
