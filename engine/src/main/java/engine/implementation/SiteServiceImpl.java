package engine.implementation;

import engine.dto.ApiResponse;
import engine.models.Site;
import engine.models.enums.SiteStatus;
import engine.repositories.SiteRepository;
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
public class SiteServiceImpl implements SiteService {

    private final SiteRepository siteRepository;

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

            siteToPatch.setLastError(site.getLastError());
            siteToPatch.setSiteName(site.getSiteName());
            siteToPatch.setSiteUrl(site.getSiteUrl());
            siteToPatch.setStatus(site.getStatus());
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
