package engine.services.implementation;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

@Getter
@Setter
public class SiteConnector {

    public SiteConnector(String url) throws IOException {
        this.doc = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/110.0.0.0 YaBrowser/23.3.4.603 Yowser/2.5 Safari/537.36")
                .referrer("https://www.google.com")
                .get();
    }

    private Document doc;
}
