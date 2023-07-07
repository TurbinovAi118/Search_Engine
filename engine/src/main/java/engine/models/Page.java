package engine.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "page", indexes = @Index(name = "path_index", columnList = "path", unique = true))
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
    @JoinColumn(name = "site_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "FK_SITE_ID",
                    foreignKeyDefinition = "FOREIGN KEY (site_id) REFERENCES site(id) ON UPDATE CASCADE ON DELETE CASCADE"
            ))
    private Site site;

    @Column(name = "path", nullable = false, columnDefinition = "mediumtext")
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

    @Override
    public String toString() {
        return "Page{" +
                "id=" + id +
                ", site=" + site +
                ", path='" + path + '\'' +
                ", responseCode=" + responseCode +
                '}';
    }
}
