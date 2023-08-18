package engine.implementation;

import engine.dto.ApiData;
import engine.dto.ApiResponse;
import engine.models.Lemma;
import engine.models.Page;
import engine.models.Site;
import engine.repositories.IndexRepository;
import engine.services.LemmaService;
import engine.services.PageService;
import engine.services.SearchService;
import engine.services.SiteService;
import lombok.AllArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final LemmaService lemmaService;
    private final SiteService siteService;
    private final IndexRepository indexRepository;
    private final PageService pageService;

    @Override

    public ApiResponse search(Map<String, String> body) {
        ApiResponse response = new ApiResponse();

        int limit = Integer.parseInt(body.get("limit"));
        String query = body.get("query").toLowerCase(Locale.ROOT);
        Site site = body.get("site") != null ? siteService.findBySiteUrl(body.get("site"))
                .orElseGet(() -> siteService.findBySiteUrl(body.get("site") + "/").get()) : null;

        Map<String, Integer> sortedQueryLemmas = parseFilterAndSortQueryLemmas(query, site);

        System.out.println(sortedQueryLemmas);

        if (sortedQueryLemmas == null || sortedQueryLemmas.size() == 0){
            response.setResult(false);
            response.setError("Задан неверный поисковый запрос.");
            return response;
        }

        Map<Page, Float> sortedFoundPages = findAndSortResultPages(sortedQueryLemmas, site);

        if (sortedFoundPages == null || sortedFoundPages.size() == 0){
            response.setResult(false);
            response.setError("По данному запросу не найдено результатов");
            return response;
        }

        List<ApiData> data = collectResponse(sortedFoundPages, limit, query, sortedQueryLemmas);

        response.setResult(true);
        response.setData(data);
        return response;
    }

    private Map<String, Integer> parseFilterAndSortQueryLemmas(String query, Site site){
        Integer lemmasAmount = lemmaService.countAllLemmas();
        double frequencyLimit = lemmasAmount * 0.005;

        Map<String, Integer> queryLemmas = new HashMap<>();
        try {
            queryLemmas = lemmaService.parseLemmas(query, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (queryLemmas.size() == 0)
            return null;

        queryLemmas.replaceAll((k, v) -> lemmaService.findFrequencyByLemmaAndSite(k, site != null ? String.valueOf(site.getId()) : "%"));

        queryLemmas.values().removeIf(value -> value == 0);

        queryLemmas.keySet().removeIf(searchLemma ->
                (double) (lemmasAmount / lemmaService.findFrequencyByLemmaAndSite(
                        searchLemma, site != null ? String.valueOf(site.getId()) : "%")) < frequencyLimit);

        Map<String, Integer> resultMap = new HashMap<>();
        for(String word : query.split(" ")){
            Map<String, Integer> cacheMap = new HashMap<>();
            ArrayList<String> list = (ArrayList<String>) lemmaService.getNormalForms(word);

            for (String normalWord : list){
                if (queryLemmas.containsKey(normalWord)){
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

    private Map<Page, Float> findAndSortResultPages(Map<String, Integer> queryLemmas, Site site){
        String firstLemma = queryLemmas.keySet().stream().findFirst().get();

        List<Lemma> firstLemmas = lemmaService.findLemmaByLemmaAndSite(firstLemma, site != null ? String.valueOf(site.getId()) : "%");
        Map<Page, Float> foundPages = new HashMap<>();
        firstLemmas.forEach(lemma -> {
            for (Page page : findPagesByLemmas(lemma.getId())) {
                foundPages.put(page, indexRepository.findByLemmaAndPage(lemma, page).getRank());
            }
        });

        queryLemmas.keySet().forEach(queryLemma -> {
            List<Lemma> lemmas = lemmaService.findLemmaByLemmaAndSite(queryLemma, site != null ? String.valueOf(site.getId()) : "%");
            for (Lemma lemma : lemmas) {
                foundPages.keySet().forEach(page -> {
                    if (indexRepository.existsByPageAndLemma(page, lemma)) {
                        foundPages.put(page, foundPages.get(page) + indexRepository.findByLemmaAndPage(lemma, page).getRank());
                    }
                });
                foundPages.keySet().removeIf(page -> !indexRepository.existsByPageAndLemma(page, lemma));
            }
        });

        if (foundPages.size() == 0){
            return null;
        }

        return foundPages.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private Map<String, Integer> defineQueryWordsInLemmas(String[] queryList, Map<String, Integer> sortedQueryLemmas){
        Map<String, Integer> queryWordsToLemmas = new HashMap<>();
        for (String queryWord : queryList){

            Map<String, Integer> lemmasInQuery = new HashMap<>();
            lemmaService.getNormalForms(queryWord).forEach(word -> {
                if (sortedQueryLemmas.containsKey(word)) {
                    lemmasInQuery.put(word, sortedQueryLemmas.get(word));
                }
            });
            if (lemmasInQuery.size() > 0)
                queryWordsToLemmas.put(queryWord, lemmasInQuery.values().stream().reduce(Integer::max).get());
        }
        return queryWordsToLemmas;
    }

    private List<ApiData> collectResponse(Map<Page, Float> sortedFoundPages, int limit, String query, Map<String, Integer> sortedLemmas){
        List<ApiData> data = new ArrayList<>();
        float maxRelevancy = sortedFoundPages.values().stream().max(Float::compare).get();

        String[] queryList = query.split(" ");
        Map<String, Integer> queryWordsToLemmas = defineQueryWordsInLemmas(queryList, sortedLemmas);

        for (Page page : sortedFoundPages.keySet()){

            String url = page.getSite().getSiteUrl().endsWith("/") ? page.getSite().getSiteUrl().replaceFirst(".$","") :
                    page.getSite().getSiteUrl();
            String siteName = page.getSite().getSiteName();
            String path = page.getPath();
            float relevance = sortedFoundPages.get(page) / maxRelevancy;

            Document doc = Jsoup.parse(page.getContent());
            String title = doc.select("title").text();


            String finalSnippet = null;
            try {
                finalSnippet = parseSnippet(queryWordsToLemmas, doc, query);
            } catch (Exception e){
                System.out.println("Snippet parsing failed in " + url + path + ". Query is: " + query);
                e.printStackTrace();
            }

            if (finalSnippet == null)
                continue;

            data.add(new ApiData(url, siteName, path, title, finalSnippet, relevance));
            if (data.size() == limit) break;
        }
        return data;
    }

    private String parseSnippet(Map<String, Integer> queryLemmas, Document doc, String query){
        String regex = "[^а-яА-Яa-zA-Z\s]";
        String maxRelevanceQueryWord = queryLemmas.entrySet().stream()
                .min(Map.Entry.comparingByValue(Comparator.reverseOrder())).get().getKey();

        Elements foundElements = doc.body().getElementsContainingOwnText(String.join(" ", queryLemmas.keySet())).size() > 0 ?
                doc.body().getElementsContainingOwnText(String.join(" ", queryLemmas.keySet())) :
                doc.body().getElementsContainingOwnText(maxRelevanceQueryWord);

        String elementText;
        if (foundElements.size() > 0)
            elementText = foundElements.get(0).text();
        else
            return null;

        List<String> snippetArray = new ArrayList<>(getSnippetArray(query, elementText));
        List<String> snippetLower = snippetArray.stream()
                .map(word -> word = word.toLowerCase(Locale.ROOT).replaceAll(regex, ""))
                .collect(Collectors.toList());

        if (snippetLower.containsAll(Arrays.asList(query.toLowerCase(Locale.ROOT).split(" "))) && !snippetArray.isEmpty()){

            for (String word : query.toLowerCase(Locale.ROOT).split(" ")){
                snippetArray.set(snippetLower.indexOf(word), "<b>" + snippetArray.get(snippetLower.indexOf(word)) + "</b>");
            }
        } else {
            snippetArray = new ArrayList<>(getSnippetArray(maxRelevanceQueryWord, elementText));
            snippetLower = snippetArray.stream()
                    .map(word -> word = word.toLowerCase(Locale.ROOT).replaceAll(regex, ""))
                    .collect(Collectors.toList());
            snippetArray.set(snippetLower.indexOf(maxRelevanceQueryWord), "<b>" + snippetArray.get(snippetLower.indexOf(maxRelevanceQueryWord)) + "</b>");
        }

        String finalSnippet = "";
        if (!snippetArray.isEmpty())
            finalSnippet = (String.join(" ", snippetArray));

        return "..." + finalSnippet + "...";
    }

    private List<String> getSnippetArray(String query, String elementText) {
        int indexOfCommon = elementText.toLowerCase(Locale.ROOT).indexOf(query.toLowerCase(Locale.ROOT));
        if (indexOfCommon == -1){
            return new ArrayList<>();
        }
        int indexOfFirstDot = elementText.substring(0, indexOfCommon).lastIndexOf(".");
        int indexOfLastDot = elementText.substring(indexOfCommon).indexOf(".");

        String startOfSnippet = elementText.substring(indexOfFirstDot == -1 ? 0 : indexOfFirstDot+1, indexOfCommon);
        String endOfSnippet = elementText.substring(indexOfCommon, (indexOfLastDot == -1 ? elementText.length() : indexOfCommon + indexOfLastDot));
        String text = startOfSnippet + endOfSnippet;
        text = text.trim();

        return List.of(text.split(" "));
    }

    private List<Page> findPagesByLemmas(int lemmaId) {
        List<Integer> pagesId = indexRepository.findPagesByLemma(lemmaId);
        List<Page> foundPages = new ArrayList<>();
        pagesId.forEach(id -> pageService.findById(id).ifPresent(foundPages::add));

        return foundPages;
    }

}