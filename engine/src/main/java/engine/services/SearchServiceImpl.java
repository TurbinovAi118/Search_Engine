package engine.services;

import engine.dto.ApiData;
import engine.dto.ApiResponse;
import engine.models.Lemma;
import engine.models.Page;
import engine.models.Site;
import engine.repositories.IndexRepository;
import lombok.AllArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class SearchServiceImpl implements SearchService{

    private final LemmaService lemmaService;
    private final SiteService siteService;
    private final IndexRepository indexRepository;
    private final PageService pageService;

    @Override
    public ApiResponse search(Map<String, String> body) {
        ApiResponse response = new ApiResponse();

        int limit = Integer.parseInt(body.get("limit"));
        String query = body.get("query");

        Map<String, Integer> queryLemmas = new HashMap<>();
        try {
            queryLemmas = lemmaService.parseLemmas(query, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (queryLemmas.size() == 0){
            response.setResult(false);
            response.setError("Задан неверный поисковый запрос.");
            return response;
        }

        Site site = body.get("site") != null ? siteService.findBySiteUrl(body.get("site"))
                .orElseGet(() -> siteService.findBySiteUrl(body.get("site") + "/").get()) : null;

        queryLemmas.replaceAll((k, v) -> lemmaService.findFrequencyByLemmaAndSite(k, site != null ? String.valueOf(site.getId()) : "%"));

        Integer lemmasAmount = lemmaService.countAllLemmas();
        double frequencyLimit = lemmasAmount * 0.005;

        System.out.println("изначальный запрос " + queryLemmas);

        queryLemmas.values().removeIf(value -> value == 0);

        System.out.println("поиск по существующим леммам " + queryLemmas);

        queryLemmas.keySet().removeIf(searchLemma ->
            (double) (lemmasAmount / lemmaService.findFrequencyByLemmaAndSite(
                searchLemma, site != null ? String.valueOf(site.getId()) : "%")) < frequencyLimit);

        System.out.println("поиск без учета лемм, встречающихся на большом кол-ве страниц" + queryLemmas);

        Map<String, Integer> sortedQueryLemmas =
                queryLemmas.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));


        System.out.println("Леммы, отсортированные по количеству упоминаний" + sortedQueryLemmas);

        if (sortedQueryLemmas.size() == 0){
            response.setResult(false);
            response.setError("По данному запросу не найдено результатов");
            return response;
        }

        String firstLemma = sortedQueryLemmas.keySet().stream().findFirst().get();
        sortedQueryLemmas.remove(firstLemma);

        List<Lemma> firstLemmas = lemmaService.findLemmaByLemmaAndSite(firstLemma, site != null ? String.valueOf(site.getId()) : "%");
        Map<Page, Float> foundPages = new HashMap<>();
        firstLemmas.forEach(lemma -> {
                    for (Page page : findPagesByLemmas(lemma.getId())) {
                        foundPages.put(page, indexRepository.findByLemmaAndPage(lemma, page).getRank());
                    }
                });


        //купить смартфон гарнитур стекло царапина гб

        sortedQueryLemmas.keySet().forEach(queryLemma -> {
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
            response.setResult(false);
            response.setError("По данному запросу не найдено результатов");
            return response;
        }

        float maxRelevancy = foundPages.values().stream().max(Float::compare).get();

        Map<Page, Float> sortedFoundPages =
                foundPages.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        List<ApiData> data = new ArrayList<>();
        for (Page page : sortedFoundPages.keySet()){
            ApiData pageData = new ApiData();

            String url = page.getSite().getSiteUrl().endsWith("/") ? page.getSite().getSiteUrl().replaceFirst(".$","") :
                    page.getSite().getSiteUrl();

            Document doc = Jsoup.parse(page.getContent());

            String title = doc.select("title").text();

            pageData.setSite(url);
            pageData.setSiteName(page.getSite().getSiteName());
            pageData.setUri(page.getPath());

            pageData.setTitle(title);
            pageData.setSnippet("EMPTY_SNIPPET");
            pageData.setRelevance(sortedFoundPages.get(page) / maxRelevancy);
            data.add(pageData);
            if (data.size() == limit)
                break;
        }

        if (foundPages.size() == 0){
            response.setResult(false);
            response.setError("По данному запросу не найдено результатов");
            return response;
        }

        response.setResult(true);
        response.setData(data);
        return response;
    }

    @Override
    public List<Page> findPagesByLemmas(int lemmaId) {
        List<Integer> pagesId = indexRepository.findPagesByLemma(lemmaId);
        List<Page> foundPages = new ArrayList<>();
        pagesId.forEach(id -> pageService.findById(id).ifPresent(foundPages::add));

        return foundPages;
    }


}
// купить синий защитный стекло царапина