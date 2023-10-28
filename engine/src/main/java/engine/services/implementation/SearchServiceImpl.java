package engine.services.implementation;

import engine.dto.search.ApiSearchResponse;
import engine.dto.search.SearchData;
import engine.models.Lemma;
import engine.models.Page;
import engine.repositories.IndexRepository;
import engine.repositories.LemmaRepository;
import engine.repositories.PageRepository;
import engine.repositories.SiteRepository;
import engine.services.SearchService;
import engine.utils.LemmaParser;
import engine.utils.SnippetParser;
import lombok.AllArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final LemmaParser lemmaParser;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;

    @Override
    public ApiSearchResponse search(Map<String, String> body) {
        ApiSearchResponse response = new ApiSearchResponse();

        int limit = Integer.parseInt(body.get("limit"));
        int offset = Integer.parseInt(body.get("offset"));
        String query = body.get("query").toLowerCase(Locale.ROOT);
        String site = body.get("site") != null ?
                //String.valueOf(
                        siteRepository.findBySiteUrl(body.get("site")).isPresent() ? String.valueOf(siteRepository.findBySiteUrl(body.get("site"))) : String.valueOf(siteRepository.findBySiteUrl(body.get("site")+"/").get().getId()) :
                "%";

        Map<String, Integer> sortedQueryLemmas = parseFilterAndSortQueryLemmas(query, site);

        if (sortedQueryLemmas == null || sortedQueryLemmas.size() == 0) {
            response.setResult(false);
            response.setError("Задан неверный поисковый запрос.");
            return response;
        }

        Map<Page, Float> sortedFoundPages = findAndSortResultPages(sortedQueryLemmas, site);

        if (sortedFoundPages == null) {
            response.setResult(false);
            response.setError("По данному запросу не найдено результатов");
            return response;
        }

        List<SearchData> data = collectResponse(sortedFoundPages, query, sortedQueryLemmas);

        if (data.size() == 0) {
            response.setResult(false);
            response.setError("По данному запросу не найдено результатов");
            return response;
        }

        response.setResult(true);
        response.setCount(data.size());
        response.setData(data.size() > limit ? data.subList(offset, Math.min(offset + limit, data.size())) : data);
        return response;
    }

    private Map<String, Integer> parseFilterAndSortQueryLemmas(String query, String site) {

        int pageAmount = Math.toIntExact(pageRepository.count());
        double frequencyLimit = pageAmount * 0.75;

        Map<String, Integer> queryLemmas = new HashMap<>();
        try {
            queryLemmas = lemmaParser.parseLemmas(query, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (queryLemmas.size() == 0)
            return null;

        queryLemmas.replaceAll((k, v) -> lemmaRepository.findFrequencyByLemmaAndSite(k, site).stream().findFirst().orElse(0));

        queryLemmas.values().removeIf(value -> value == 0);

        if (queryLemmas.size() > 1)
            queryLemmas.values().removeIf(value -> value > frequencyLimit);

        Map<String, Integer> resultMap = new HashMap<>();
        for (String word : query.split(" ")) {
            Map<String, Integer> cacheMap = new HashMap<>();
            ArrayList<String> list = (ArrayList<String>) lemmaParser.getNormalForms(word);

            for (String normalWord : list) {
                if (queryLemmas.containsKey(normalWord)) {
                    cacheMap.put(normalWord, queryLemmas.get(normalWord));
                }
            }
            cacheMap = cacheMap.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            if (!cacheMap.isEmpty()) {
                String firstKey = cacheMap.keySet().stream().findFirst().get();
                resultMap.put(firstKey, cacheMap.get(firstKey));
            }
        }

        return resultMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private Map<Page, Float> findAndSortResultPages(Map<String, Integer> queryLemmas, String site) {
        String firstLemma = queryLemmas.keySet().stream().findFirst().get();

        List<Lemma> firstLemmas = lemmaRepository.findLemmaByLemmaAndSite(firstLemma, site);
        Map<Page, Float> foundPages = new HashMap<>();
        firstLemmas.forEach(lemma -> {
            foundPages.putAll(findPagesByLemmas(lemma.getId()));
        });

        for (String queryLemma : queryLemmas.keySet()){
            List<Lemma> lemmas = lemmaRepository.findLemmaByLemmaAndSite(queryLemma, site);
            for (Lemma lemma : lemmas) {
                for (Page page : findPagesByLemmas(lemma.getId()).keySet())
                    foundPages.put(page, indexRepository.findByLemmaAndPage(lemma, page).get(0).getRank());
            }
        }

        if (foundPages.size() == 0) {
            return null;
        }

        return foundPages.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private List<SearchData> collectResponse(Map<Page, Float> sortedFoundPages, String query, Map<String, Integer> sortedLemmas) {
        List<SearchData> data = new ArrayList<>();
        float maxRelevancy = sortedFoundPages.values().stream().max(Float::compare).get();

        SnippetParser parser = new SnippetParser(lemmaParser);

        String[] queryList = query.split(" ");
        Map<String, Integer> queryWordsToLemmas = parser.defineQueryWordsInLemmas(queryList, sortedLemmas);

        for (Page page : sortedFoundPages.keySet()) {

            String url = page.getSite().getSiteUrl().endsWith("/") ? page.getSite().getSiteUrl().replaceFirst(".$", "") :
                    page.getSite().getSiteUrl();
            String siteName = page.getSite().getSiteName();
            String path = page.getPath();
            float relevance = sortedFoundPages.get(page) / maxRelevancy;

            Document doc = Jsoup.parse(page.getContent());
            String title = doc.select("title").text();


            String finalSnippet = null;
            try {
                finalSnippet = parser.parseSnippet(doc, queryWordsToLemmas, query);
            } catch (Exception e) {
                System.out.println("Snippet parsing failed in " + url + path + ". Query is: " + query);
                e.printStackTrace();
            }

            if (finalSnippet == null)
                continue;

            data.add(new SearchData(url, siteName, path, title, finalSnippet, relevance));
        }
        return data;
    }

    private Map<Page, Float> findPagesByLemmas(int lemmaId) {
        List<Integer> pagesId = indexRepository.findPagesByLemma(lemmaId);
        Map<Page, Float> foundPages = new HashMap<>();

        Lemma lemma = lemmaRepository.findLemmaById(lemmaId);

        for (Integer id : pagesId){
            Page curPage = pageRepository.findById(id).orElse(null);
            if (curPage == null)
                continue;
            foundPages.put(curPage, indexRepository.findByLemmaAndPage(lemma, curPage).get(0).getRank());
        }

        return foundPages;
    }

}