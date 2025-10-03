package com.firmable.aisearch.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class SearchResponse {
    @JsonProperty("solrJson")
    private String solrJson;
    
    @JsonProperty("employeeFacets")
    private Map<String, Integer> employeeFacets;

    public SearchResponse() {}

    public SearchResponse(String solrJson, Map<String, Integer> employeeFacets) {
        this.solrJson = solrJson;
        this.employeeFacets = employeeFacets;
    }

    public String getSolrJson() { return solrJson; }
    public void setSolrJson(String solrJson) { this.solrJson = solrJson; }
    
    public Map<String, Integer> getEmployeeFacets() { return employeeFacets; }
    public void setEmployeeFacets(Map<String, Integer> employeeFacets) { this.employeeFacets = employeeFacets; }
}
