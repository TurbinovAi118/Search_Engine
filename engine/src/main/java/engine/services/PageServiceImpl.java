package engine.services;

import engine.config.SiteConfig;
import engine.config.SitesConfigList;
import engine.dto.ApiResponse;
import engine.models.Page;;
import engine.models.Site;
import engine.models.enums.SiteStatus;
import engine.repositories.PageRepository;
import lombok.AllArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

@Service
@AllArgsConstructor
public class PageServiceImpl  implements PageService{

    private final PageRepository pageRepository;
    private final SiteService siteService;
    private final LemmaService lemmaService;
    private final SitesConfigList sites;


    @Override
    public void add(Page page) {
        if (siteService.findById(page.getSite().getId()).isPresent()){
            pageRepository.save(page);
        }
    }

    @Override
    public List<Page> addAll(List<Page> pageList){
        List<Page> addedPages = new ArrayList<>();
        Iterable<Page> iterable = pageList;
        pageRepository.saveAll(iterable).forEach(addedPages::add);
        return addedPages;
    }

    @Override
    public List<Page> list() {
        List<Page> pageList = new ArrayList<>();
        pageRepository.findAll().forEach(pageList::add);
        return pageList;
    }

    @Override
    public Optional<Page> findById(int id) {
        return pageRepository.findById(id);
    }

    @Override
    public void delete(int id) {
        findById(id).ifPresent(pageRepository::delete);
    }

    @Override
    public List<Page> findPagesBySiteId(int id) {
        return pageRepository.getPageBySiteId(id);

    }

    @Override
    public Boolean existPageByPath(String path) {
        return pageRepository.existsByPath(path);
    }

    @Override
    public Integer countPagesBySiteId(Site site) {
        return pageRepository.countAllBySite(site);
    }

    @Override
    public ApiResponse addSinglePage(String pageUrl) {
        ApiResponse response = new ApiResponse();
        String validSearchURL = "";
        String protocol = pageUrl.startsWith("https://") ? "https://" : "http://";

        validSearchURL = pageUrl.replace(protocol, "");
        validSearchURL = protocol +
                validSearchURL.replace(validSearchURL.substring(validSearchURL.indexOf("/")), "");

        Optional<Site> siteOptional = siteService.findBySiteUrl(validSearchURL).isPresent() ?
                siteService.findBySiteUrl(validSearchURL) : siteService.findBySiteUrl(validSearchURL + "/");

        Site siteForPage = null;

        if (siteOptional.isEmpty()){
            for (SiteConfig site : sites.getSites()){
                if (pageUrl.startsWith(site.getUrl())){
                    siteForPage = new Site(SiteStatus.INDEXING, LocalDateTime.now(), null, site.getUrl(),
                            site.getName());
                    siteService.add(siteForPage);
                } else {
                    response.setResult(false);
                    response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
                    return response;
                }
            }
        } else {
            siteForPage = siteOptional.get();
        }

        assert siteForPage != null;
        String path = siteForPage.getSiteUrl().endsWith("/") ?
                pageUrl.replace(siteForPage.getSiteUrl(), "/") :
                pageUrl.replace(siteForPage.getSiteUrl(), "");


        try {
            Document doc = Jsoup.connect(pageUrl).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/110.0.0.0 YaBrowser/23.3.4.603 Yowser/2.5 Safari/537.36")
                    .referrer("https://www.google.com")
                    .get();
//            Page page;
//            if (!existPageByPath(path)) {
//                int statusCode = Jsoup.connect(pageUrl).ignoreHttpErrors(true).execute().statusCode();
//                page = new Page(siteForPage, path, statusCode, doc.html());
//                pageRepository.save(page);
//            }
//            else {
//                page = pageRepository.getPageByPath(path).get();
//            }
            Page page = existPageByPath(path) ? pageRepository.getPageByPath(path).get() :
                    pageRepository.save(new Page(siteForPage, path, Jsoup.connect(pageUrl)
                            .ignoreHttpErrors(true).execute().statusCode(), doc.html()));

            lemmaService.addLemmas(page);

            siteService.patch(siteForPage);

        } catch (Exception e){
            e.printStackTrace();
        }
        response.setResult(true);
        return response;
    }

    @Override
    public Optional<Page> findPageById(int id) {
        return pageRepository.findById(id);
    }

    @Override
    public void patch(Page page) {
        Optional<Page> optionalPage = findPageById(page.getId());
        if (optionalPage.isPresent()){
            Page pageToPatch = optionalPage.get();

            pageToPatch.setPath(page.getPath());
            pageToPatch.setSite(page.getSite());
            pageToPatch.setResponseCode(page.getResponseCode());
            pageToPatch.setContent(page.getContent());

            pageRepository.save(pageToPatch);
        }
    }
}