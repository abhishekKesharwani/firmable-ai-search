package com.firmable.aisearch.controller;

import com.firmable.aisearch.model.SearchResponse;
import com.firmable.aisearch.model.SearchResult;
import com.firmable.aisearch.model.ComprehensiveSearchResponse;
import com.firmable.aisearch.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) List<String> industry,
            @RequestParam(required = false) List<String> size,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Integer foundingYearFrom,
            @RequestParam(required = false) Integer foundingYearTo,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(defaultValue = "relevance") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int sizePerPage,
            @RequestParam(required = false) List<String> facetFields,
            @RequestParam(defaultValue = "10") int facetLimit
    ) throws Exception {
        ResponseEntity<String> solrResponse = searchService.search(query, industry, size, country, city, foundingYearFrom, foundingYearTo, tags, sort, page, sizePerPage, facetFields, facetLimit);
        String solrJson = solrResponse.getBody();
        Map<String, Integer> employeeFacets = searchService.extractEmployeeEstimateFacets(solrJson);
        SearchResponse response = new SearchResponse(solrJson, employeeFacets);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/comprehensive")
    public ResponseEntity<ComprehensiveSearchResponse> comprehensiveSearch(
            @RequestBody ComprehensiveSearchRequest request
    ) throws Exception {
        ComprehensiveSearchResponse response = searchService.comprehensiveSearch(
                request.getQuery(),
                request.getFilters(),
                request.getFacetFields(),
                request.getSortField(),
                request.getSortDirection(),
                request.getPage() != null ? request.getPage() : 0,
                request.getPageSize() != null ? request.getPageSize() : 10,
                request.getReturnFields(),
                request.getSearchType() != null ? request.getSearchType() : "lexical"
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/comprehensive")
    public ResponseEntity<ComprehensiveSearchResponse> comprehensiveSearchGet(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String filters,
            @RequestParam(required = false) List<String> facetFields,
            @RequestParam(required = false) String sortField,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) List<String> returnFields,
            @RequestParam(defaultValue = "lexical") String searchType
    ) throws Exception {
        // Parse filters from JSON string if provided
        Map<String, Object> filtersMap = null;
        if (filters != null && !filters.trim().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                filtersMap = mapper.readValue(filters, Map.class);
            } catch (Exception e) {
                // If parsing fails, ignore filters
                filtersMap = null;
            }
        }

        ComprehensiveSearchResponse response = searchService.comprehensiveSearch(
                query,
                filtersMap,
                facetFields,
                sortField,
                sortDirection,
                page,
                pageSize,
                returnFields,
                searchType
        );
        return ResponseEntity.ok(response);
    }

    public static class ComprehensiveSearchRequest {
        private String query;
        private Map<String, Object> filters;
        private List<String> facetFields;
        private String sortField;
        private String sortDirection;
        private Integer page;
        private Integer pageSize;
        private List<String> returnFields;
        private String searchType;

        public ComprehensiveSearchRequest() {}

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public Map<String, Object> getFilters() { return filters; }
        public void setFilters(Map<String, Object> filters) { this.filters = filters; }

        public List<String> getFacetFields() { return facetFields; }
        public void setFacetFields(List<String> facetFields) { this.facetFields = facetFields; }

        public String getSortField() { return sortField; }
        public void setSortField(String sortField) { this.sortField = sortField; }

        public String getSortDirection() { return sortDirection; }
        public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }

        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }

        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

        public List<String> getReturnFields() { return returnFields; }
        public void setReturnFields(List<String> returnFields) { this.returnFields = returnFields; }
        
        public String getSearchType() { return searchType; }
        public void setSearchType(String searchType) { this.searchType = searchType; }
    }
}
