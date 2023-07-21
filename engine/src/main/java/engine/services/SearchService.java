package engine.services;

import engine.dto.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface SearchService {

    ApiResponse search(Map<String, String> body);

}
