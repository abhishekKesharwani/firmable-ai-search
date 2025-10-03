package com.firmable.aisearch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class SemanticSearchService {
    
    private static final String EMBEDDING_API_URL = "http://localhost:8085/embed";
    private static final String SOLR_URL = "http://localhost:8983/solr/company";
    
    public List<Map<String, Object>> semanticSearch(String query, int maxResults) throws Exception {
        // Generate embeddings for the query using the Flask API
        float[] queryEmbedding = generateEmbedding(query);
        
        if (queryEmbedding.length == 0) {
            // Fallback to empty results if embedding generation failed
            return new ArrayList<>();
        }
        
        // Perform vector search using the generated embeddings
        return performVectorSearch(queryEmbedding, maxResults);
    }
    
    public float[] generateEmbedding(String text) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Create request body for Flask API
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("text", text);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                EMBEDDING_API_URL, HttpMethod.POST, request, String.class);
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode embeddingNode = root.path("embedding");
            
            float[] embedding = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                embedding[i] = (float) embeddingNode.get(i).asDouble();
            }
            
            System.out.println("=== EMBEDDING DEBUG ===");
            System.out.println("Generated embedding for: '" + text + "'");
            System.out.println("Embedding dimension: " + embedding.length);
            System.out.println("Processing time: " + root.path("processing_time_ms").asDouble() + "ms");
            
            return embedding;
        } catch (Exception e) {
            // Fallback: return empty results if embedding service is unavailable
            System.err.println("Embedding service unavailable, falling back to lexical search: " + e.getMessage());
            return new float[0];
        }
    }
    
    private List<Map<String, Object>> performVectorSearch(float[] queryEmbedding, int maxResults) throws Exception {
        if (queryEmbedding.length == 0) {
            return new ArrayList<>();
        }
        
        RestTemplate restTemplate = new RestTemplate();
        
        // Build vector search query using name_embedding_vector field
        // We'll search against the name embeddings first, but could be extended to search multiple fields
        StringBuilder vectorQuery = new StringBuilder();
        vectorQuery.append("{!knn f=name_embedding_vector topK=").append(maxResults).append("}");
        vectorQuery.append("[");
        for (int i = 0; i < queryEmbedding.length; i++) {
            if (i > 0) vectorQuery.append(",");
            vectorQuery.append(queryEmbedding[i]);
        }
        vectorQuery.append("]");
        
        String solrQuery = SOLR_URL + "/select?q=" + 
            URLEncoder.encode(vectorQuery.toString(), StandardCharsets.UTF_8) + 
            "&wt=json&rows=" + maxResults +
            "&fl=id,name,name_s,industry,industry_s,locality,locality_ss,country_s,domain_s,linkedin_url_s,current_employee_estimate_l,totalemployeeestimate_l,year_founded_d,size_range_s,score";
        
        System.out.println("=== VECTOR SEARCH DEBUG ===");
        System.out.println("Vector Search Query: " + solrQuery);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                solrQuery, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            
            List<Map<String, Object>> results = new ArrayList<>();
            JsonNode docs = root.path("response").path("docs");
            for (JsonNode doc : docs) {
                @SuppressWarnings("unchecked")
                Map<String, Object> docMap = mapper.convertValue(doc, Map.class);
                docMap.put("searchType", "semantic");
                results.add(docMap);
            }
            
            System.out.println("Vector search returned " + results.size() + " results");
            return results;
        } catch (Exception e) {
            System.err.println("Vector search failed, returning empty results: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public List<Map<String, Object>> hybridSearch(String query, int maxResults) throws Exception {
        // Get semantic results (top 60% of requested results)
        int semanticCount = (int) Math.ceil(maxResults * 0.6);
        List<Map<String, Object>> semanticResults = semanticSearch(query, semanticCount);
        
        // Note: Lexical results will be handled in SearchService
        // This method is mainly for getting semantic results for hybrid mode
        return semanticResults;
    }
}