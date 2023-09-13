package engine.repositories;

import engine.models.Site;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SiteRepository extends CrudRepository<Site, Integer> {
    Optional<Site> findBySiteUrl(String url);

    Boolean existsBySiteUrl(String url);
}