package searchengine.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchingService;
import searchengine.services.StatisticsService;


@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger logger = LogManager.getLogger();
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchingService searchingService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SearchingService searchingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchingService = searchingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        boolean result = indexingService.startIndexingService();
        IndexingResponse response = new IndexingResponse();
        if (result) {
            response.setResult(true);
            logger.info("Starting indexing by web-interface.");
        } else {
            response.setResult(false);
            response.setError("Indexing already started.");
            logger.info("Trying to start indexing. Already started");
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        boolean result = indexingService.stopIndexingService();
        IndexingResponse response = new IndexingResponse();
        if (result) {
            response.setResult(true);
            logger.info("Stop indexing by front-interface.");
        } else {
            response.setResult(false);
            response.setError("Indexing not running");
            logger.info("Trying to stop indexing. Indexing not running");
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(String url) {
        boolean result = indexingService.indexingPage(url);
        IndexingResponse response = new IndexingResponse();
        if (result) {
            response.setResult(true);
            logger.info("Indexing individual page - {}", url);
        } else {
            response.setResult(false);
            response.setError("Данная страница находится за пределами сайтов указанных в конфигурационном файле");
            logger.info("Trying indexing individual page without sites in config-file: {}", url);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<SearchingResponse> search(String query, String site, Integer offset, Integer limit) {
        SearchingResponse response = searchingService.getSearchingResult(query, site, offset, limit);
        return ResponseEntity.ok(response);
    }
}
