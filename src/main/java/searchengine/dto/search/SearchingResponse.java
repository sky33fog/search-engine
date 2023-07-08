package searchengine.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class SearchingResponse {
     private boolean result;
     Integer count;
     List<SearchData> data;
     String error;
}
