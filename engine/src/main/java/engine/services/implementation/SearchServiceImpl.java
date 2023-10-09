package engine.services.implementation;

import engine.dto.search.ApiSearchResponse;
import engine.dto.search.SearchData;
import engine.models.Lemma;
import engine.models.Page;
import engine.repositories.IndexRepository;
import engine.services.LemmaService;
import engine.services.PageService;
import engine.services.SearchService;
import engine.services.SiteService;
import lombok.AllArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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

    public ApiSearchResponse search(Map<String, String> body) {
        ApiSearchResponse response = new ApiSearchResponse();

        int limit = Integer.parseInt(body.get("limit"));
        int offset = Integer.parseInt(body.get("offset"));
        String query = body.get("query").toLowerCase(Locale.ROOT);
        String site = body.get("site") != null ? String.valueOf(siteService.findBySiteUrl(body.get("site")).get().getId()) : "%";

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

        List<SearchData> data = collectResponse(sortedFoundPages, limit, query, sortedQueryLemmas);

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

        int pageAmount = pageService.countAllPages();
        double frequencyLimit = pageAmount * 0.75;

        Map<String, Integer> queryLemmas = new HashMap<>();
        try {
            queryLemmas = lemmaService.parseLemmas(query, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (queryLemmas.size() == 0)
            return null;

        queryLemmas.replaceAll((k, v) -> lemmaService.findFrequencyByLemmaAndSite(k, site));

        queryLemmas.values().removeIf(value -> value == 0);

        if (queryLemmas.size() > 1)
            queryLemmas.values().removeIf(value -> value > frequencyLimit);



        Map<String, Integer> resultMap = new HashMap<>();
        for (String word : query.split(" ")) {
            Map<String, Integer> cacheMap = new HashMap<>();
            ArrayList<String> list = (ArrayList<String>) lemmaService.getNormalForms(word);

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


        List<Lemma> firstLemmas = lemmaService.findLemmaByLemmaAndSite(firstLemma, site);
        Map<Page, Float> foundPages = new HashMap<>();
        firstLemmas.forEach(lemma -> {
            for (Page page : findPagesByLemmas(lemma.getId())) {
                foundPages.put(page, indexRepository.findByLemmaAndPage(lemma, page).getRank());
            }
        });

        for (String queryLemma : queryLemmas.keySet()){
            List<Lemma> lemmas = lemmaService.findLemmaByLemmaAndSite(queryLemma, site);
            for (Lemma lemma : lemmas) {
                for (Page page : findPagesByLemmas(lemma.getId()))
                    foundPages.put(page, indexRepository.findByLemmaAndPage(lemma, page).getRank());
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

    private Map<String, Integer> defineQueryWordsInLemmas(String[] queryList, Map<String, Integer> sortedQueryLemmas) {
        Map<String, Integer> queryWordsToLemmas = new HashMap<>();
        for (String queryWord : queryList) {

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

    private List<SearchData> collectResponse(Map<Page, Float> sortedFoundPages, int limit, String query, Map<String, Integer> sortedLemmas) {
        List<SearchData> data = new ArrayList<>();
        float maxRelevancy = sortedFoundPages.values().stream().max(Float::compare).get();

        String[] queryList = query.split(" ");
        Map<String, Integer> queryWordsToLemmas = defineQueryWordsInLemmas(queryList, sortedLemmas);

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
                finalSnippet = parseSnippet(queryWordsToLemmas, doc, query);
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

    private String parseSnippet(Map<String, Integer> queryLemmas, Document doc, String query) {
        String maxRelevanceQueryWord = queryLemmas.entrySet().stream()
                .min(Map.Entry.comparingByValue(Comparator.reverseOrder())).get().getKey();

        Elements foundElements = doc.body().getElementsContainingOwnText(String.join(" ", queryLemmas.keySet())).size() > 0 ?
                doc.body().getElementsContainingOwnText(String.join(" ", queryLemmas.keySet())) :
                doc.body().getElementsContainingOwnText(maxRelevanceQueryWord);

        String elementText;
        String snippet = "";
        for (Element element : foundElements) {
            elementText = element.text();
            try {
                snippet = markup(elementText, query);
            } catch (Exception e) {
                continue;
            }
            if (!snippet.isEmpty())
                break;
        }

        if (snippet.isEmpty()) {
            return null;
        }

        String finalSnippet = getSnippetArray(snippet);

        return "..." + finalSnippet + "...";
    }

    private String markup(String text, String query) {
        String regex = "[^а-яА-Яa-zA-Z\s]";

        String[] textWords = text.split(" ");
        List<String> queryWords = Arrays.asList(query.split(" "));

        List<List<String>> textLemmas = new ArrayList<>();
        List<List<String>> queryLemmas = new ArrayList<>();
        List<String> textList = new ArrayList<>();

        for (String word : textWords) {
            textList.add(word);
            try {
                textLemmas.add(lemmaService.getNormalForms(word.replaceAll(regex, "").toLowerCase(Locale.ROOT)));
            } catch (Exception e) {
                textLemmas.add(new ArrayList<>());
            }
        }

        queryWords.forEach(word -> queryLemmas.add(lemmaService.getNormalForms(word)));

        StringJoiner queryLemmasJoiner = new StringJoiner(" ");
        queryLemmas.forEach(list -> queryLemmasJoiner.add(String.join(" ", list)));

        int firstWordIndex = textLemmas.indexOf(textLemmas.stream().filter(list -> list.containsAll(queryLemmas.get(0)))
                .collect(Collectors.toList()).get(0));
        List<List<String>> textLemmasSub;
        boolean forWhile = true;
        if (firstWordIndex == -1)
            return "";

        while (forWhile) {
            textLemmasSub = textLemmas.subList(firstWordIndex, textLemmas.size());
            StringJoiner textChecker = new StringJoiner(" ");
            textLemmasSub.forEach(list -> textChecker.add(String.join(" ", list)));
            if (textChecker.toString().startsWith(queryLemmasJoiner.toString()))
                forWhile = false;
            else {
                textLemmasSub = textLemmas.subList(firstWordIndex + 1, textLemmas.size());
                int common = (!textLemmasSub.contains(queryLemmas.get(0))) ? -1 : textLemmasSub.indexOf(queryLemmas.get(0));
                firstWordIndex += (common == -1) ? -(firstWordIndex + 1) : common + 1;
            }
            if (firstWordIndex == -1)
                return "";
        }
        for (int i = 0; i < queryWords.size(); i++) {
            textList.set(firstWordIndex + i, "<b>" + textList.get(firstWordIndex + i) + "</b>");
        }

        return String.join(" ", textList);
    }

    private static String getSnippetArray(String elementText) {
        int indexOfCommon = elementText.toLowerCase(Locale.ROOT).indexOf("<b>");
        int lastIndexOfCommon = elementText.toLowerCase(Locale.ROOT).lastIndexOf("</b>") + 4;
        if (indexOfCommon == -1) {
            return "";
        }
        int indexOfFirstComma = elementText.substring(0, indexOfCommon).lastIndexOf(",");
        int indexOfLastComma = elementText.substring(lastIndexOfCommon).indexOf(",");

        String startOfSnippet = elementText.substring(indexOfFirstComma == -1 ? 0 : indexOfFirstComma + 1, lastIndexOfCommon);
        String endOfSnippet = elementText.substring(lastIndexOfCommon, (indexOfLastComma == -1 ? elementText.length() : lastIndexOfCommon + indexOfLastComma));
        String text = startOfSnippet + endOfSnippet;
        text = text.trim();

        return text;
    }

    private List<Page> findPagesByLemmas(int lemmaId) {
        List<Integer> pagesId = indexRepository.findPagesByLemma(lemmaId);
        List<Page> foundPages = new ArrayList<>();
        pagesId.forEach(id -> pageService.findById(id).ifPresent(foundPages::add));

        return foundPages;
    }

}