package engine.services;

import engine.models.Index;
import engine.models.Lemma;
import engine.models.Page;
import engine.models.Site;
import engine.repositories.IndexRepository;
import engine.repositories.LemmaRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import javax.print.Doc;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LemmaServiceImpl implements LemmaService {

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
//    private final PageService pageService;

    private LuceneMorphology luceneMorph;
    {
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addLemmas(Page page) {
        Map<String, Integer> lemmas = null;

        boolean invalid = String.valueOf(page.getResponseCode()).startsWith("4") || String.valueOf(page.getResponseCode()).startsWith("5");

        if (!invalid){
            String siteUrl = page.getSite().getSiteUrl().endsWith("/") ?
                    page.getSite().getSiteUrl().replaceFirst(".$","") : page.getSite().getSiteUrl();
            String url = siteUrl + page.getPath();

            try {
                Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/110.0.0.0 YaBrowser/23.3.4.603 Yowser/2.5 Safari/537.36")
                        .referrer("https://www.google.com")
                        .get();
                lemmas = parseLemmas(doc.html(), false);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        if (lemmas != null) {
            for (String lemma : lemmas.keySet()) {
                lemmaRepository.add(page.getSite().getId(), lemma);
                List<Lemma> indexLemmas = lemmaRepository.findLemmaByLemmaAndSite(lemma, String.valueOf(page.getSite().getId()));
                for (Lemma indexLemma : indexLemmas){
                    indexRepository.save(new Index(page, indexLemma, lemmas.get(lemma)));
                }
//                indexLemmas.forEach(indexLemma -> indexRepository.save(new Index(page, indexLemma, lemmas.get(lemma))));
            }
            System.out.println(page.getId() + " - " +  lemmas.size());
        }
    }

    @Override
    public Map<String, Integer> parseLemmas(String string, Boolean isText) {

        String regex = "[^а-яА-Я\s]";

        String text = isText ? string : Jsoup.parse(string).text();

        text = text.replaceAll(regex, "")
                .replaceAll("\s+", " ").toLowerCase(Locale.ROOT).trim();


        List<String> allLemmas = new ArrayList<>();

        for (String word : text.split(" ")){
            if (word.isEmpty()){
               continue;
            }
            allLemmas.addAll(luceneMorph.getMorphInfo(word));
        }

        List<String> sortedLemmas = allLemmas.stream()
                .filter(wordInfo -> !wordInfo.contains("ПРЕДЛ") && !wordInfo.contains("СОЮЗ")
                        && !wordInfo.contains("ЧАСТ") && !wordInfo.contains("МЕЖД"))
                .map(wordInfo ->wordInfo.split("\\|")[0])
                        //wordInfo.replace(wordInfo.substring(wordInfo.lastIndexOf("|")), "")
                .collect(Collectors.toList());

        List<String> uniqueLemmas = sortedLemmas.stream().distinct().collect(Collectors.toList());

        Map<String, Integer> lemmas = new HashMap<>();

        uniqueLemmas.forEach(lemma -> lemmas.put(lemma, Collections.frequency(sortedLemmas, lemma)));

        return lemmas;
    }

    @Override
    public Integer countLemmasBySiteId(Site site) {
        return lemmaRepository.countAllBySite(site);
    }

    @Override
    public Integer countAllLemmas() {
        return lemmaRepository.countAllLemmas();
    }

    @Override
    public List<Lemma> findLemmasBySite(Site site) {
        return lemmaRepository.findLemmaBySite(site);
    }

    @Override
    public List<Lemma> findLemmaByLemmaAndSite(String lemma, String siteId) {
        return lemmaRepository.findLemmaByLemmaAndSite(lemma, siteId);
    }

    @Override
    public Integer findFrequencyByLemmaAndSite(String lemma, String siteId) {
        return lemmaRepository.findFrequencyByLemmaAndSite(lemma, siteId).stream().findFirst().orElse(0);
    }



}
