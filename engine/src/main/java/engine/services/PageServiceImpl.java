package engine.services;


import engine.models.Page;
import engine.repositories.PageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PageServiceImpl implements PageService {

    private final PageRepository pageRepository;
    private final SiteService siteService;

    public PageServiceImpl(PageRepository pageRepository, SiteService siteService) {
        this.pageRepository = pageRepository;
        this.siteService = siteService;
    }

    @Override
    public ResponseEntity<Page> add(Page page) {
        if (siteService.findById(page.getSite().getId()).getStatusCode().equals(HttpStatus.NOT_FOUND)){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        pageRepository.save(page);
        return ResponseEntity.status(HttpStatus.CREATED).body(null);
    }

    @Override
    public List<Page> list() {
        Iterable<Page> pageIterable = pageRepository.findAll();
        List<Page> pageList = new ArrayList<>();
        pageIterable.forEach(pageList::add);
        return pageList;
    }

    @Override
    public ResponseEntity<Page> findById(int id) {
        Optional<Page> pageOptional = pageRepository.findById(id);
        if (pageOptional.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return new ResponseEntity<>(pageOptional.get(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> delete(int id) {
        if (findById(id).getStatusCode().equals(HttpStatus.NOT_FOUND)){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        Page pageToDelete = findById(id).getBody();
        assert pageToDelete != null;
        pageRepository.delete(pageToDelete);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @Override
    public List<String> pathList() {
        Iterable<Page> pageIterable = pageRepository.findAll();
        List<String> pathList = new ArrayList<>();
        pageIterable.forEach(page -> pathList.add(page.getPath()));
        return pathList;
    }

    @Override
    public List<Page> findPagesBySiteId(int id) {
        return pageRepository.getPageBySiteId(id);
    }
}
