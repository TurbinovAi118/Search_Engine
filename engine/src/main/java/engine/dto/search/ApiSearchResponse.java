package engine.dto.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiSearchResponse {
    private boolean result;
    private Integer count;
    private List<SearchData> data;
    private String error;
}
