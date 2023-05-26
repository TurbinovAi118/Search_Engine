package engine.services;


import engine.dto.statistics.DetailedStatisticsItem;
import engine.dto.statistics.StatisticsData;
import engine.dto.statistics.StatisticsResponse;
import engine.dto.statistics.TotalStatistics;
import engine.models.Site;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsServiceImpl implements StatisticsService{

    private final SiteService siteService;

    public StatisticsServiceImpl(SiteService siteService) {
        this.siteService = siteService;
    }

    @Override
    public StatisticsResponse getStatistics() {
        List<Site> siteList = siteService.list();

        TotalStatistics total = new TotalStatistics();
        total.setSites(siteList.size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for(Site site : siteList){
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getSiteName());
            item.setUrl(site.getSiteUrl());
            item.setPages(site.getPageList().size());
            //
            item.setLemmas(0);
            //
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
