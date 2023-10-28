package engine.services.implementation;

import engine.config.SitesConfigList;
import engine.dto.ApiResponse;
import engine.models.Page;
import engine.models.Site;
import engine.models.enums.SiteStatus;
import engine.repositories.PageRepository;
import engine.repositories.SiteRepository;
import engine.services.*;
import engine.utils.LemmaParser;
import engine.utils.SiteIndexer;
import engine.utils.SitePatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitePatcher sitePatcher;
    private final LemmaParser lemmaParser;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesConfigList sites;

    public static volatile RunnableFuture<String> futureIndexer;

    public static ExecutorService executor;

    public static boolean isIndexing = false;

    public static volatile List<Page> pageList;

    @Override
    public ApiResponse startIndexing() {
        ApiResponse response = new ApiResponse();
        pageList = Collections.synchronizedList(new ArrayList<>());
        if (isIndexing) {
            response.setResult(false);
            response.setError("Индексация уже запущена");
            return response;
        }

        isIndexing = true;
        executor = Executors.newSingleThreadExecutor();
        futureIndexer = new FutureTask<>(new SiteIndexer(sitePatcher, lemmaParser, siteRepository, pageRepository, sites), "");
        executor.submit(futureIndexer);
        executor.shutdown();

        response.setResult(true);
        return response;
    }

    @Override
    public ApiResponse stopIndexing() {
        ApiResponse response = new ApiResponse();
        if (isIndexing) {
            List<Site> sites = new ArrayList<>();
            siteRepository.findAll().forEach(sites::add);

            futureIndexer.cancel(true);

            SiteIndexer.pool.shutdownNow();

            awaitPoolTermination(SiteIndexer.pool);

            Executors.newSingleThreadExecutor().execute(() -> parseRemainingLemmas(pageList));

            for (Site site : sites) {
                if (site.getStatus().equals(SiteStatus.INDEXING)) {
                    site.setLastError("Индексация остановлена пользователем");
                    site.setStatus(SiteStatus.FAILED);
                    sitePatcher.patch(site);
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

    public void parseRemainingLemmas(List<Page> pageList) {
        if (pageList.size() > 0) {
            List<Page> pagesForLemmas = new ArrayList<>();
            pageRepository.saveAll(pageList).forEach(pagesForLemmas::add);
            for (Page page : pagesForLemmas) {
                lemmaParser.addLemmas(page);
            }
            pageList.clear();
        }
    }

    public static void awaitPoolTermination(ForkJoinPool pool) {
        while (true) {
            if (pool.getActiveThreadCount() == 0) {
                break;
            }
        }
    }

}
