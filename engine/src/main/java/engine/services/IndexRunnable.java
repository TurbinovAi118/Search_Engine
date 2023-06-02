package engine.services;

import engine.models.Page;
import engine.models.Site;
import engine.models.enums.SiteStatus;

import java.util.*;
import java.util.concurrent.ForkJoinPool;

public class IndexRunnable implements Runnable {

    private final SiteService siteService;
    private final PageService pageService;

    protected static String currentUrl;
    protected static Map<String, String> pages = Collections.synchronizedMap(new HashMap<>());


    public IndexRunnable(SiteService siteService, PageService pageService) {
        this.siteService = siteService;
        this.pageService = pageService;
    }

    @Override
    public void run() {
        List<Site> sitesToIndex = siteService.list();
        if (sitesToIndex.size() >= 1) {
            for (Site site : sitesToIndex) {
                site.setStatus(SiteStatus.INDEXING);
                siteService.patch(site);

                currentUrl = site.getSiteUrl();
                SiteParser parser = new SiteParser(currentUrl);
                pages = new ForkJoinPool().invoke(parser);

                List<String> pathList = pageService.pathList();

                for (String page : pages.keySet()){
                    String path = currentUrl.endsWith("/") ?
                            page.replace(currentUrl, "/") :
                            page.replace(currentUrl, "");
                    if (path.length() > 1 && !pathList.contains(path)) {
                        Page parsedPage = new Page(site, path, 200, pages.get(page));
                        pageService.add(parsedPage);
                    }
                }
            }
        }
    }
}
