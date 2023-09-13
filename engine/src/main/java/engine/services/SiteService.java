package engine.services;

import engine.dto.ApiResponse;
import engine.models.Site;

import java.util.List;
import java.util.Optional;

public interface SiteService {
    ApiResponse add(Site site);

    Optional<Site> findById(int id);

    List<Site> list();

    void delete(int id);

    void patch(Site site);

    Optional<Site> findBySiteUrl(String url);

    Boolean existsBySiteUrl(String url);
}
