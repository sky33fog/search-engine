package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class MorphologyHandler {

    private static LuceneMorphology luceneMorph;

    static {
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final String[] EXCESS_TYPE_WORDS = new String[]{" ПРЕДЛ", " МЕЖД", " СОЮЗ", " ЧАСТ"};

    public Map<String, Integer> getLemmas(String content) {
        if (IndexingServiceHandler.isInterrupt) {
            return new HashMap<>();
        }
        String[] words = getArrayWithAllRusWords(content);
        List<String> lemmasWords = getListWithRusLemmas(words, true);
        Map<String, Integer> mapLemmas = new HashMap<>();
        for (String lemma : lemmasWords) {
            if (mapLemmas.containsKey(lemma)) {
                mapLemmas.put(lemma, mapLemmas.get(lemma) + 1);
            } else {
                mapLemmas.put(lemma, 1);
            }
        }
        return mapLemmas;
    }

    public List<String> getNormalFormOfWords(String query) {
        String[] words = getArrayWithAllRusWords(query);
        if (words.length == 0) {
            return new ArrayList<>();
        }
        return getListWithRusLemmas(words, false);
    }

    private String[] getArrayWithAllRusWords(String text) {
        return text.toLowerCase().replace("ё", "е").replaceAll("[^а-я\\s]", "").trim().split("\\s+");
    }

    private List<String> getListWithRusLemmas(String[] words, boolean checkInterrupt) {
        List<String> list = new ArrayList<>();
        for (String word : words) {
            if (checkInterrupt && IndexingServiceHandler.isInterrupt) {
                return new ArrayList<>();
            }
            if(word.equals("")) {
                continue;
            }
            List<String> wordMorphInfo = luceneMorph.getMorphInfo(word);
            if (wordMorphInfo.stream().noneMatch(this::checkInvalidWord)) {
                list.addAll(luceneMorph.getNormalForms(word));
            }
        }
        return list;
    }

    private boolean checkInvalidWord(String word) {
        for (String type : EXCESS_TYPE_WORDS) {
            if (word.contains(type) || word.length() > 55) {
                return true;
            }
        }
        return false;
    }
}
