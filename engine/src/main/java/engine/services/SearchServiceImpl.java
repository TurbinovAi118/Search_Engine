package engine.services;

import engine.dto.ApiResponse;
import engine.models.Lemma;
import engine.models.Page;
import engine.models.Site;
import engine.repositories.IndexRepository;
import lombok.AllArgsConstructor;
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

        Map<String, Integer> queryLemmas = new HashMap<>();
        try {
            queryLemmas = lemmaService.parseLemmas(body.get("query"), true);
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

        queryLemmas.keySet().removeIf(searchLemma -> lemmaService.findFrequencyByLemmaAndSite(
                searchLemma, site != null ? String.valueOf(site.getId()) : "%") == null);

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

        String firstLemma = sortedQueryLemmas.keySet().stream().findFirst().get();
        sortedQueryLemmas.remove(firstLemma);

        int firstLemmaId = lemmaService.findLemmaByLemmaAndSite(firstLemma, site != null ? String.valueOf(site.getId()) : "%").getId();
        Map<Page, Float> foundPages = new HashMap<>();
        for (Page page : findPagesByLemmas(firstLemmaId)){
            foundPages.put(page, 0F);
        }

        //купить смартфон гарнитур стекло царапина гб

        System.out.println(foundPages);

        sortedQueryLemmas.keySet().forEach(queryLemma -> {
            Lemma lemma = lemmaService.findLemmaByLemmaAndSite(queryLemma, site != null ? String.valueOf(site.getId()) : "%");
            foundPages.keySet().forEach(page -> {
                if (indexRepository.existsByPageAndLemma(page, lemma)) {
                    foundPages.put(page, foundPages.get(page) + indexRepository.findByLemmaAndPage(lemma, page).getRank());
                }
            });
            foundPages.keySet().removeIf(page -> !indexRepository.existsByPageAndLemma(page, lemma));
        });

        System.out.println(foundPages);

        if (foundPages.size() == 0){
            response.setResult(false);
            response.setError("По данному запросу не найдено результатов");
            return response;
        }



        response.setResult(false);
        response.setError("Неизвестная ошибка.");
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