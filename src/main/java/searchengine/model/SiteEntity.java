package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity(name = "sites")
@NoArgsConstructor
@Getter
@Setter
public class SiteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "site_id")
    private int siteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IndexingStatus status;

    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)", unique = true)
    private String url;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String name;

    @OneToMany(mappedBy = "site")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<PageEntity> pages = new HashSet<>();

    @OneToMany(mappedBy = "site")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<LemmaEntity> lemmas = new HashSet<>();
}
