package engine.dto;

import lombok.Data;

@Data
public class ApiData {

    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;

}
