package engine.dto;

import lombok.Data;

import java.util.List;

@Data
public class ApiResponse {
    private boolean result;
    private String error;
    private List<ApiData> data;
}
