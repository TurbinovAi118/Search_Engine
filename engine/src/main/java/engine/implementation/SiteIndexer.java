package engine.implementation;

import engine.config.SiteConfig;
import engine.config.SitesConfigList;

import engine.models.Page;
import engine.models.Site;
import engine.models.enums.SiteStatus;
import engine.services.LemmaService;
import engine.services.PageService;
import engine.services.SiteService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
public class SiteIndexer implements Runnable {

    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final SitesConfigList sites;
    public static ForkJoinPool pool;

    @Override
    public void run() {
        for (SiteConfig site : sites.getSites()){
            siteService.findBySiteUrl(site.getUrl()).ifPresent(value -> siteService.delete(value.getId()));
            siteService.add(new Site(SiteStatus.INDEXING, LocalDateTime.now(), null, site.getUrl(), site.getName()));
        }
        IndexingServiceImpl.pageList.clear();
        List<Site> sitesToIndex = siteService.list();

        if (!sitesToIndex.isEmpty()) {
            for (Site site : sitesToIndex) {
                if (site.getStatus() != SiteStatus.INDEXING) {
                    site.setStatus(SiteStatus.INDEXING);
                    siteService.patch(site);
                }
                SiteParser parser = new SiteParser(site, site.getSiteUrl(), pageService, siteService, lemmaService);
                pool = new ForkJoinPool();
                pool.invoke(parser);

                IndexingServiceImpl.awaitPoolTermination(pool);

                if (!IndexingServiceImpl.futureIndexer.isCancelled()) {
                    pool.shutdown();

                    if (IndexingServiceImpl.pageList.size() > 0) {
                        List<Page> pagesForLemmas = pageService.addAll(IndexingServiceImpl.pageList);
                        for (Page page : pagesForLemmas){
                            lemmaService.addLemmas(page);
                        }
                        IndexingServiceImpl.pageList.clear();
                    }

                    site.setStatus(SiteStatus.INDEXED);
                    siteService.patch(site);
                }
            }
            IndexingServiceImpl.isIndexing = false;
        }
    }
}
