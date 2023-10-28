package engine.services;

import engine.config.SiteConfig;
import engine.config.SitesConfigList;
import engine.dto.ApiResponse;
import engine.models.Page;
import engine.models.Site;
import engine.models.enums.SiteStatus;
import engine.repositories.PageRepository;
import engine.repositories.SiteRepository;
import engine.utils.LemmaParser;
import engine.utils.SiteConnector;
import engine.utils.SitePatcher;
import lombok.AllArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Executors;

@Service
@AllArgsConstructor
public class PageService {
    private final SitesConfigList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final SitePatcher sitePatcher;
    private final LemmaParser lemmaParser;

    public ApiResponse addSinglePage(String pageUrl) {
        ApiResponse response = new ApiResponse();

        String siteURL = sites.getSites().stream().map(SiteConfig::getUrl)
                .filter(pageUrl::startsWith).findFirst().orElse("");

        if (siteURL.isBlank()){
            response.setResult(false);
            response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            return response;
        }

        Executors.newSingleThreadExecutor().execute(() -> indexPage(siteURL, pageUrl));

        response.setResult(true);
        return response;
    }

    private void indexPage(String siteURL, String pageUrl){
        SiteConfig siteConfig = sites.findSiteByURL(siteURL);

        Site siteForPage;
        Optional<Site> siteOptional = siteRepository.findBySiteUrl(siteURL).isPresent() ?
                siteRepository.findBySiteUrl(siteURL) : siteRepository.findBySiteUrl(siteURL+"/");
        if (siteOptional.isEmpty()){
            siteForPage = new Site(SiteStatus.INDEXING, LocalDateTime.now(), null, siteConfig.getUrl(), siteConfig.getName());
            siteRepository.save(siteForPage);
        } else
            siteForPage = siteOptional.get();

        String path = siteForPage.getSiteUrl().endsWith("/") ?
                pageUrl.replace(siteForPage.getSiteUrl(), "/") :
                pageUrl.replace(siteForPage.getSiteUrl(), "");

        try {
            Document doc = new SiteConnector(pageUrl).getDoc();
            Page page = pageRepository.existsByPath(path) ? pageRepository.findPageByPath(path).get() :
                    pageRepository.save(new Page(siteForPage, path, Jsoup.connect(pageUrl)
                            .ignoreHttpErrors(true).execute().statusCode(), doc.html()));
            Executors.newSingleThreadExecutor().execute(() -> lemmaParser.addLemmas(page));
            sitePatcher.patch(siteForPage);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
