package engine.services;

import engine.models.Page;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface PageService {

    ResponseEntity<Page> add(Page page);

    List<Page> list();

    ResponseEntity<Page> findById(int id);

    ResponseEntity<?> delete(int id);

    List<String> pathList();

}