package engine.services;

import engine.dto.ApiResponse;
import engine.dto.search.ApiSearchResponse;
import engine.models.Page;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface SearchService {

    ApiSearchResponse search(Map<String, String> body);

}
