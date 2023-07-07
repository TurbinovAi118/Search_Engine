package engine.services;

import engine.config.SiteConfig;
import engine.config.SitesConfigList;
import engine.dto.ApiResponse;
import engine.models.Page;
import engine.models.Site;
import engine.models.enums.SiteStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService{

    private final SiteService siteService;
    private final PageService pageService;
    private final SitesConfigList sites;

    RunnableFuture<?> futureIndexer;

    private boolean isIndexing = false;

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


//        pageList.clear();
//        List<Site> sitesToIndex = siteService.list();
//        if (sitesToIndex.size() >= 1) {
//            for (Site site : sitesToIndex) {
//                site.setStatus(SiteStatus.INDEXING);
//                siteService.patch(site);
//                SiteParser parser = new SiteParser(site.getSiteUrl(), site, pageService, siteService);
//                ForkJoinPool pool= new ForkJoinPool();
//                pool.invoke(parser);
//            }
//        }
//        System.out.println(pageList);
//        if (pageList.size() > 0) {
//            pageService.addAll(pageList);
//        }
//        pageList.clear();
//        System.out.println("indexed");
//        isIndexing = false;


        ExecutorService executor = Executors.newFixedThreadPool(2);
        futureIndexer = new FutureTask<>(new IndexRunnable(siteService, pageService, sites), true);
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
            futureIndexer.cancel(true);

            isIndexing = false;
            response.setError("Индексация остановлена пользователем");
            response.setResult(true);
            return response;
        }
        response.setResult(false);
        response.setError("Индексация не запущена/завершена");
        return response;
    }

    private void futureCompleteCatcher() {
        while(!futureIndexer.isDone()){
            if (futureIndexer.isCancelled()){
                break;
            }
        }
        siteService.list().forEach(site -> {
            if (!site.getStatus().equals(SiteStatus.FAILED)) {
                site.setStatus(SiteStatus.INDEXED);
                siteService.patch(site);
            }
        });
        if (!futureIndexer.isCancelled())
            isIndexing = false;
    }
}
