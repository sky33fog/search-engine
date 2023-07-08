package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Set;

public interface IndexEntityRepository extends JpaRepository<IndexEntity, Integer> {

    @Query(value = "SELECT i.page.pageId FROM indexes i WHERE lemma.lemma = :lem AND lemma.site IN :sites")
    List<Integer> findPagesIdByLemma(@Param("lem") String lemma,
                                      @Param("sites") List<SiteEntity> sites);

    @Query(value = "SELECT SUM(i.ranks) FROM indexes i WHERE i.page.pageId = :id AND i.lemma IN :lemmas")
    Float findSumRanksByPage(@Param("id") Integer pageId,
                             @Param("lemmas") Set<LemmaEntity> lemmas);
}