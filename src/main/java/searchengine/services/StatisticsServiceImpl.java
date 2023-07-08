package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.repository.LemmaEntityRepository;
import searchengine.repository.PageEntityRepository;
import searchengine.repository.SiteEntityRepository;

import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    @Autowired
    private PageEntityRepository pageEntityRepository;
    @Autowired
    private SiteEntityRepository siteEntityRepository;
    @Autowired
    private LemmaEntityRepository lemmaEntityRepository;

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(false);
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> confFileSites = sites.getSites();
        for (Site site : confFileSites) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            Optional<SiteEntity> optionalSite = siteEntityRepository.findByUrl(site.getUrl());
            int pageAmount = 0;
            int lemmaAmount = 0;
            if (optionalSite.isPresent()) {
                SiteEntity siteEntity = optionalSite.get();
                pageAmount = pageEntityRepository.getAmountPagesInSite(siteEntity);
                item.setPages(pageAmount);
                lemmaAmount = lemmaEntityRepository.getCountLemmasInSite(siteEntity);
                item.setLemmas(lemmaAmount);
                item.setStatus(siteEntity.getStatus().toString());
                item.setError(siteEntity.getLastError());
                item.setStatusTime(siteEntity.getStatusTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            } else {
                item.setPages(0);
                item.setStatus("FAILED");
                item.setStatusTime(new Date().getTime());
            }
            total.setPages(total.getPages() + pageAmount);
            total.setLemmas(total.getLemmas() + lemmaAmount);
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
