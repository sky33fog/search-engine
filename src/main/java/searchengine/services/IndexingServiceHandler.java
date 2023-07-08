package searchengine.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.config.ConnectionSettings;
import searchengine.model.PageEntity;
import searchengine.repository.IndexEntityRepository;
import searchengine.repository.LemmaEntityRepository;
import searchengine.repository.PageEntityRepository;
import searchengine.model.SiteEntity;
import searchengine.repository.SiteEntityRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class IndexingServiceHandler extends RecursiveAction {

    private static final Logger logger = LogManager.getLogger();
    public static boolean isInterrupt;
    public static Map<SiteEntity, Long> timeLastConnect = new HashMap<>();
    private final ConnectionSettings connectionSettings;
    private final List<IndexingServiceHandler> pageTasks = new ArrayList<>();
    private final PageEntityRepository pageEntityRepository;
    private final SiteEntityRepository siteEntityRepository;
    private final LemmaEntityRepository lemmaEntityRepository;
    private final IndexEntityRepository indexEntityRepository;
    private final MorphologyHandler morphologyHandler;
    private final LemmasHandler lemmasHandler;
    private final ForkJoinPool forkJoinPool;
    private final List<String> pathsForIndexing;
    private final SiteEntity siteParent;

    public IndexingServiceHandler(List<String> pathsForIndexing,
                                  SiteEntity siteParent,
                                  PageEntityRepository pageRepository,
                                  SiteEntityRepository siteRepository,
                                  LemmaEntityRepository lemmaEntityRepository,
                                  IndexEntityRepository indexEntityRepository,
                                  MorphologyHandler morphologyHandler,
                                  LemmasHandler lemmasHandler,
                                  ConnectionSettings settings,
                                  ForkJoinPool forkJoinPool) {
        this.pathsForIndexing = pathsForIndexing;
        this.siteParent = siteParent;
        this.pageEntityRepository = pageRepository;
        this.siteEntityRepository = siteRepository;
        this.lemmaEntityRepository = lemmaEntityRepository;
        this.indexEntityRepository = indexEntityRepository;
        this.morphologyHandler = morphologyHandler;
        this.lemmasHandler = lemmasHandler;
        this.connectionSettings = settings;
        this.forkJoinPool = forkJoinPool;
    }

    @Override
    protected void compute() {
        for (String path : pathsForIndexing) {
            if (isInterrupt) {
                return;
            }
            if (checkInvalidPath(path)) {
                continue;
            }
            PageEntity newPage = createNewPageEntity(siteParent, path);
            if (newPage == null) {
                continue;
            }
            lemmasHandler.addLemmas(newPage);
        }
        updateSiteStatus(siteParent);
        try {
            pageTasks.forEach(ForkJoinTask::join);
        } catch (RuntimeException e) {
            logger.error(e);
        }
    }

    private void updateSiteStatus(SiteEntity siteParent) {
        siteParent.setStatusTime(LocalDateTime.now());
        siteEntityRepository.save(siteParent);
    }

    private PageEntity createNewPageEntity(SiteEntity site, String path) {
        String formattedPath = path.substring(site.getUrl().length());
        PageEntity newPage = new PageEntity();
        Document doc;
        synchronized (siteParent) {
            Optional<PageEntity> optionalPage = pageEntityRepository.findByPathAndSite(formattedPath, site);
            if (optionalPage.isPresent()) {
                return null;
            }
            newPage.setSite(site);
            newPage.setPath(formattedPath);
            Connection.Response response;
            long l = System.currentTimeMillis() - timeLastConnect.get(siteParent);
            if (l < 100) {
                try {
                    Thread.sleep(200 - l);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            try {
                response = connect(path);
                newPage.setCodeResponse(response.statusCode());
                doc = response.parse();
                newPage.setContent(doc.toString());
            } catch (Exception e) {
                logger.warn("Connection error for path {} {}", path, e);
                return null;
            }
            pageEntityRepository.saveAndFlush(newPage);
        }
        List<String> pathsFromPage = IndexingService.getPathsFromPage(doc, site.getUrl());
        IndexingServiceHandler task = new IndexingServiceHandler(
                pathsFromPage,
                site,
                pageEntityRepository,
                siteEntityRepository,
                lemmaEntityRepository,
                indexEntityRepository,
                morphologyHandler,
                lemmasHandler,
                connectionSettings,
                forkJoinPool);
        pageTasks.add(task);
        forkJoinPool.invoke(task);
        return newPage;
    }

    private Connection.Response connect(String path) throws IOException {
        Connection.Response response;
        timeLastConnect.put(siteParent, System.currentTimeMillis());
        response = Jsoup.connect(path)
                .userAgent(connectionSettings.getUserAgent())
                .referrer(connectionSettings.getReferer())
                .ignoreHttpErrors(true)
                .timeout(10000)
                .execute();
        return response;
    }

    private static boolean checkInvalidPath(String path) {
        return path.contains(".pdf")
                || path.contains(".PDF")
                || path.contains(".jpg")
                || path.contains(".jpeg")
                || path.contains(".png")
                || path.contains(".gif")
                || path.contains(".tif")
                || path.contains(".doc")
                || path.contains(".DOC")
                || path.contains(".docx")
                || path.contains(".rtf")
                || path.contains(".rar")
                || path.contains(".zip")
                || path.contains(".xls")
                || path.contains(".xlsx")
                || path.contains(".JPG")
                || path.contains(".webp")
                || path.contains("#");
    }
}
