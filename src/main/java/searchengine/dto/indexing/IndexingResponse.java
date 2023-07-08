package searchengine.dto.indexing;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class IndexingResponse {
    private boolean result;
    private String error;
}
