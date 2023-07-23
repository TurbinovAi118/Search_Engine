package engine.repositories;

import engine.models.Index;
import engine.models.Lemma;
import engine.models.Page;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndexRepository extends CrudRepository<Index, Integer> {

    @Query(value = "SELECT page_id FROM `search_index` WHERE lemma_id = :lemma_id", nativeQuery = true)
    List<Integer> findPagesByLemma(int lemma_id);

    boolean existsByPageAndLemma(Page page, Lemma lemma);

    Index findByLemmaAndPage(Lemma lemma, Page page);


}
