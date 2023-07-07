package engine.models;

import engine.models.enums.SiteStatus;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "site")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Site implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "enum('NOT_INDEXED', 'INDEXING', 'INDEXED', 'FAILED')")
    private SiteStatus status;

    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "url", nullable = false)
    private String siteUrl;

    @Column(name = "name", nullable = false)
    private String siteName;

    public Site(SiteStatus status, LocalDateTime statusTime, String lastError, String siteUrl, String siteName) {
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.siteUrl = siteUrl;
        this.siteName = siteName;
    }
}
