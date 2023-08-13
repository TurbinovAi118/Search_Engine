package engine.implementation;

import engine.config.SiteConfig;
import engine.config.SitesConfigList;
import engine.dto.ApiResponse;
import engine.models.Page;;
import engine.models.Site;
import engine.models.enums.SiteStatus;
import engine.repositories.PageRepository;
import engine.services.LemmaService;
import engine.services.PageService;
import engine.services.SiteService;
import lombok.AllArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PageServiceImpl  implements PageService {

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
        return pageRepository.findPagesBySiteId(id);
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
        String siteURL = "";
        for (SiteConfig site : sites.getSites()){
            if (pageUrl.startsWith(site.getUrl()))
                siteURL = site.getUrl();
        }

        if (siteURL.isBlank()){
            response.setResult(false);
            response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            return response;
        }

        SiteConfig siteConfig = sites.findSiteByURL(siteURL);

        Site siteForPage;
        Optional<Site> siteOptional = siteService.findBySiteUrl(siteURL);
        if (siteOptional.isEmpty()){
            siteForPage = new Site(SiteStatus.INDEXING, LocalDateTime.now(), null, siteConfig.getUrl(), siteConfig.getName());
            siteService.add(siteForPage);
        } else
            siteForPage = siteOptional.get();

        String path = siteForPage.getSiteUrl().endsWith("/") ?
                pageUrl.replace(siteForPage.getSiteUrl(), "/") :
                pageUrl.replace(siteForPage.getSiteUrl(), "");

        try {
            Document doc = new SiteConnector(pageUrl).getDoc();
            Page page = existPageByPath(path) ? pageRepository.findPageByPath(path).get() :
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