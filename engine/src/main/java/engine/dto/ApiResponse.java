package engine.dto;

import lombok.Data;

@Data
public class ApiResponse {
    private boolean result;
    private String error;
}
