package com.example.Exception;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor 
@Builder 
public class ErrorResponse {
    private String message;
    private int status;
    private LocalDateTime timestamp;
    private String error;
    private String path;
    private String exception;
    private Map<String, String> validationErrors;

    public ErrorResponse(String message, int status) {
        this.message = message;
        this.status = status;
    }

}
