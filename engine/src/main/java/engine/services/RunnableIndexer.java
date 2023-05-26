package engine.services;


import engine.models.Page;
import engine.models.Site;
import engine.models.enums.SiteStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class RunnableIndexer implements Runnable {

    protected static String currentUrl;
    protected static Set<String> pages = Collections.synchronizedSet(new HashSet<>());

    private final SiteService siteService;
    private final PageService pageService;
    private Boolean isIndexing;

    public RunnableIndexer(SiteService siteService, PageService pageService, Boolean isIndexing) {
        this.siteService = siteService;
        this.pageService = pageService;
        this.isIndexing = isIndexing;
    }

    @Override
    public void run() {
        List<Site> sitesToIndex = siteService.list();
        for (Site site : sitesToIndex){
            site.setStatus(SiteStatus.INDEXING);
            siteService.patch(site);
            currentUrl = site.getSiteUrl();
            SiteParser parser = new SiteParser(currentUrl);
            pages = new ForkJoinPool().invoke(parser);
            for (String page : pages){
                List<String> pathList = pageService.pathList();
                String path = currentUrl.endsWith("/") ?
                        page.replace(currentUrl, "/") :
                        page.replace(currentUrl, "");
                if (path.length() <= 1 || pathList.contains(path)){
                    continue;
                }
                Page parsedPage = new Page();
                parsedPage.setSite(site);
                parsedPage.setPath(path);
                parsedPage.setResponseCode(200);
                parsedPage.setContent("asd");
                site.addPage(parsedPage);
                pageService.add(parsedPage);
            }
            site.setStatus(SiteStatus.INDEXED);
            isIndexing = false;
            siteService.patch(site);
            pages.clear();
        }
    }

}
