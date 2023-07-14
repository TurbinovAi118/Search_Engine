package engine.models;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "lemma")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "site_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "FK_LEMMA_SITE_ID",
                    foreignKeyDefinition = "FOREIGN KEY (site_id) REFERENCES site(id) ON UPDATE CASCADE ON DELETE CASCADE"
            ))
    private Site site;

    @Column(name = "lemma")
    private String lemma;

    @Column(name = "frequency")
    private int frequency;

}
