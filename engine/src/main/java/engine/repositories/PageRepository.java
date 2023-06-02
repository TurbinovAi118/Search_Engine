package engine.repositories;

import engine.models.Page;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageRepository extends CrudRepository<Page, Integer> {

    @Query(value = "select * from page where page.site_id = ?1", nativeQuery = true)
    List<Page> getPageBySiteId(int id);
}