package engine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.concurrent.RecursiveTask;

public class SiteParser extends RecursiveTask<Map<String, String>> {

    private String url;

    public SiteParser(String url){
        this.url = url;
    }

    private boolean checkLink(String url){
        try {
            if ((!IndexRunnable.currentUrl.equals(url) && !IndexRunnable.currentUrl.equals(url +"/")) && url.startsWith(IndexRunnable.currentUrl)
                    && !IndexRunnable.pages.containsKey(url) && !url.substring(url.lastIndexOf("/")).contains("#")
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
    protected Map<String, String> compute() {
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
                    SiteParser task = new SiteParser(href);
                    task.fork();
                    taskList.add(task);
                    IndexRunnable.pages.put(url, doc.html());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (SiteParser task : taskList) {
            IndexRunnable.pages.putAll(task.join());

        }
        return IndexRunnable.pages;
    }
}
