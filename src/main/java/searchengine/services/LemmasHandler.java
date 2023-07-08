package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.repository.IndexEntityRepository;
import searchengine.repository.LemmaEntityRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LemmasHandler {

    @Autowired
    private LemmaEntityRepository lemmaEntityRepository;
    @Autowired
    private IndexEntityRepository indexEntityRepository;
    @Autowired
    private MorphologyHandler morphologyHandler;

    public void addLemmas(PageEntity pageEntity) {
        List<IndexEntity> indexSet = new ArrayList<>();
        List<LemmaEntity> newLemmaSet = new ArrayList<>();
        List<LemmaEntity> updatedLemmaSet = new ArrayList<>();
        Document doc = Jsoup.parse(pageEntity.getContent());
        Map<String, Integer> mapLemmas = morphologyHandler.getLemmas(doc.select("body").text());
        synchronized (pageEntity.getSite().getName()) {
            List<LemmaEntity> lemmasInDB = lemmaEntityRepository.findLemmasByNameAndSite(mapLemmas.keySet(), pageEntity.getSite());
            List<String> nameLemmasInDB = new ArrayList<>();
            lemmasInDB.forEach(l -> nameLemmasInDB.add(l.getLemma()));
            mapLemmas.forEach((lemma, ranks) -> {
                LemmaEntity newLemma = new LemmaEntity();
                IndexEntity newIndex = new IndexEntity();
                if (nameLemmasInDB.contains(lemma)) {
                    newLemma = lemmasInDB.get(nameLemmasInDB.indexOf(lemma));
                    newLemma.setFrequency(newLemma.getFrequency() + 1);
                    updatedLemmaSet.add(newLemma);
                } else {
                    newLemma.setLemma(lemma);
                    newLemma.setSite(pageEntity.getSite());
                    newLemma.setFrequency(1);
                    newLemmaSet.add(newLemma);
                }
                newIndex.setPage(pageEntity);
                newIndex.setLemma(newLemma);
                newIndex.setRanks(ranks);
                indexSet.add(newIndex);
            });
            lemmaEntityRepository.saveAllAndFlush(newLemmaSet);
            lemmaEntityRepository.saveAllAndFlush(updatedLemmaSet);
            indexEntityRepository.saveAllAndFlush(indexSet);
        }
    }
}
