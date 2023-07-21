package engine.services;

import com.sun.xml.bind.v2.TODO;
import engine.dto.ApiResponse;
import engine.models.Site;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.swing.text.html.parser.Entity;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class SearchServiceImpl implements SearchService{

    private final LemmaService lemmaService;
    private final SiteService siteService;
//    private final IndexRepository indexRepository;

    @Override
    public ApiResponse search(Map<String, String> body) {
        ApiResponse response = new ApiResponse();

//        Set<String> queryLemmas = new HashSet<>();
//        try {
//            Map<String, Integer> parsedLemmas = lemmaService.parseLemmas(body.get("query"), true);
//            queryLemmas.addAll(parsedLemmas.keySet());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

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

        Site site = body.get("site") != null ? siteService.findBySiteUrl(body.get("site") + "/")
                .orElseGet(() -> siteService.findBySiteUrl(body.get("site") + "/").get()) : null;

        for (String key : queryLemmas.keySet()){
            queryLemmas.put(key, lemmaService.findFrequencyByLemmaAndSite(key, body.get("site") != null ? String.valueOf(site.getId()) : "%"));
        }

        Integer lemmasAmount = lemmaService.countAllLemmas();
        double frequencyLimit = lemmasAmount * 0.005;

        System.out.println("изначальный запрос " + queryLemmas);

        queryLemmas.keySet().removeIf(searchLemma -> lemmaService.findFrequencyByLemmaAndSite(
                searchLemma, body.get("site") != null ? String.valueOf(site.getId()) : "%") == null);

        System.out.println("поиск по существующим леммам " + queryLemmas);

        queryLemmas.keySet().removeIf(searchLemma ->
            (double) (lemmasAmount / lemmaService.findFrequencyByLemmaAndSite(
                searchLemma, body.get("site") != null ? String.valueOf(site.getId()) : "%")) < frequencyLimit);

        System.out.println("поиск без учета лемм, встречающихся на большом кол-ве страниц" + queryLemmas);

        //отсортировать queryLemmas по value

        System.out.println("Леммы, отсортированные по количеству упоминаний" + queryLemmas);



        response.setResult(false);
        response.setError("Неизвестная ошибка.");
        return response;
    }
}
