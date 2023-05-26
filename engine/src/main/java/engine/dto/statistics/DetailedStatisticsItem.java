package engine.dto.statistics;

import lombok.Data;
import engine.models.enums.SiteStatus;

import java.time.LocalDateTime;

@Data
public class DetailedStatisticsItem {
    private String url;
    private String name;
    private SiteStatus status;
    private LocalDateTime statusTime;
    private String error;
    private int pages;
    private int lemmas;
}
