package engine.services;

import engine.config.SiteConfig;
import engine.config.SitesConfigList;
import engine.models.Page;
import engine.models.Site;
import engine.models.enums.SiteStatus;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
public class IndexRunnable implements Runnable /*Callable<Boolean>*/ {

    private final SiteService siteService;
    private final PageService pageService;
    private final SitesConfigList sites;



    @Override
    public void run() {
        for (SiteConfig site : sites.getSites()){
            siteService.findBySiteUrl(site.getUrl()).ifPresent(value -> siteService.delete(value.getId()));
            siteService.add(new Site(SiteStatus.INDEXING, LocalDateTime.now(), null, site.getUrl(), site.getName()));
        }
        IndexingServiceImpl.pageList.clear();
        List<Site> sitesToIndex = siteService.list();

        if (sitesToIndex.size() >= 1) {
            for (Site site : sitesToIndex) {
                if (site.getStatus() != SiteStatus.INDEXING) {
                    site.setStatus(SiteStatus.INDEXING);
                    siteService.patch(site);
                }
                SiteParser parser = new SiteParser(site.getSiteUrl(), site, pageService, siteService);
                ForkJoinPool pool= new ForkJoinPool();
                pool.invoke(parser);
                pool.shutdown();
            }
        }
        System.out.println(IndexingServiceImpl.pageList);
        if (IndexingServiceImpl.pageList.size() > 0) {
            pageService.addAll(IndexingServiceImpl.pageList);
        }
        IndexingServiceImpl.pageList.clear();
        System.out.println("indexed");

    }

//    public IndexRunnable(SiteService siteService, PageService pageService) {
//        this.siteService = siteService;
//        this.pageService = pageService;
//    }


}
