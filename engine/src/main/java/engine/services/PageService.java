package engine.services;

import engine.dto.ApiResponse;
import engine.models.Page;
import engine.models.Site;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

public interface PageService {

    void add(Page page);

    List<Page> addAll(List<Page> pageList);

    List<Page> list();

    Optional<Page> findById(int id);

    void delete(int id);

    List<Page> findPagesBySiteId(int id);

    Boolean existPageByPath(String path);

    Integer countPagesBySiteId(Site site);

    ApiResponse addSinglePage(String page);

    void patch(Page page);

    Optional<Page> findPageById(int id);

    Integer countAllPages();

}
