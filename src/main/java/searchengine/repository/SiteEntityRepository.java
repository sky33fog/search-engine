package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;

public interface SiteEntityRepository extends JpaRepository<SiteEntity, Integer> {
    Optional<SiteEntity> findByUrl(String url);

    @Query(value = "SELECT s FROM sites s WHERE s.status = 'INDEXED' AND s.url = :url")
    Optional<SiteEntity> findIndexedSite(@Param("url") String url);

    @Query(value = "SELECT s FROM sites s WHERE s.status = 'INDEXED'")
    List<SiteEntity> findIndexedSites();

    @Query(value = "SELECT s FROM sites s WHERE s.status = 'INDEXING'")
    List<SiteEntity> findIndexingSites();
}