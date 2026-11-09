package com.example.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.Client.Client;
import com.example.DTO.EmployeeResponseDTO;

@RestController 
public class TestFeighController {

    private final Client client;

    public TestFeighController(Client client) {
        this.client = client;
    }

    @GetMapping("/test-feigh/{id}")
    public String testerCommunication(@PathVariable("id") Long id) {
        try {
            EmployeeResponseDTO employee = client.getUserById(id);
            return "Communication réussie avec le microservice Employee. Utilisateur récupéré : " 
                    + employee.getFirstName() + " " + employee.getLastName();
        } catch (Exception e) {
            return "Erreur lors de la communication avec le microservice Employee : " + e.getMessage();
        }
    }
}