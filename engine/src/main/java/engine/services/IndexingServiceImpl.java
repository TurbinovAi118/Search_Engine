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
    private final LemmaService lemmaService;
    private final SitesConfigList sites;

    public static volatile RunnableFuture<String> futureIndexer;

    public static ExecutorService executor;

    public static boolean isIndexing = false;

    public static volatile List<Page> pageList;

    @Override
    public ApiResponse startIndexing() {
        ApiResponse response = new ApiResponse();
        pageList = Collections.synchronizedList(new ArrayList<>());
        if (isIndexing){
            response.setResult(false);
            response.setError("Индексация уже запущена");
            return response;
        }
        isIndexing = true;
        executor = Executors.newSingleThreadExecutor();
        futureIndexer = new FutureTask<>(new SiteIndexer(siteService, pageService, lemmaService, sites), "");
        executor.submit(futureIndexer);
        executor.shutdown();

        response.setResult(true);
        return response;
    }

    @Override
    public ApiResponse stopIndexing(){
        ApiResponse response = new ApiResponse();
        if (isIndexing){
            List<Site> sites = siteService.list();

            futureIndexer.cancel(true);

            SiteIndexer.pool.shutdownNow();

            awaitPoolTermination(SiteIndexer.pool);

            System.out.println(futureIndexer);

            if (pageList.size() > 0) {
                List<Page> pagesForLemmas = pageService.addAll(pageList);
                for (Page page : pagesForLemmas){
                    lemmaService.addLemmas(page);
                }
                pageList.clear();
            }
            for (Site site : sites){
                if (site.getStatus().equals(SiteStatus.INDEXING)) {
                    site.setLastError("Индексация остановлена пользователем");
                    site.setStatus(SiteStatus.FAILED);
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

    public static void awaitPoolTermination(ForkJoinPool pool){
        while (true){
            if (pool.getActiveThreadCount() == 0){
                break;
            }
        }
    }

}
