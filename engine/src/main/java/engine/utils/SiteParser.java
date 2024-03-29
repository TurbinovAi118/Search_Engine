package engine.utils;

import engine.models.Page;
import engine.models.Site;
import engine.repositories.PageRepository;
import engine.services.implementation.IndexingServiceImpl;
import lombok.AllArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;


@AllArgsConstructor
public class SiteParser extends RecursiveAction {

    private final Site site;
    private final String currentUrl;
    private static String path;

    private final PageRepository pageRepository;
    private final SitePatcher sitePatcher;
    private final LemmaParser lemmaParser;

    private synchronized boolean checkLink(String url) {
        path = site.getSiteUrl().endsWith("/") ?
                url.replace(site.getSiteUrl(), "/") :
                url.replace(site.getSiteUrl(), "");

        return url.startsWith(site.getSiteUrl())
                && !site.getSiteUrl().equals(url)
                && !path.equals("/")
                && !path.endsWith("#")
                && checkType(path)
                && !path.contains("sort")
                && !path.contains("?")
                && !path.contains("&")
                && !path.substring(path.lastIndexOf("/")).contains("#")
                && !IndexingServiceImpl.pageList.stream().map(Page::getPath).collect(Collectors.toList()).contains(path)
                && !pageRepository.existsByPath(path);
    }

    private boolean checkType(String url) {
        return !url.toLowerCase(Locale.ROOT)
                .contains(".jpg")
                && !url.contains(".jpeg")
                && !url.contains(".png")
                && !url.contains(".gif")
                && !url.contains(".webp")
                && !url.contains(".pdf")
                && !url.contains(".eps")
                && !url.contains(".xlsx")
                && !url.contains(".doc")
                && !url.contains(".docx")
                && !url.contains(".pptx")
                && !url.contains(".mp3")
                && !url.contains(".mp4");
    }

    private static synchronized void sleepBeforeConnect() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void compute() {
        try {
            sleepBeforeConnect();
            Document doc = new SiteConnector(currentUrl).getDoc();

            Elements docElements = doc.select("a");
            for (Element element : docElements) {
                if (IndexingServiceImpl.futureIndexer.isCancelled()) {
                    break;
                }
                String href = element.attr("abs:href");
                synchronized (SiteParser.class) {
                    indexPage(href);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void indexPage(String href) throws IOException {
        if (checkLink(href)) {
            int statusCode = Jsoup.connect(href).ignoreHttpErrors(true).execute().statusCode();

            String content = (statusCode == 200) ? new SiteConnector(href).getDoc().html() : "";

            IndexingServiceImpl.pageList.add(new Page(site, path, statusCode, content));

            if (IndexingServiceImpl.pageList.size() >= 100)
                multiInsertPages();

            if (statusCode == 200) {
                SiteParser task = new SiteParser(site, href, pageRepository, sitePatcher, lemmaParser);
                task.fork();
            }
        }
    }

    private void multiInsertPages() {
        List<Page> pagesForLemmas = new ArrayList<>();
        pageRepository.saveAll(IndexingServiceImpl.pageList).forEach(pagesForLemmas::add);

        for (Page page : pagesForLemmas) {
            lemmaParser.addLemmas(page);
        }

        IndexingServiceImpl.pageList.clear();
        sitePatcher.patch(site);
    }
}
