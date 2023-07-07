package engine.services;

import engine.models.Page;;
import engine.repositories.PageRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PageServiceImpl  implements PageService{

    private final PageRepository pageRepository;
    private final SiteService siteService;

    public PageServiceImpl(PageRepository pageRepository, SiteService siteService) {
        this.pageRepository = pageRepository;
        this.siteService = siteService;
    }

    @Override
    public void add(Page page) {
        if (siteService.findById(page.getSite().getId()).isPresent()){
            pageRepository.save(page);
        }
    }

    @Override
    public void addAll(List<Page> pageList){
        pageRepository.saveAll(pageList);
    }

    @Override
    public List<Page> list() {
        List<Page> pageList = new ArrayList<>();
        pageRepository.findAll().forEach(pageList::add);
        return pageList;
    }

    @Override
    public Optional<Page> findById(int id) {
        return pageRepository.findById(id);
    }

    @Override
    public void delete(int id) {
        findById(id).ifPresent(pageRepository::delete);
    }

    @Override
    public List<Page> findPagesBySiteId(int id) {
        return pageRepository.getPageBySiteId(id);

    }

    @Override
    public Boolean existPageByPath(String path) {
        return pageRepository.existsByPath(path);
    }

    @Override
    public Integer countPagesBySiteId(int id) {
        return pageRepository.countPagesBySiteId(id);
    }

}
