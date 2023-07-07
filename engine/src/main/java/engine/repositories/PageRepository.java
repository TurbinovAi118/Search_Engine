package engine.repositories;

import engine.models.Page;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends CrudRepository<Page, Integer> {

    @Query(value = "select * from page where page.site_id = ?1", nativeQuery = true)
    List<Page> getPageBySiteId(int id);

    @Query(value = "SELECT COUNT(*) FROM `page` where page.site_id = ?1", nativeQuery = true)
    Integer countPagesBySiteId(int id);

    @Query(value = "select * from page where `path` = ?1 ", nativeQuery = true)
    Optional<Page> getPageByPath(String path);

    Boolean existsByPath(String path);
}
