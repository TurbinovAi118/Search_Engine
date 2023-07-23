package engine.services;

import engine.models.Lemma;
import engine.models.Page;
import engine.models.Site;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SiteParser extends RecursiveAction {

    private final Site site;
    private final String currentUrl;
    private static String path;

    private final PageService pageService;
    private final SiteService siteService;
    private final LemmaService lemmaService;

//    public SiteParser(String currentUrl, Site site, PageService pageService, SiteService siteService) {
//        this.currentUrl = currentUrl;
//        this.site = site;
//        this.pageService = pageService;
//        this.siteService = siteService;
//    }

    private synchronized boolean checkLink(String url){
        path = site.getSiteUrl().endsWith("/") ?
                url.replace(site.getSiteUrl(), "/") :
                url.replace(site.getSiteUrl(), "");

        return url.startsWith(site.getSiteUrl())
                && !site.getSiteUrl().equals(url)
                && !path.equals("/")
                && !path.endsWith("#")
                && !path.endsWith(".pdf")
                //
                && !path.contains("sort")
                //
                && !path.substring(path.lastIndexOf("/")).contains("#")
                && !path.substring(path.lastIndexOf("/")).contains("?")
                && !path.substring(path.lastIndexOf("/")).contains("&")
                && !IndexingServiceImpl.pageList.stream().map(Page::getPath).collect(Collectors.toList()).contains(path)
                && !pageService.existPageByPath(path);
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
                if (IndexingServiceImpl.futureIndexer.isCancelled()){
                    break;
                }
                String href = element.attr("abs:href");
                synchronized (SiteParser.class) {
                    if (checkLink(href)) {
                        int statusCode = Jsoup.connect(href).ignoreHttpErrors(true).execute().statusCode();

                        //
                        System.out.println(href);
                        //

                        IndexingServiceImpl.pageList.add(new Page(site, path, statusCode, doc.html()));

                        if (IndexingServiceImpl.pageList.size() >= 100)
                            multiInsertPages();

                        SiteParser task = new SiteParser(site, href, pageService, siteService, lemmaService);
                        task.fork();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void multiInsertPages(){
        List<Page> pagesForLemmas = pageService.addAll(IndexingServiceImpl.pageList);

        for (Page page : pagesForLemmas){
            lemmaService.addLemmas(page);
        }

        IndexingServiceImpl.pageList.clear();
        siteService.patch(site);

        //
        System.out.println("added new 100 pages");
        //
    }
}
