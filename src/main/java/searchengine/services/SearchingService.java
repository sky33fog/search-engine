package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchingResponse;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexEntityRepository;
import searchengine.repository.LemmaEntityRepository;
import searchengine.repository.PageEntityRepository;
import searchengine.repository.SiteEntityRepository;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SearchingService {

    static final int RANGE_LENGTH = 330;
    static final String START = "start";
    static final String END = "end";
    @Autowired
    private MorphologyHandler morphologyHandler;
    @Autowired
    private SiteEntityRepository siteEntityRepository;
    @Autowired
    private LemmaEntityRepository lemmaEntityRepository;
    @Autowired
    private IndexEntityRepository indexEntityRepository;
    @Autowired
    private PageEntityRepository pageEntityRepository;

    public SearchingResponse getSearchingResult(String query, String site, Integer offset, Integer limit) {
        SearchingResponse response = new SearchingResponse();
        List<SiteEntity> indexedSitesList = new ArrayList<>();
        if (site == null) {
            indexedSitesList.addAll(siteEntityRepository.findIndexedSites());
            if (indexedSitesList.isEmpty()) {
                response.setResult(false);
                response.setError("Нет проиндексированных сайтов.");
                return response;
            }
        } else {
            SiteEntity siteEntity = siteIndexedCheck(site);
            if (siteEntity == null) {
                response.setResult(false);
                response.setError("Выбранный сайт не проиндексирован.");
                return response;
            } else {
                indexedSitesList.add(siteEntity);
            }
        }
        List<String> normalWordsFromQuery = morphologyHandler.getNormalFormOfWords(query);
        Set<LemmaEntity> lemmasFromQuery = getSortedLemmasList(normalWordsFromQuery, indexedSitesList);
        indexedSitesList.retainAll(getSitesListFromLemmasList(lemmasFromQuery));
        Map<Float, Integer> pagesWithRelevance = getPagesRelevanceMap(lemmasFromQuery, indexedSitesList);
        List<SearchData> searchDataSet = new ArrayList<>();
        pagesWithRelevance.entrySet().stream().skip(offset).limit(limit).forEach((entrySet) -> {
            SearchData searchData = new SearchData();
            Optional<PageEntity> optionalPage = pageEntityRepository.findById(entrySet.getValue());
            if(optionalPage.isPresent()) {
                PageEntity page = optionalPage.get();
                searchData.setSite(page.getSite().getUrl());
                searchData.setSiteName(page.getSite().getName());
                searchData.setUri(page.getPath());
                searchData.setTitle(getTitlePage(page));
                searchData.setSnippet(getSnippet(page, normalWordsFromQuery));
                searchData.setRelevance(entrySet.getKey().toString());
                searchDataSet.add(searchData);
            }
        });
        response.setResult(true);
        response.setCount(pagesWithRelevance.size());
        response.setData(searchDataSet);
        return response;
    }

    private SiteEntity siteIndexedCheck(String url) {
        Optional<SiteEntity> optionalSite = siteEntityRepository.findIndexedSite(url);
        return optionalSite.orElse(null);
    }

    private Set<LemmaEntity> getSortedLemmasList(List<String> normalWords, List<SiteEntity> indexedSitesList) {
        List<LemmaEntity> finalLemmasList = new ArrayList<>();
        for (String word : normalWords) {
            List<LemmaEntity> lemmasList = lemmaEntityRepository.findLemmasByNameAndSites(word, indexedSitesList);
            if (lemmasList.isEmpty()) {
                return new HashSet<>();
            }
            if (!checkExistsLemmasForAllSites(lemmasList, indexedSitesList)) {
                indexedSitesList.retainAll(getSitesListFromLemmasList(lemmasList));
            }
            finalLemmasList.addAll(lemmasList);
        }
        finalLemmasList.removeIf(lemma -> !indexedSitesList.contains(lemma.getSite()));
        Set<LemmaEntity> sortedLemmasSet = new TreeSet<>((o1, o2) -> {
            if (o1.getFrequency() >= o2.getFrequency()) {
                return 1;
            } else {
                return -1;
            }
        });
        sortedLemmasSet.addAll(finalLemmasList);
        Map<SiteEntity, Integer> averageFrequencyMostOftenLemmas = getAverageFrequencyMostOftenLemmas(indexedSitesList);
        sortedLemmasSet.removeIf(lemma -> lemma.getFrequency() > (averageFrequencyMostOftenLemmas.get(lemma.getSite()) * 0.95));
        return sortedLemmasSet;
    }

    private List<SiteEntity> getSitesListFromLemmasList(Collection<LemmaEntity> lemmasList) {
        List<SiteEntity> savedSites = new ArrayList<>();
        for (LemmaEntity l : lemmasList) {
            savedSites.add(l.getSite());
        }
        return savedSites;
    }

    private boolean checkExistsLemmasForAllSites(List<LemmaEntity> lemmasList, List<SiteEntity> indexedSitesList) {
        Set<SiteEntity> sitesForLemmas = new HashSet<>();
        for (LemmaEntity lemma : lemmasList) {
            sitesForLemmas.add(lemma.getSite());
        }
        return sitesForLemmas.size() == indexedSitesList.size();
    }

    private Map<SiteEntity, Integer> getAverageFrequencyMostOftenLemmas(List<SiteEntity> indexedSitesList) {
        Map<SiteEntity, Integer> map = new HashMap<>();
        for (SiteEntity site : indexedSitesList) {
            int summ = 0;
            List<LemmaEntity> lemmaEntityList = lemmaEntityRepository.find10MostOftenLemmasInSite(site);
            for (LemmaEntity lemma : lemmaEntityList) {
                summ = (summ + lemma.getFrequency());
            }
            int averageValue = summ / lemmaEntityList.size();
            map.put(site, averageValue);
        }
        return map;
    }

    private Map<Float, Integer> getPagesRelevanceMap(Set<LemmaEntity> lemmasSet, List<SiteEntity> indexedSites) {
        List<Integer> finalPagesIdList = null;
        for (LemmaEntity lem : lemmasSet) {
            List<Integer> pagesIdList = indexEntityRepository.findPagesIdByLemma(lem.getLemma(), indexedSites);
            if (finalPagesIdList == null) {
                finalPagesIdList = new ArrayList<>(pagesIdList);
            } else {
                finalPagesIdList.retainAll(pagesIdList);
            }
        }
        if (finalPagesIdList == null || finalPagesIdList.isEmpty()) {
            return new TreeMap<>();
        } else {
            return getRelevance(finalPagesIdList, lemmasSet);
        }
    }

    private Map<Float, Integer> getRelevance(List<Integer> finalPagesIdList, Set<LemmaEntity> lemmas) {
        Map<Integer, Float> absRelevanceMap = new HashMap<>();
        Map<Float, Integer> relRelevanceMap = new TreeMap<>((o1, o2) -> {
            if (o1 > o2) {
                return -1;
            }
            return 1;
        });
        for (Integer pageId : finalPagesIdList) {
            Float sum = indexEntityRepository.findSumRanksByPage(pageId, lemmas);
            absRelevanceMap.put(pageId, sum);
        }
        Float maxRelevance = Collections.max(absRelevanceMap.values());
        absRelevanceMap.forEach((p, i) -> relRelevanceMap.put((i / maxRelevance), p));
        return relRelevanceMap;
    }

    private String getSnippet(PageEntity page, List<String> normalWordsFromQuery) {
        Document content = Jsoup.parse(page.getContent());
        String body = content.select("body").text();
        String rusText = body.toLowerCase()
                .replace("ё", "е")
                .replaceAll("[^а-я\\s]", "")
                .trim();
        String[] rusWords = rusText.split("\\s+");
        Set<String> wordsForBold = new HashSet<>();
        for (String word : rusWords) {
            List<String> lemmas = morphologyHandler.getNormalFormOfWords(word);
            for(String lemmaFromQuery : normalWordsFromQuery) {
                if(lemmas.contains(lemmaFromQuery)) {
                    wordsForBold.add(word);
                }
            }
        }
        Set<Integer> positionsWordsFromQuery = new TreeSet<>();
        for(String word : wordsForBold) {
            positionsWordsFromQuery.addAll(getAllPositionsWord(body.toLowerCase(), word));
            body = body.replaceAll(word, "<b>" + word + "</b>");
            String upperWord = word.toUpperCase().charAt(0) + word.substring(1);
            body = body.replaceAll(upperWord, "<b>" + upperWord + "</b>");
        }
        Map<String, Integer> rangeBorder = calculateOutputRange(positionsWordsFromQuery, body.length());
        body = body.substring(rangeBorder.get(START), rangeBorder.get(END));
        return "..." + body + "...";
    }

    private Set<Integer> getAllPositionsWord(String text, String word) {
        Set<Integer> positions = new TreeSet<>();
        Pattern pattern = Pattern.compile(word);
        Matcher matcher = pattern.matcher(text);
        while(matcher.find()) {
            positions.add(matcher.start());
        }
        return positions;
    }

    private String getTitlePage(PageEntity page) {
        Document doc = Jsoup.parse(page.getContent());
        return doc.select("title").text();
    }

    private Map<String, Integer> calculateOutputRange(Set<Integer> positionWordsFromQuery, int textLength) {
        Map<String, Integer> map = new HashMap<>();
        int minSplit = RANGE_LENGTH;
        int minSplitPosition = 0;
        int count = 0;
        List<Integer> positionsList = new ArrayList<>(positionWordsFromQuery);
        for (int i = 1; i < positionsList.size(); i++) {
            if ((positionsList.get(i) - positionsList.get(i - 1)) < minSplit) {
                minSplit = (positionsList.get(i) - positionsList.get(i - 1));
                minSplitPosition = positionsList.get(i);
                count++;
            }
        }
        if (count == 0) {
            minSplitPosition = positionsList.get(0);
        }
        int startPosition = Math.max((minSplitPosition - 10), 0);
        if (textLength < RANGE_LENGTH) {
            map.put(START, 0);
            map.put(END, textLength);
            return map;
        }
        if ((startPosition + RANGE_LENGTH) > textLength) {
            map.put(START, textLength - RANGE_LENGTH);
            map.put(END, textLength);
            return map;
        }
        map.put(START, startPosition);
        map.put(END, startPosition + RANGE_LENGTH);
        return map;
    }
}