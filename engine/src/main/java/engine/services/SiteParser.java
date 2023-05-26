package engine.services;

import engine.models.Site;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveTask;

public class SiteParser extends RecursiveTask<Set<String>> {

    private String url;

    public SiteParser(String url){
        this.url = url;
    }

    private boolean checkLink(String url){
        try {
            if ((!RunnableIndexer.currentUrl.equals(url) && !RunnableIndexer.currentUrl.equals(url +"/")) && url.startsWith(RunnableIndexer.currentUrl)
                    && !RunnableIndexer.pages.contains(url) && !url.substring(url.lastIndexOf("/")).contains("#")
                    && !url.endsWith(".pdf") && !url.endsWith("#")) {
                int parentStatusCode = Jsoup.connect(url).ignoreHttpErrors(true).execute().statusCode();
                if (parentStatusCode == 200){
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
    protected Set<String> compute() {
        List<SiteParser> taskList = new ArrayList<>();
        Document doc;
        try {
            sleepBeforeConnect();
            doc = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/110.0.0.0 YaBrowser/23.3.4.603 Yowser/2.5 Safari/537.36")
                    .referrer("https://www.google.com")
                    .get();
            Elements docElements = doc.select("a");
            for (Element element : docElements){
                String href = element.attr("abs:href");
                if (checkLink(href)){
                    System.out.println(href);
                    SiteParser task = new SiteParser(href);
                    task.fork();
                    taskList.add(task);
                    RunnableIndexer.pages.add(url);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (SiteParser task : taskList) {
            RunnableIndexer.pages.addAll(task.join());
        }
        return RunnableIndexer.pages;
    }
}
