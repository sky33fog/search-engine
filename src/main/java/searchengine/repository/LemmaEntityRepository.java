package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.Collection;
import java.util.List;

public interface LemmaEntityRepository extends JpaRepository<LemmaEntity, Integer> {

    @Query(value = "SELECT l FROM lemmas l WHERE l.lemma IN :lem AND l.site = :site")
    List<LemmaEntity> findLemmasByNameAndSite(@Param("lem") Collection<String> list,
                                              @Param("site") SiteEntity site);

    @Query(value = "SELECT COUNT(*) FROM lemmas WHERE site = :site")
    Integer getCountLemmasInSite(SiteEntity site);

    @Query(value = "SELECT l FROM lemmas l WHERE l.lemma = :lemma AND l.site IN :sites")
    List<LemmaEntity> findLemmasByNameAndSites(@Param("lemma") String lemma,
                                               @Param(("sites")) Collection<SiteEntity> sites);

    @Query(value = "SELECT l FROM lemmas l WHERE site = :site ORDER BY frequency DESC LIMIT 10")
    List<LemmaEntity> find10MostOftenLemmasInSite(SiteEntity site);
}
