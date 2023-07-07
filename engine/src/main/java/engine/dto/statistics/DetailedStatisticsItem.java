package engine.dto.statistics;

import engine.models.enums.SiteStatus;
import lombok.Data;

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