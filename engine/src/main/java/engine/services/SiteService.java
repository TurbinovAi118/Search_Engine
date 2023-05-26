package engine.services;

import engine.dto.ApiResponse;
import engine.models.Site;
import engine.models.enums.SiteStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;


public interface SiteService {

    ApiResponse add(String url);

    ResponseEntity<Site> findById(int id);

    List<Site> list();

    ResponseEntity<?> delete(int id);

    ResponseEntity<Site> patch(Site site);
}
