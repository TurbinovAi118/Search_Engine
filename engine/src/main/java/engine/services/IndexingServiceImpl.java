package engine.services;

import engine.dto.ApiResponse;
import engine.models.Site;
import engine.models.enums.SiteStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;

@Service
public class IndexingServiceImpl implements IndexingService{

    private final SiteService siteService;
    private final PageService pageService;

    private RunnableFuture<?> futureIndexer;

    private boolean isIndexing = false;

    public IndexingServiceImpl(SiteService siteService, PageService pageService) {
        this.siteService = siteService;
        this.pageService = pageService;
    }

    @Override
    public ApiResponse startIndexing() {
        ApiResponse response = new ApiResponse();
        if (isIndexing){
            response.setResult(false);
            response.setError("Индексация уже запущена");
            return response;
        }
        if (siteService.list().size() < 1){
            response.setResult(false);
            response.setError("Нечего индексировать");
            return response;
        }
        isIndexing = true;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        futureIndexer = new FutureTask<>(new IndexRunnable(siteService, pageService), "");
        executor.execute(futureIndexer);

        Runnable completeCatcher = this::futureCompleteCatcher;
        executor.submit(completeCatcher);

        executor.shutdown();

        response.setResult(true);
        return response;
    }

    @Override
    public ApiResponse stopIndexing() {
        ApiResponse response = new ApiResponse();
        if (isIndexing){
            List<Site> sites = siteService.list();
            for (Site site : sites){
                if (site.getStatus().equals(SiteStatus.INDEXING)) {
                    site.setStatus(SiteStatus.FAILED);
                    site.setLastError("Индексанция остановлена пользователем");
                    siteService.patch(site);
                }
            }

            futureIndexer.cancel(true);

            isIndexing = false;
            response.setError("Индексация звершена/остановлена");
            response.setResult(true);
            return response;
        }
        response.setResult(false);
        response.setError("Индексация не запущена");
        return response;
    }

    private void futureCompleteCatcher(){
        while(!futureIndexer.isDone()){
            if (futureIndexer.isCancelled()){
                return;
            }
            continue;
        }
        siteService.list().forEach(site -> {
            site.setPageId(pageService.findPagesBySiteId(site.getId()));
            if (!site.getStatus().equals(SiteStatus.FAILED)) {
                site.setStatus(SiteStatus.INDEXED);
            }
            siteService.patch(site);
        });
        System.out.println("stop now!");


    }
}
