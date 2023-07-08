package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.HashSet;
import java.util.Set;

@Entity(name = "lemmas")
@NoArgsConstructor
@Getter
@Setter
@Table(indexes = @Index(name = "lemma_index", columnList = "lemma, site_id", unique = true))
public class LemmaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "lemma_id")
    private int lemmaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private SiteEntity site;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    @OneToMany(mappedBy = "lemma")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<IndexEntity> indexes = new HashSet<>();
}
