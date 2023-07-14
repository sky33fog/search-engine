package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.ConnectionSettings;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repository.IndexEntityRepository;
import searchengine.repository.LemmaEntityRepository;
import searchengine.repository.PageEntityRepository;
import searchengine.repository.SiteEntityRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service
@Getter
@RequiredArgsConstructor
public class IndexingService {

    private static final Logger logger = LogManager.getLogger();
    static final Map<SiteEntity, ForkJoinPool> forkJoinPools = new HashMap<>();
    @Autowired
    private SiteEntityRepository siteEntityRepository;
    @Autowired
    private PageEntityRepository pageEntityRepository;
    @Autowired
    private LemmaEntityRepository lemmaEntityRepository;
    @Autowired
    private IndexEntityRepository indexEntityRepository;
    @Autowired
    private MorphologyHandler morphologyHandler;
    @Autowired
    private LemmasHandler lemmasHandler;
    private final SitesList sites;
    private final ConnectionSettings connectionSettings;
    private final List<Thread> waitingTaskList = new ArrayList<>();

    public boolean startIndexingService() {
        if(forkJoinPools.values().stream().anyMatch(fjp -> fjp.getPoolSize() != 0)) {
            return false;
        }
        IndexingServiceHandler.isInterrupt = false;
        siteEntityRepository.deleteAll();
        forkJoinPools.clear();
        for (Site site : sites.getSites()) {
            SiteEntity siteEntity = new SiteEntity();
            PageEntity mainPage = new PageEntity();
            launchTaskForOneSite(siteEntity, site, mainPage);
        }
        return true;
    }

    public boolean stopIndexingService() {
        if(forkJoinPools.values().stream().allMatch(fjp -> fjp.getPoolSize() == 0)) {
            return false;
        }
        IndexingServiceHandler.isInterrupt = true;
        forkJoinPools.values().forEach(ForkJoinPool::shutdown);
        waitingTaskList.forEach(Thread::interrupt);
        setFailedInStatus();
        return true;
    }

    public boolean indexingPage(String url) {
        SiteEntity siteEntity = new SiteEntity();
        for (Site site : sites.getSites()) {
            if (url.contains(site.getUrl())) {
                setSiteValues(siteEntity, site.getName(), site.getUrl());
                Optional<SiteEntity> optionalSite = siteEntityRepository.findByUrl(site.getUrl());
                if (optionalSite.isEmpty()) {
                    siteEntity.setStatus(IndexingStatus.FAILED);
                    siteEntity.setLastError("Individual pages indexed");
                    siteEntityRepository.save(siteEntity);
                } else {
                    siteEntity.setStatus(optionalSite.get().getStatus());
                    siteEntity.setLastError(optionalSite.get().getLastError());
                    siteEntity.setSiteId(optionalSite.get().getSiteId());
                    siteEntityRepository.save(siteEntity);
                }
            }
        }
        if (siteEntity.getUrl() == null) {
            return false;
        }
        launchParseForOnePage(url, siteEntity);
        return true;
    }

    private Connection.Response connect(String path) throws IOException {
        Connection.Response response;
        response = Jsoup.connect(path)
                .userAgent(connectionSettings.getUserAgent())
                .referrer(connectionSettings.getReferer())
                .ignoreHttpErrors(true)
                .execute();
        return response;
    }

    public static List<String> getPathsFromPage(Document doc, String rootParentUrl) {
        Elements elements = doc.select("a[href]");
        List<String> pathList = new ArrayList<>();
        elements.forEach(el -> pathList.add(el.attr("abs:href")));
        return pathList.stream().filter(link -> link.contains(rootParentUrl)).toList();
    }

    private static void setSiteValues(SiteEntity site, String name, String url) {
        site.setName(name);
        site.setUrl(url);
        site.setStatus(IndexingStatus.INDEXING);
        site.setStatusTime(LocalDateTime.now());
    }

    private static void setPageValues(PageEntity page, SiteEntity site, int codeResponse, String content) {
        page.setSite(site);
        page.setPath("/");
        page.setCodeResponse(codeResponse);
        page.setContent(content);
    }

    private void waitingTaskExecuting(SiteEntity siteEntity, IndexingServiceHandler taskIndexing) {
        Runnable task = () -> {
            taskIndexing.join();
            if (!IndexingServiceHandler.isInterrupt) {
                siteEntity.setStatus(IndexingStatus.INDEXED);
                siteEntity.setStatusTime(LocalDateTime.now());
                siteEntityRepository.save(siteEntity);
                logger.info("Indexing completed for {}", siteEntity.getUrl());
            }
            forkJoinPools.get(siteEntity).shutdown();
        };
        Thread waitingThread = new Thread(task);
        waitingTaskList.add(waitingThread);
        waitingThread.start();
    }

    private void launchParseForOnePage(String url, SiteEntity siteEntity) {
        Runnable task = () -> {
            Connection.Response response;
            Document doc;
            try {
                response = connect(url);
                doc = response.parse();
            } catch (IOException e) {
                logger.warn("Connection error for path {} {}", siteEntity.getUrl(), e);
                return;
            }
            Optional<PageEntity> optionalPage =
                    pageEntityRepository.findByPathAndSite(url.substring(siteEntity.getUrl().length()), siteEntity);
            optionalPage.ifPresent(page -> pageEntityRepository.delete(page));
            PageEntity pageEntity = new PageEntity();
            pageEntity.setSite(siteEntity);
            pageEntity.setPath(url.substring(siteEntity.getUrl().length()));
            pageEntity.setCodeResponse(response.statusCode());
            pageEntity.setContent(doc.toString());
            pageEntityRepository.save(pageEntity);
            lemmasHandler.addLemmas(pageEntity);
        };
        Thread thread = new Thread(task);
        thread.start();
    }

    private void launchTaskForOneSite(SiteEntity siteEntity, Site site, PageEntity mainPage) {
        Runnable task = () -> {
            setSiteValues(siteEntity, site.getName(), site.getUrl());
            siteEntityRepository.save(siteEntity);
            IndexingServiceHandler.timeLastConnect.put(siteEntity, System.currentTimeMillis());
            Connection.Response response;
            Document doc;
            try {
                response = connect(site.getUrl());
                doc = response.parse();
            } catch (IOException e) {
                logger.warn("Connection error for path {} {}", site.getUrl(), e);
                return;
            }
            setPageValues(mainPage, siteEntity, response.statusCode(), doc.toString());
            pageEntityRepository.save(mainPage);
            List<String> paths = getPathsFromPage(doc, siteEntity.getUrl());
            forkJoinPools.put(siteEntity, new ForkJoinPool());
            IndexingServiceHandler indexingServiceHandler = new IndexingServiceHandler();
            indexingServiceHandler.setPathsForIndexing(paths);
            indexingServiceHandler.setSiteParent(siteEntity);
            indexingServiceHandler.setPageEntityRepository(pageEntityRepository);
            indexingServiceHandler.setSiteEntityRepository(siteEntityRepository);
            indexingServiceHandler.setLemmaEntityRepository(lemmaEntityRepository);
            indexingServiceHandler.setIndexEntityRepository(indexEntityRepository);
            indexingServiceHandler.setMorphologyHandler(morphologyHandler);
            indexingServiceHandler.setLemmasHandler(lemmasHandler);
            indexingServiceHandler.setConnectionSettings(connectionSettings);
            indexingServiceHandler.setForkJoinPool(forkJoinPools.get(siteEntity));
            forkJoinPools.get(siteEntity).invoke(indexingServiceHandler);
            waitingTaskExecuting(siteEntity, indexingServiceHandler);
            lemmasHandler.addLemmas(mainPage);
        };
        Thread thread = new Thread(task);
        thread.start();
        logger.info("Started indexing site: {}", site.getUrl());
    }

    private void setFailedInStatus() {
        Runnable task = () -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                logger.error(e);
            }
            List<SiteEntity> sitesList = siteEntityRepository.findIndexingSites();
            for (SiteEntity site : sitesList) {
                site.setStatus(IndexingStatus.FAILED);
                site.setLastError("Indexing interrupted by user");
                site.setStatusTime(LocalDateTime.now());
            }
            siteEntityRepository.saveAll(sitesList);
        };
        Thread thread = new Thread(task);
        thread.start();
    }
}
