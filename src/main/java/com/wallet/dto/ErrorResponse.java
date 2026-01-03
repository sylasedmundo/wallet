package com.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    @JsonProperty("timestamp")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @JsonProperty("status")
    private int status;
    
    @JsonProperty("error")
    private String error;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("path")
    private String path;
    
    @JsonProperty("errors")
    private List<FieldError> errors;
    
    public static ErrorResponse of(int status, String error, String message, String path) {
        return ErrorResponse.builder()
            .status(status)
            .error(error)
            .message(message)
            .path(path)
            .build(); 
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        @JsonProperty("field")
        private String field;
        
        @JsonProperty("message")
        private String message;
        
        public static FieldError of(String field, String message) {
            return FieldError.builder()
                .field(field)
                .message(message)
                .build();
        }
    }
}