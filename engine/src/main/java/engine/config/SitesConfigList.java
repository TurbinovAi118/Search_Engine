package engine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;


@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class SitesConfigList {
    List<SiteConfig> sites;

    public SiteConfig findSiteByURL(String url) {
        return sites.stream().filter(sites -> sites.getUrl().equals(url)).findFirst().get();
    }
}
