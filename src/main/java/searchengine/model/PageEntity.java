package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.HashSet;
import java.util.Set;

@Entity(name = "pages")
@NoArgsConstructor
@Getter
@Setter
@Table(indexes = @Index(name = "path_index", columnList = "path, site_id", unique = true))
public class PageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "page_id")
    private int pageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private SiteEntity site;

    @Column(name = "path", nullable = false, columnDefinition = "VARCHAR(255)")
    private String path;

    @Column(name = "code_response", nullable = false)
    private int codeResponse;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @OneToMany(mappedBy = "page")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<IndexEntity> indexes = new HashSet<>();
}
