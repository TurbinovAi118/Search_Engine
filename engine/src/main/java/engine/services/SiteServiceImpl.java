package engine.services;



import com.google.protobuf.Api;
import engine.dto.ApiResponse;
import engine.models.Site;
import engine.models.enums.SiteStatus;
import engine.repositories.SiteRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SiteServiceImpl implements SiteService {

    private final SiteRepository siteRepository;
    Document doc;

    public SiteServiceImpl(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @Override
    public ApiResponse add(String url) {
        ApiResponse response = new ApiResponse();
        try {
            doc = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/110.0.0.0 YaBrowser/23.3.4.603 Yowser/2.5 Safari/537.36").get();
        } catch (Exception e) {
            e.printStackTrace();
            response.setResult(false);
            response.setError("Введен не верный адресс.");
            return response;
        }
        Site site = new Site();
        String name = doc.select("head").select("title").text();
        site.setSiteUrl(url);
        site.setSiteName(name);
        site.setStatus(SiteStatus.NOT_INDEXED);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        response.setResult(true);
        return response;
    }

    @Override
    public ResponseEntity<Site> findById(int id) {
        Optional<Site> siteOptional = siteRepository.findById(id);
        if (siteOptional.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return new ResponseEntity<>(siteOptional.get(), HttpStatus.OK);
    }

    @Override
    public List<Site> list() {
        Iterable<Site> siteIterable = siteRepository.findAll();
        List<Site> siteList = new ArrayList<>();
        siteIterable.forEach(siteList::add);
        return siteList;
    }

    @Override
    public ResponseEntity<?> delete(int id) {
        if (findById(id).getStatusCode().equals(HttpStatus.NOT_FOUND)){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        Site siteToDelete = findById(id).getBody();
        assert siteToDelete != null;
        siteRepository.delete(siteToDelete);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @Override
    public ResponseEntity<Site> patch(Site site) {
        if (findById(site.getId()).getStatusCode().equals(HttpStatus.NOT_FOUND)){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        Site siteToPatch = findById(site.getId()).getBody();
        assert siteToPatch != null;

        if (site.getLastError() != null && !siteToPatch.getLastError().equals(site.getLastError())){
            siteToPatch.setLastError(site.getLastError());
        }
        if (!siteToPatch.getPageList().equals(site.getPageList())){
            siteToPatch.setPageList(site.getPageList());
        }
        if (!siteToPatch.getStatus().equals(site.getStatus())){
            siteToPatch.setStatus(site.getStatus());
        }
        siteToPatch.setStatusTime(LocalDateTime.now());

        siteRepository.save(siteToPatch);
        return new ResponseEntity<>(siteToPatch, HttpStatus.OK);
    }
}
