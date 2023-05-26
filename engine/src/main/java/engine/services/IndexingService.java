package engine.services;

import engine.dto.ApiResponse;

public interface IndexingService {

    ApiResponse startIndexing();

    ApiResponse stopIndexing();
}
