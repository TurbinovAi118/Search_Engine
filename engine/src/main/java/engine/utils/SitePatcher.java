package engine.utils;

import engine.models.Site;
import engine.repositories.SiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class SitePatcher {

    private final SiteRepository siteRepository;

    public void patch(Site site) {
        Optional<Site> siteOptional = siteRepository.findById(site.getId());
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

}
