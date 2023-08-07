package engine.services;

import engine.models.Lemma;
import engine.models.Page;
import engine.models.Site;

import java.io.IOException;
import java.util.List;
import java.util.Map;


public interface LemmaService {

    void addLemmas(Page page);

    Map<String, Integer> parseLemmas(String html, Boolean isText) throws IOException;

    Integer countLemmasBySiteId(Site site);

    Integer countAllLemmas();

    List<Lemma> findLemmasBySite(Site site);

    List<Lemma> findLemmaByLemmaAndSite(String lemma, String siteId);

    Integer findFrequencyByLemmaAndSite(String lemma, String siteId);

    List<String> getNormalForms(String word);

}
