package engine.controllers;

import engine.dto.ApiResponse;
import engine.dto.statistics.StatisticsResponse;
import engine.services.IndexingService;
import engine.services.SiteService;
import engine.services.StatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("/api")
@RestController
public class ApiController {

    private final SiteService siteService;
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    public ApiController(SiteService siteService, StatisticsService statisticsService, IndexingService indexingService) {
        this.siteService = siteService;
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
    }

    @PostMapping("/indexPage")
    public ResponseEntity<ApiResponse> add(@RequestParam Map<String, String> body){
        return ResponseEntity.ok(siteService.addCustom(body.get("url")));
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<ApiResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<ApiResponse> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }
}
