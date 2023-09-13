package engine.services.implementation;

import engine.models.Index;
import engine.models.Lemma;
import engine.models.Page;
import engine.models.Site;
import engine.repositories.IndexRepository;
import engine.repositories.LemmaRepository;
import engine.services.LemmaService;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LemmaServiceImpl implements LemmaService {

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    private static final String ruRegex = "[а-яА-Я]+";
    private static final String enRegex = "[a-zA-Z]+";

    private LuceneMorphology ruMorph;
    private LuceneMorphology enMorph;

    {
        try {
            ruMorph = new RussianLuceneMorphology();
            enMorph = new EnglishLuceneMorphology();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addLemmas(Page page) {
        Map<String, Integer> lemmas = null;

        boolean invalid = String.valueOf(page.getResponseCode()).startsWith("4") || String.valueOf(page.getResponseCode()).startsWith("5");

        if (!invalid) {
            lemmas = parseLemmas(page.getContent(), false);
        }

        if (lemmas != null) {
            for (String lemma : lemmas.keySet()) {
                lemmaRepository.add(page.getSite().getId(), lemma);
                List<Lemma> indexLemmas = lemmaRepository.findLemmaByLemmaAndSite(lemma, String.valueOf(page.getSite().getId()));
                for (Lemma indexLemma : indexLemmas) {
                    indexRepository.save(new Index(page, indexLemma, lemmas.get(lemma)));
                }
            }
        }
    }

    @Override
    public Map<String, Integer> parseLemmas(String string, Boolean isText) {
        String regex = "[^а-яА-Яa-zA-Z\s]";

        String elementRegex = "<[^<>]*>";

        String text = isText ? string : Jsoup.parse(string).text().replaceAll(elementRegex, "");

        text = text.replaceAll(regex, "")
                .replaceAll("\s+", " ").toLowerCase(Locale.ROOT).trim();

        List<String> allLemmas = new ArrayList<>();

        for (String word : text.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            List<String> wordInfo = word.trim().matches(ruRegex) ? ruMorph.getMorphInfo(word) :
                    word.trim().matches(enRegex) ? enMorph.getMorphInfo(word) : new ArrayList<>();
            if (wordInfo.isEmpty())
                continue;
            List<String> checkList = new ArrayList<>();
            wordInfo.stream().map(info -> info.split(" ")).forEach(list -> checkList.addAll(List.of(list)));
            if (!checkList.contains("ПРЕДЛ") && !checkList.contains("СОЮЗ") && !checkList.contains("ЧАСТ") && !checkList.contains("МЕЖД")) {
                allLemmas.addAll(wordInfo);
            }
        }

        List<String> sortedLemmas = allLemmas.stream()
                .map(wordInfo -> wordInfo.split("\\|")[0])
                .collect(Collectors.toList());

        List<String> uniqueLemmas = sortedLemmas.stream().distinct().collect(Collectors.toList());

        Map<String, Integer> lemmas = new HashMap<>();

        uniqueLemmas.forEach(lemma -> lemmas.put(lemma, Collections.frequency(sortedLemmas, lemma)));

        return lemmas;
    }

    @Override
    public List<String> getNormalForms(String word) {
        return word.matches(ruRegex) ? ruMorph.getNormalForms(word) :
                word.matches(enRegex) ? enMorph.getNormalForms(word) : new ArrayList<>();
    }

    @Override
    public Integer countLemmasBySiteId(Site site) {
        return lemmaRepository.countAllBySite(site);
    }

    @Override
    public Integer countAllLemmas() {
        return Math.toIntExact(lemmaRepository.count());
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
