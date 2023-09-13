package engine.services;

import engine.dto.search.ApiSearchResponse;

import java.util.Map;

public interface SearchService {
    ApiSearchResponse search(Map<String, String> body);
}
