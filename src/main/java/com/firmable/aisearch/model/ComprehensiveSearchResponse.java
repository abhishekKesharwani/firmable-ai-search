package com.firmable.aisearch.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class ComprehensiveSearchResponse {
    @JsonProperty("documents")
    private List<Map<String, Object>> documents;
    
    @JsonProperty("totalResults")
    private long totalResults;
    
    @JsonProperty("facets")
    private Map<String, Map<String, Integer>> facets;
    
    @JsonProperty("pagination")
    private PaginationInfo pagination;
    
    @JsonProperty("queryInfo")
    private QueryInfo queryInfo;
    
    @JsonProperty("searchMetadata")
    private SearchMetadata searchMetadata;

    public ComprehensiveSearchResponse() {}

    public ComprehensiveSearchResponse(List<Map<String, Object>> documents, long totalResults, 
                                     Map<String, Map<String, Integer>> facets, 
                                     PaginationInfo pagination, QueryInfo queryInfo, 
                                     SearchMetadata searchMetadata) {
        this.documents = documents;
        this.totalResults = totalResults;
        this.facets = facets;
        this.pagination = pagination;
        this.queryInfo = queryInfo;
        this.searchMetadata = searchMetadata;
    }

    public List<Map<String, Object>> getDocuments() { return documents; }
    public void setDocuments(List<Map<String, Object>> documents) { this.documents = documents; }
    
    public long getTotalResults() { return totalResults; }
    public void setTotalResults(long totalResults) { this.totalResults = totalResults; }
    
    public Map<String, Map<String, Integer>> getFacets() { return facets; }
    public void setFacets(Map<String, Map<String, Integer>> facets) { this.facets = facets; }
    
    public PaginationInfo getPagination() { return pagination; }
    public void setPagination(PaginationInfo pagination) { this.pagination = pagination; }
    
    public QueryInfo getQueryInfo() { return queryInfo; }
    public void setQueryInfo(QueryInfo queryInfo) { this.queryInfo = queryInfo; }
    
    public SearchMetadata getSearchMetadata() { return searchMetadata; }
    public void setSearchMetadata(SearchMetadata searchMetadata) { this.searchMetadata = searchMetadata; }

    public static class PaginationInfo {
        @JsonProperty("currentPage")
        private int currentPage;
        
        @JsonProperty("pageSize")
        private int pageSize;
        
        @JsonProperty("totalPages")
        private int totalPages;
        
        @JsonProperty("hasNext")
        private boolean hasNext;
        
        @JsonProperty("hasPrevious")
        private boolean hasPrevious;

        public PaginationInfo() {}

        public PaginationInfo(int currentPage, int pageSize, long totalResults) {
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.totalPages = (int) Math.ceil((double) totalResults / pageSize);
            this.hasNext = currentPage < totalPages - 1;
            this.hasPrevious = currentPage > 0;
        }

        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
        
        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }
        
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        
        public boolean isHasNext() { return hasNext; }
        public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
        
        public boolean isHasPrevious() { return hasPrevious; }
        public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
    }

    public static class QueryInfo {
        @JsonProperty("query")
        private String query;
        
        @JsonProperty("filters")
        private Map<String, Object> filters;
        
        @JsonProperty("sort")
        private String sort;
        
        @JsonProperty("executionTime")
        private long executionTime;

        public QueryInfo() {}

        public QueryInfo(String query, Map<String, Object> filters, String sort, long executionTime) {
            this.query = query;
            this.filters = filters;
            this.sort = sort;
            this.executionTime = executionTime;
        }

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        
        public Map<String, Object> getFilters() { return filters; }
        public void setFilters(Map<String, Object> filters) { this.filters = filters; }
        
        public String getSort() { return sort; }
        public void setSort(String sort) { this.sort = sort; }
        
        public long getExecutionTime() { return executionTime; }
        public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
    }

    public static class SearchMetadata {
        @JsonProperty("searchType")
        private String searchType;
        
        @JsonProperty("lexicalResultsCount")
        private int lexicalResultsCount;
        
        @JsonProperty("semanticResultsCount")
        private int semanticResultsCount;
        
        @JsonProperty("totalResultsCount")
        private int totalResultsCount;

        public SearchMetadata() {}

        public SearchMetadata(String searchType, int lexicalResultsCount, int semanticResultsCount) {
            this.searchType = searchType;
            this.lexicalResultsCount = lexicalResultsCount;
            this.semanticResultsCount = semanticResultsCount;
            this.totalResultsCount = lexicalResultsCount + semanticResultsCount;
        }

        public String getSearchType() { return searchType; }
        public void setSearchType(String searchType) { this.searchType = searchType; }
        
        public int getLexicalResultsCount() { return lexicalResultsCount; }
        public void setLexicalResultsCount(int lexicalResultsCount) { this.lexicalResultsCount = lexicalResultsCount; }
        
        public int getSemanticResultsCount() { return semanticResultsCount; }
        public void setSemanticResultsCount(int semanticResultsCount) { this.semanticResultsCount = semanticResultsCount; }
        
        public int getTotalResultsCount() { return totalResultsCount; }
        public void setTotalResultsCount(int totalResultsCount) { this.totalResultsCount = totalResultsCount; }
    }
}