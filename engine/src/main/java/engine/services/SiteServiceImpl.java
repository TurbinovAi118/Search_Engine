package engine.services;

import engine.dto.ApiResponse;
import engine.models.Site;
import engine.models.enums.SiteStatus;
import engine.repositories.SiteRepository;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SiteServiceImpl implements SiteService{

    private final SiteRepository siteRepository;

    public SiteServiceImpl(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @Override
    public ApiResponse addCustom(String url) {
        Document doc;
        ApiResponse response = new ApiResponse();
        if (url.isEmpty()){
            response.setResult(false);
            response.setError("Введите адресс");
            return response;
        }
        try {
            doc = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/110.0.0.0 YaBrowser/23.3.4.603 Yowser/2.5 Safari/537.36").get();
        } catch (Exception e) {
            e.printStackTrace();
            response.setResult(false);
            response.setError("Введен не верный адресс.");
            return response;
        }
        if (existsBySiteUrl(url)){
            response.setResult(false);
            response.setError("Такой сайт уже присутствует в базе данных.");
            return response;
        }
        String name = doc.select("head").select("title").text();
        Site site = new Site(SiteStatus.NOT_INDEXED, LocalDateTime.now(), null, url, name);
        siteRepository.save(site);
        response.setResult(true);
        return response;
    }

    @Override
    public ApiResponse add(Site site) {
        ApiResponse response = new ApiResponse();
        siteRepository.save(site);
        response.setResult(true);
        return response;
    }

    @Override
    public Optional<Site> findById(int id) {
        return siteRepository.findById(id);
    }

    @Override
    public List<Site> list() {
        List<Site> siteList = new ArrayList<>();
        siteRepository.findAll().forEach(siteList::add);
        return siteList;
    }

    @Override
    public void delete(int id) {
        findById(id).ifPresent(siteRepository::delete);
    }

    @Override
    public void patch(Site site) {
        Optional<Site> siteOptional = findById(site.getId());
        if (siteOptional.isPresent()) {
            Site siteToPatch = siteOptional.get();
            if (site.getLastError() != null && !siteToPatch.getLastError().equals(site.getLastError())) {
                siteToPatch.setLastError(site.getLastError());
            }
            if (!siteToPatch.getStatus().equals(site.getStatus())) {
                siteToPatch.setStatus(site.getStatus());
            }

            siteToPatch.setStatusTime(LocalDateTime.now());

            siteRepository.save(siteToPatch);
        }
    }

    @Override
    public Optional<Site> findBySiteUrl(String url) {
        return siteRepository.findBySiteUrl(url);
    }

    @Override
    public Boolean existsBySiteUrl(String url) {
        return siteRepository.existsBySiteUrl(url);
    }


}
