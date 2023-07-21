package engine.services;

import engine.config.SiteConfig;
import engine.config.SitesConfigList;
import engine.dto.statistics.DetailedStatisticsItem;
import engine.dto.statistics.StatisticsData;
import engine.dto.statistics.StatisticsResponse;
import engine.dto.statistics.TotalStatistics;
import engine.models.Lemma;
import engine.models.Site;
import engine.models.enums.SiteStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService{

    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;


    @Override
    public StatisticsResponse getStatistics() {
        List<Site> sitesList = siteService.list();
        TotalStatistics total = new TotalStatistics();
        total.setSites(sitesList.size());
        total.setIndexing(IndexingServiceImpl.isIndexing);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for(Site site : sitesList){

            String url = site.getSiteUrl().endsWith("/") ? site.getSiteUrl().replaceFirst(".$","") :
                    site.getSiteUrl();

            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getSiteName());
            item.setUrl(url);
            item.setPages(pageService.countPagesBySiteId(site));
            item.setLemmas(lemmaService.countLemmasBySiteId(site));
            item.setError(site.getLastError());
            item.setStatusTime(site.getStatusTime());
            item.setStatus(site.getStatus());
            total.setPages(total.getPages() + item.getPages());
            total.setLemmas(total.getLemmas() + item.getLemmas());
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
