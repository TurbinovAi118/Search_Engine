package engine.services;

import engine.models.Page;
import engine.models.Site;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

public class SiteParser extends RecursiveAction {

    private final Site site;
    private final String currentUrl;
    private static String path;

    private final PageService pageService;
    private final SiteService siteService;

    public SiteParser(String currentUrl, Site site, PageService pageService, SiteService siteService) {
        this.currentUrl = currentUrl;
        this.site = site;
        this.pageService = pageService;
        this.siteService = siteService;
    }

    private synchronized boolean checkLink(String url){
        path = site.getSiteUrl().endsWith("/") ?
                url.replace(site.getSiteUrl(), "/") :
                url.replace(site.getSiteUrl(), "");
        try {
            if (!url.substring(url.lastIndexOf("/")).contains("#")
                    && !path.endsWith(".pdf")
                    && !url.endsWith("#")
                    && url.startsWith(site.getSiteUrl())
                    && !site.getSiteUrl().equals(url)
                    && !path.equals("/")
                    && !IndexingServiceImpl.pageList.stream().map(Page::getPath).collect(Collectors.toList()).contains(path)
                    && !pageService.existPageByPath(path)
            ) {
                int parentStatusCode = Jsoup.connect(url).ignoreHttpErrors(true).execute().statusCode();
                if (parentStatusCode == 200) {
                    return true;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    private static synchronized void sleepBeforeConnect(){
        try{
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void compute() {
        Document doc;
        try {
            sleepBeforeConnect();
            doc = Jsoup.connect(currentUrl).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/110.0.0.0 YaBrowser/23.3.4.603 Yowser/2.5 Safari/537.36")
                    .referrer("https://www.google.com")
                    .get();
            Elements docElements = doc.select("a");
            for (Element element : docElements){
                String href = element.attr("abs:href");
                synchronized (pageService) {
                    if (checkLink(href)) {
                        //
                        System.out.println(href);
                        //
                        SiteParser task = new SiteParser(href, site, pageService, siteService);
                        task.fork();
                        IndexingServiceImpl.pageList.add(new Page(site, path, 200, doc.html()));
                        if (IndexingServiceImpl.pageList.size() >= 100)
                            multiInsertPages(IndexingServiceImpl.pageList);

//                    pageService.add(new Page(site, path, 200, doc.html()));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        sleepBeforeConnect();
    }

    private void multiInsertPages(List<Page> pageList){
        pageService.addAll(pageList);
        IndexingServiceImpl.pageList.clear();
        siteService.patch(site);
        System.out.println("added new 100 pages");
    }
}
