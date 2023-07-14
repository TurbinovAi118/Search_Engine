package engine.models;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "search_index")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "page_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "FK_PAGE_ID",
                    foreignKeyDefinition = "FOREIGN KEY (page_id) REFERENCES page(id) ON UPDATE CASCADE ON DELETE CASCADE"
            ))
    private Page page;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "lemma_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "FK_LEMMA_ID",
                    foreignKeyDefinition = "FOREIGN KEY (lemma_id) REFERENCES lemma(id) ON UPDATE CASCADE ON DELETE CASCADE"
            ))
    private Lemma lemma;

    @Column(name = "index_rank", nullable = false)
    private float rank;

}
