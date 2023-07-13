package engine.services;

import engine.config.SitesConfigList;
import engine.dto.ApiResponse;
import engine.models.Page;
import engine.models.Site;
import engine.models.enums.SiteStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService{

    private final SiteService siteService;
    private final PageService pageService;
    private final SitesConfigList sites;

    public static RunnableFuture<Boolean> futureIndexer;

    public static ExecutorService executor;

    public static boolean isIndexing = false;

    public static List<Page> pageList = Collections.synchronizedList(new ArrayList<>());

    @Override
    public ApiResponse startIndexing() {
        ApiResponse response = new ApiResponse();
        if (isIndexing){
            response.setResult(false);
            response.setError("Индексация уже запущена");
            return response;
        }
        isIndexing = true;
        executor = Executors.newFixedThreadPool(2);
        futureIndexer = new FutureTask<>(new IndexRunnable(siteService, pageService, sites), true);
        executor.submit(futureIndexer);


        response.setResult(true);
        return response;
    }

    @Override
    public ApiResponse stopIndexing() {
        ApiResponse response = new ApiResponse();
        if (isIndexing){

            futureIndexer.cancel(true);

            System.out.println(futureIndexer);

            if (pageList.size() > 0) {
                pageService.addAll(pageList);
                pageList.clear();
            }
            List<Site> sites = siteService.list();
            for (Site site : sites){
                if (site.getStatus().equals(SiteStatus.INDEXING)) {
                    site.setStatus(SiteStatus.FAILED);
                    site.setLastError("Индексанция остановлена пользователем");
                    siteService.patch(site);
                }
            }

            isIndexing = false;
            response.setError("Индексация остановлена пользователем");
            response.setResult(true);
            return response;
        }
        response.setResult(false);
        response.setError("Индексация не запущена/завершена");
        return response;
    }

}
