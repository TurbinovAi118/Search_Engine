package engine.services;

import engine.dto.ApiResponse;
import engine.models.Site;
import engine.models.enums.SiteStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class IndexingServiceImpl implements IndexingService{

    private Boolean isIndexing = false;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final RunnableIndexer indexer;
    private Future<?> indexerFuture;

    private final SiteService siteService;

    public IndexingServiceImpl(SiteService siteService, PageService pageService) {
        this.siteService = siteService;
        indexer = new RunnableIndexer(siteService, pageService, isIndexing);

    }

    @Override
    public ApiResponse startIndexing() {
        ApiResponse response = new ApiResponse();
        if (isIndexing){
            response.setResult(false);
            response.setError("Индексация уже запущена");
            return response;
        }
        isIndexing = true;
        indexerFuture = executor.submit(indexer);
        response.setResult(true);
//        executor.shutdown();
        return response;
    }



    @Override
    public ApiResponse stopIndexing() {
        ApiResponse response = new ApiResponse();
        if (isIndexing){
            isIndexing = false;
            indexerFuture.cancel(true);

            List<Site> sites = siteService.list();

            sites.forEach(site -> {
                if (site.getStatus().equals(SiteStatus.INDEXING)) {
                    site.setStatus(SiteStatus.FAILED);
                    site.setLastError("Индексанция остановлена пользователем");
                    siteService.patch(site);
                }
            });

            response.setError("остановлено");
            response.setResult(true);
            return response;
        }
        response.setResult(false);
        response.setError("Индексация не запущена");
        return response;
    }
}
