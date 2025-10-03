package com.firmable.aisearch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import java.util.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class AutosuggestService {
    
    private static final String SOLR_URL = "http://localhost:8983/solr/company";
    
    @Autowired
    private QueryUnderstandingService queryUnderstandingService;
    
    public AutosuggestResponse getAutosuggestions(String query, int maxSuggestions) {
        AutosuggestResponse response = new AutosuggestResponse();
        
        if (query == null || query.trim().length() < 2) {
            return response; // Return empty response for very short queries
        }
        
        String normalizedQuery = query.toLowerCase().trim();
        
        try {
            // Get company name suggestions
            List<Suggestion> companyNames = getCompanyNameSuggestions(normalizedQuery, maxSuggestions / 3);
            
            // Get industry suggestions 
            List<Suggestion> industries = getIndustrySuggestions(normalizedQuery, maxSuggestions / 3);
            
            // Get location suggestions
            List<Suggestion> locations = getLocationSuggestions(normalizedQuery, maxSuggestions / 3);
            
            // Get intelligent query suggestions based on natural language patterns
            List<Suggestion> queryTemplates = getQueryTemplateSuggestions(normalizedQuery, maxSuggestions / 4);
            
            response.setCompanyNames(companyNames);
            response.setIndustries(industries);
            response.setLocations(locations);
            response.setQueryTemplates(queryTemplates);
            
            // Create combined suggestions for overall autosuggest
            List<Suggestion> allSuggestions = new ArrayList<>();
            allSuggestions.addAll(companyNames);
            allSuggestions.addAll(industries);
            allSuggestions.addAll(locations);
            allSuggestions.addAll(queryTemplates);
            
            // Sort by relevance and limit to maxSuggestions
            allSuggestions.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
            if (allSuggestions.size() > maxSuggestions) {
                allSuggestions = allSuggestions.subList(0, maxSuggestions);
            }
            
            response.setAllSuggestions(allSuggestions);
            
        } catch (Exception e) {
            System.err.println("Error generating autosuggestions: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }
    
    private List<Suggestion> getCompanyNameSuggestions(String query, int limit) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        
        // Use Solr suggest component for company names
        String solrQuery = SOLR_URL + "/suggest?suggest=true&suggest.build=true&suggest.dictionary=companySuggester&suggest.q=" + 
                          URLEncoder.encode(query, StandardCharsets.UTF_8) + "&suggest.count=" + limit;
        
        System.out.println("Company suggest query: " + solrQuery);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                solrQuery, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            
            List<Suggestion> suggestions = new ArrayList<>();
            JsonNode suggestNode = root.path("suggest").path("companySuggester").path(query);
            
            if (suggestNode.isArray() && suggestNode.size() > 0) {
                JsonNode suggestionsArray = suggestNode.get(0).path("suggestions");
                for (JsonNode suggestionNode : suggestionsArray) {
                    String term = suggestionNode.path("term").asText();
                    float weight = (float) suggestionNode.path("weight").asDouble();
                    
                    suggestions.add(new Suggestion(term, "company", weight, 
                        "Search for companies named: " + term));
                }
            } else {
                // Fallback to prefix matching if suggest component is not configured
                suggestions.addAll(getCompanyNamesByPrefix(query, limit));
            }
            
            return suggestions;
            
        } catch (Exception e) {
            System.err.println("Company suggest failed, using fallback: " + e.getMessage());
            return getCompanyNamesByPrefix(query, limit);
        }
    }
    
    private List<Suggestion> getCompanyNamesByPrefix(String query, int limit) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        
        // Fallback: Use regular search with prefix matching
        String solrQuery = SOLR_URL + "/select?q=name:" + URLEncoder.encode(query + "*", StandardCharsets.UTF_8) + 
                          "&fl=name,name_s&rows=" + limit + "&wt=json";
        
        ResponseEntity<String> response = restTemplate.exchange(
            solrQuery, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.getBody());
        
        List<Suggestion> suggestions = new ArrayList<>();
        JsonNode docs = root.path("response").path("docs");
        
        Set<String> seenNames = new HashSet<>();
        for (JsonNode doc : docs) {
            String name = doc.path("name_s").asText();
            if (!name.isEmpty() && !seenNames.contains(name.toLowerCase())) {
                seenNames.add(name.toLowerCase());
                float score = calculatePrefixScore(query, name);
                suggestions.add(new Suggestion(name, "company", score, 
                    "Search for companies named: " + name));
            }
        }
        
        return suggestions;
    }
    
    private List<Suggestion> getIndustrySuggestions(String query, int limit) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        
        // Get industry facets that match the query
        String solrQuery = SOLR_URL + "/select?q=*:*&facet=true&facet.field=industry&facet.limit=" + (limit * 3) + 
                          "&facet.mincount=1&facet.prefix=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&rows=0&wt=json";
        
        ResponseEntity<String> response = restTemplate.exchange(
            solrQuery, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.getBody());
        
        List<Suggestion> suggestions = new ArrayList<>();
        JsonNode facetFields = root.path("facet_counts").path("facet_fields").path("industry");
        
        if (facetFields.isArray()) {
            for (int i = 0; i < facetFields.size() - 1; i += 2) {
                String industry = facetFields.get(i).asText();
                int count = facetFields.get(i + 1).asInt();
                
                if (industry.toLowerCase().contains(query)) {
                    float score = calculateFacetScore(query, industry, count);
                    suggestions.add(new Suggestion(industry, "industry", score, 
                        "Search in " + industry + " industry (" + count + " companies)"));
                }
            }
        }
        
        // Sort by score and limit
        suggestions.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
        if (suggestions.size() > limit) {
            suggestions = suggestions.subList(0, limit);
        }
        
        return suggestions;
    }
    
    private List<Suggestion> getLocationSuggestions(String query, int limit) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        
        // Get location facets that match the query
        String solrQuery = SOLR_URL + "/select?q=*:*&facet=true&facet.field=locality&facet.limit=" + (limit * 3) + 
                          "&facet.mincount=1&rows=0&wt=json";
        
        ResponseEntity<String> response = restTemplate.exchange(
            solrQuery, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.getBody());
        
        List<Suggestion> suggestions = new ArrayList<>();
        JsonNode facetFields = root.path("facet_counts").path("facet_fields").path("locality");
        
        if (facetFields.isArray()) {
            for (int i = 0; i < facetFields.size() - 1; i += 2) {
                String location = facetFields.get(i).asText();
                int count = facetFields.get(i + 1).asInt();
                
                if (location.toLowerCase().contains(query)) {
                    float score = calculateFacetScore(query, location, count);
                    suggestions.add(new Suggestion(location, "location", score, 
                        "Search companies in " + location + " (" + count + " companies)"));
                }
            }
        }
        
        // Sort by score and limit
        suggestions.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
        if (suggestions.size() > limit) {
            suggestions = suggestions.subList(0, limit);
        }
        
        return suggestions;
    }
    
    private List<Suggestion> getQueryTemplateSuggestions(String query, int limit) {
        List<Suggestion> suggestions = new ArrayList<>();
        
        // Use QueryUnderstandingService to detect entities
        QueryUnderstandingService.ParsedQuery parsedQuery = queryUnderstandingService.parseQuery(query);
        
        // Suggest natural language query templates based on detected entities
        if (parsedQuery.hasIndustryFilters()) {
            for (String industry : parsedQuery.getIndustries()) {
                suggestions.add(new Suggestion(
                    industry + " companies",
                    "template",
                    0.8f,
                    "Find all " + industry + " companies"
                ));
                suggestions.add(new Suggestion(
                    industry + " companies in california",
                    "template",
                    0.7f,
                    "Find " + industry + " companies in California"
                ));
            }
        }
        
        if (parsedQuery.hasLocationFilters()) {
            for (String location : parsedQuery.getLocations()) {
                suggestions.add(new Suggestion(
                    "tech companies in " + location,
                    "template",
                    0.8f,
                    "Find technology companies in " + location
                ));
                suggestions.add(new Suggestion(
                    "startups in " + location,
                    "template",
                    0.7f,
                    "Find startups in " + location
                ));
            }
        }
        
        // Generic query suggestions based on partial input
        if (query.length() >= 3) {
            String[] commonQueries = {
                "technology companies",
                "startups",
                "financial services",
                "healthcare companies",
                "software companies",
                "consulting firms",
                "manufacturing companies",
                "retail companies"
            };
            
            for (String commonQuery : commonQueries) {
                if (commonQuery.toLowerCase().contains(query)) {
                    float score = calculatePrefixScore(query, commonQuery);
                    suggestions.add(new Suggestion(
                        commonQuery,
                        "template", 
                        score * 0.6f, // Lower priority than exact matches
                        "Search for " + commonQuery
                    ));
                }
            }
        }
        
        // Sort by score and limit
        suggestions.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
        if (suggestions.size() > limit) {
            suggestions = suggestions.subList(0, limit);
        }
        
        return suggestions;
    }
    
    private float calculatePrefixScore(String query, String candidate) {
        String lowerQuery = query.toLowerCase();
        String lowerCandidate = candidate.toLowerCase();
        
        if (lowerCandidate.equals(lowerQuery)) {
            return 1.0f; // Exact match
        } else if (lowerCandidate.startsWith(lowerQuery)) {
            return 0.9f - (lowerCandidate.length() - lowerQuery.length()) * 0.01f; // Prefix match
        } else if (lowerCandidate.contains(lowerQuery)) {
            return 0.7f - (lowerCandidate.length() - lowerQuery.length()) * 0.01f; // Contains match
        } else {
            return 0.1f; // Low relevance
        }
    }
    
    private float calculateFacetScore(String query, String facetValue, int count) {
        float baseScore = calculatePrefixScore(query, facetValue);
        float countBoost = Math.min(1.0f, count / 1000.0f); // Boost based on popularity
        return baseScore * (0.7f + countBoost * 0.3f);
    }
    
    public static class AutosuggestResponse {
        private List<Suggestion> allSuggestions = new ArrayList<>();
        private List<Suggestion> companyNames = new ArrayList<>();
        private List<Suggestion> industries = new ArrayList<>();
        private List<Suggestion> locations = new ArrayList<>();
        private List<Suggestion> queryTemplates = new ArrayList<>();
        
        public List<Suggestion> getAllSuggestions() { return allSuggestions; }
        public void setAllSuggestions(List<Suggestion> allSuggestions) { this.allSuggestions = allSuggestions; }
        
        public List<Suggestion> getCompanyNames() { return companyNames; }
        public void setCompanyNames(List<Suggestion> companyNames) { this.companyNames = companyNames; }
        
        public List<Suggestion> getIndustries() { return industries; }
        public void setIndustries(List<Suggestion> industries) { this.industries = industries; }
        
        public List<Suggestion> getLocations() { return locations; }
        public void setLocations(List<Suggestion> locations) { this.locations = locations; }
        
        public List<Suggestion> getQueryTemplates() { return queryTemplates; }
        public void setQueryTemplates(List<Suggestion> queryTemplates) { this.queryTemplates = queryTemplates; }
    }
    
    public static class Suggestion {
        private String text;
        private String type;
        private float score;
        private String description;
        
        public Suggestion() {}
        
        public Suggestion(String text, String type, float score, String description) {
            this.text = text;
            this.type = type;
            this.score = score;
            this.description = description;
        }
        
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public float getScore() { return score; }
        public void setScore(float score) { this.score = score; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        @Override
        public String toString() {
            return "Suggestion{" +
                    "text='" + text + '\'' +
                    ", type='" + type + '\'' +
                    ", score=" + score +
                    ", description='" + description + '\'' +
                    '}';
        }
    }
}