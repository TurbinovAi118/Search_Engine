package engine.utils;

import engine.config.SiteConfig;
import engine.config.SitesConfigList;

import engine.models.Page;
import engine.models.Site;
import engine.models.enums.SiteStatus;
import engine.repositories.PageRepository;
import engine.repositories.SiteRepository;
import engine.services.implementation.IndexingServiceImpl;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
public class SiteIndexer implements Runnable {

    private final SitePatcher sitePatcher;
    private final LemmaParser lemmaParser;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesConfigList sites;

    public static ForkJoinPool pool;

    @Override
    public void run() {

        for (SiteConfig site : sites.getSites()) {
            if (siteRepository.findBySiteUrl(site.getUrl()).isPresent()) {
                siteRepository.delete(siteRepository.findBySiteUrl(site.getUrl()).get());
            } else {
                siteRepository.findBySiteUrl(site.getUrl() + "/").ifPresent(siteRepository::delete);
            }
            siteRepository.save(new Site(SiteStatus.INDEXING, LocalDateTime.now(), null, site.getUrl(), site.getName()));
        }
        List<Site> sitesToIndex = new ArrayList<>();
        siteRepository.findAll().forEach(sitesToIndex::add);

        for (Site site : sitesToIndex) {
            if (site.getStatus() != SiteStatus.INDEXING) {
                site.setStatus(SiteStatus.INDEXING);
                sitePatcher.patch(site);
            }
            SiteParser parser = new SiteParser(site, site.getSiteUrl(), pageRepository, sitePatcher, lemmaParser);
            pool = new ForkJoinPool();
            pool.invoke(parser);

            IndexingServiceImpl.awaitPoolTermination(pool);

            if (!IndexingServiceImpl.futureIndexer.isCancelled()) {
                pool.shutdown();
                addPagesAndLemmas();

                site.setStatus(SiteStatus.INDEXED);
                sitePatcher.patch(site);
            }
        }
        IndexingServiceImpl.isIndexing = false;
    }

    private void addPagesAndLemmas() {
        if (IndexingServiceImpl.pageList.size() > 0) {
            List<Page> pagesForLemmas = new ArrayList<>();
            pageRepository.saveAll(IndexingServiceImpl.pageList).forEach(pagesForLemmas::add);
            pagesForLemmas.forEach(lemmaParser::addLemmas);
            IndexingServiceImpl.pageList.clear();
        }
    }
}



/*
    @Override
    public void run() {

        for (SiteConfig site : sites.getSites()) {
            if (siteRepository.findBySiteUrl(site.getUrl()).isPresent()) {
                siteRepository.findBySiteUrl(site.getUrl());
            } else {
                siteRepository.findBySiteUrl(site.getUrl() + "/").flatMap(value -> siteRepository.findById(value.getId()))
                        .ifPresent(siteRepository::delete);
            }
            siteRepository.save(new Site(SiteStatus.INDEXING, LocalDateTime.now(), null, site.getUrl(), site.getName()));
        }
        List<Site> sitesToIndex = new ArrayList<>();
        siteRepository.findAll().forEach(sitesToIndex::add);

        for (Site site : sitesToIndex) {
            if (site.getStatus() != SiteStatus.INDEXING) {
                site.setStatus(SiteStatus.INDEXING);
                SiteUtils.patch(site);
            }
            SiteParser parser = new SiteParser(site, site.getSiteUrl(), pageRepository);
            pool = new ForkJoinPool();
            pool.invoke(parser);

            IndexingServiceImpl.awaitPoolTermination(pool);

            if (!IndexingServiceImpl.futureIndexer.isCancelled()) {
                pool.shutdown();
                addPagesAndLemmas();

                site.setStatus(SiteStatus.INDEXED);
                SiteUtils.patch(site);
            }
        }
        IndexingServiceImpl.isIndexing = false;
    }

    private void addPagesAndLemmas() {
        if (IndexingServiceImpl.pageList.size() > 0) {
            List<Page> pagesForLemmas = new ArrayList<>();
            pageRepository.saveAll(IndexingServiceImpl.pageList).forEach(pagesForLemmas::add);
            pagesForLemmas.forEach(LemmaParser::addLemmas);
            IndexingServiceImpl.pageList.clear();
        }
    }
}
*/

