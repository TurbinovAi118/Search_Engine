package engine.repositories;

import engine.models.Lemma;
import engine.models.Site;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Repository
public interface LemmaRepository extends CrudRepository<Lemma, Integer> {

    @Modifying
    @Query(value = "INSERT INTO lemma (site_id, lemma, frequency) VALUE (:site_id, :lemma, 1) ON DUPLICATE KEY UPDATE frequency = frequency + 1",  nativeQuery = true)
    @Transactional
    void add(Integer site_id, String lemma);

    Integer countAllBySite(Site site);

    @Query(value = "SELECT COUNT(*) from `lemma`", nativeQuery = true)
    Integer countAllLemmas();

    Lemma findLemmaByLemmaAndSite(String lemma, Site site);

    List<Lemma> findLemmaBySite(Site site);

    @Query(value = "SELECT frequency FROM `lemma` WHERE lemma = :lemma AND site_id LIKE :siteId", nativeQuery = true)
    Integer findFrequencyByLemmaAndSite(String lemma, String siteId);

}
