package engine.services;

import engine.models.Page;

import java.util.List;
import java.util.Optional;

public interface PageService {

    void add(Page page);

    void addAll(List<Page> pageList);

    List<Page> list();

    Optional<Page> findById(int id);

    void delete(int id);

    List<Page> findPagesBySiteId(int id);

    Boolean existPageByPath(String path);

    Integer countPagesBySiteId(int id);
}
