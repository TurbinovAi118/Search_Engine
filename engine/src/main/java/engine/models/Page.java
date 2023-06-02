package engine.models;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "page", indexes = @Index(name = "fn_index", columnList = "path"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Page implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "code", nullable = false)
    private int responseCode;

    @Column(name = "content", nullable = false, columnDefinition = "mediumtext")
    private String content;

    public Page(Site site, String path, int responseCode, String content) {
        this.site = site;
        this.path = path;
        this.responseCode = responseCode;
        this.content = content;
    }
}