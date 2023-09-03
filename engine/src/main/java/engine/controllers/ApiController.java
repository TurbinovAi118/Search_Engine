package engine.controllers;

import engine.dto.ApiResponse;
import engine.dto.search.ApiSearchResponse;
import engine.dto.statistics.StatisticsResponse;
import engine.services.*;
import jdk.swing.interop.SwingInterOpUtils;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("/api")
@RestController
@AllArgsConstructor
public class ApiController {

    private final PageService pageService;
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @PostMapping("/indexPage")
    public ResponseEntity<ApiResponse> add(@RequestParam Map<String, String> body){
        ApiResponse response = pageService.addSinglePage(body.get("url"));
        return response.isResult() ? ResponseEntity.ok(response) : ResponseEntity.status(404).body(response);
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        StatisticsResponse statisticsResponse = statisticsService.getStatistics();
        return statisticsResponse.isResult() ? ResponseEntity.ok(statisticsResponse) :
                ResponseEntity.status(500).body(statisticsResponse);
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<ApiResponse> startIndexing() {
        ApiResponse response = indexingService.startIndexing();
        return response.isResult() ? ResponseEntity.status(202).body(response) : ResponseEntity.status(422).body(response);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<ApiResponse> stopIndexing() {
        ApiResponse response = indexingService.stopIndexing();
        return response.isResult() ? ResponseEntity.status(202).body(response) : ResponseEntity.status(422).body(response);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiSearchResponse> search(@RequestParam Map<String, String> body){
        ApiSearchResponse response = searchService.search(body);
        return response.isResult() ? ResponseEntity.status(200).body(response) : ResponseEntity.status(404).body(response);
    }
}
