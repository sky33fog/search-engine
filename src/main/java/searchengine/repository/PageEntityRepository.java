package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.Optional;

public interface PageEntityRepository extends JpaRepository<PageEntity, Integer> {

    Optional<PageEntity> findByPathAndSite(String path, SiteEntity site);

    @Query(value = "SELECT COUNT(*) FROM pages WHERE site = :site")
    Integer getAmountPagesInSite(SiteEntity site);

}

