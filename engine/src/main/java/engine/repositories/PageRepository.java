package engine.repositories;

import engine.models.Page;
import engine.models.Site;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends CrudRepository<Page, Integer> {

    List<Page> findPagesBySiteId(int siteId);

    Integer countAllBySite(Site site);

    Optional<Page> findPageByPath(String path);

    Boolean existsByPath(String path);

    Optional<Page> findById(int id);
}
