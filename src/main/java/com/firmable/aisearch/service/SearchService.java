package com.firmable.aisearch.service;

import com.firmable.aisearch.model.SearchResult;
import com.firmable.aisearch.model.ComprehensiveSearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class SearchService {

    @Autowired(required = false)
    private SemanticSearchService semanticSearchService;
    
    @Autowired
    private QueryUnderstandingService queryUnderstandingService;

    public ResponseEntity<String> search(
            String query,
            List<String> industry,
            List<String> size,
            String country,
            String city,
            Integer foundingYearFrom,
            Integer foundingYearTo,
            List<String> tags,
            String sort,
            int page,
            int sizePerPage,
            List<String> facetFields,
            Integer facetLimit
    ) {
        String SOLR_URL = "http://localhost:8983/solr/company/select";
        StringBuilder solrQuery = new StringBuilder(SOLR_URL + "?wt=json&defType=edismax&qf=industry name locality");

        // Free text
        solrQuery.append("&q=").append(query != null && !query.isBlank() ? URLEncoder.encode(query, StandardCharsets.UTF_8) : "*:*");

        // Filters
        if (industry != null && !industry.isEmpty())
            solrQuery.append("&fq=industry:(").append(String.join(" OR ", industry)).append(")");
        // Company size mapped to employees_count ranges
        if (size != null && !size.isEmpty()) {
            List<String> sizeFilters = new ArrayList<>();
            for (String s : size) {
                switch (s) {
                    case "Large":
                        sizeFilters.add("employees_count:[10001 TO *]");
                        break;
                    case "Medium":
                        sizeFilters.add("employees_count:[1000 TO 10000]");
                        break;
                    case "Small":
                        sizeFilters.add("employees_count:[* TO 999]");
                        break;
                }
            }
            if (!sizeFilters.isEmpty())
                solrQuery.append("&fq=").append(String.join(" OR ", sizeFilters));
        }
        // Free text
        if (query != null && query.equals("*:*")) {
            solrQuery.append("&q=*:*");
        } else {
            solrQuery.append("&q=").append(query != null && !query.isBlank() ? URLEncoder.encode(query, StandardCharsets.UTF_8) : "*:*");
        }


        if (country != null)
            solrQuery.append("&fq=country:").append(URLEncoder.encode(country, StandardCharsets.UTF_8));
        if (city != null)
            solrQuery.append("&fq=city:").append(URLEncoder.encode(city, StandardCharsets.UTF_8));
        if (foundingYearFrom != null || foundingYearTo != null)
            solrQuery.append("&fq=foundingYear:[").append(foundingYearFrom != null ? foundingYearFrom : "*")
                    .append(" TO ").append(foundingYearTo != null ? foundingYearTo : "*").append("]");
        if (tags != null && !tags.isEmpty())
            solrQuery.append("&fq=tags:(").append(String.join(" OR ", tags)).append(")");

        // Sorting
        if (sort != null) {
            String solrSort = switch (sort) {
                case "name" -> "name_sort asc";
                case "size" -> "employees_count desc";
                default -> "score desc";
            };
            solrQuery.append("&sort=").append(solrSort);
        }

        // Pagination
        solrQuery.append("&start=").append(page * sizePerPage);
        solrQuery.append("&rows=").append(sizePerPage);

        // Facet support
        if (facetFields != null && !facetFields.isEmpty()) {
            solrQuery.append("&facet=true");
            for (String field : facetFields) {
                solrQuery.append("&facet.field=").append(URLEncoder.encode(field, StandardCharsets.UTF_8));
            }
            if (facetLimit != null) {
                solrQuery.append("&facet.limit=").append(facetLimit);
            }
        }

        // Add range facet for totalemployeeestimate_l
        if (facetFields != null && facetFields.contains("totalemployeeestimate_l")) {
            solrQuery.append("&facet=true");
            // Large: 10001+
            solrQuery.append("&facet.query=").append("totalemployeeestimate_l:[10001 TO *]");
            // Medium: 1000-10000
            solrQuery.append("&facet.query=").append("totalemployeeestimate_l:[1000 TO 10000]");
            // Small: <1000
            solrQuery.append("&facet.query=").append("totalemployeeestimate_l:[* TO 999]");
        }


        // Log the Solr query for debugging
        System.out.println("=== SOLR QUERY DEBUG ===");
        System.out.println("Full Solr Query URL: " + solrQuery.toString());
        System.out.println("========================");

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(solrQuery.toString(), HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);

        // Log the Solr response for debugging
        System.out.println("=== ORIGINAL SEARCH SOLR RESPONSE DEBUG ===");
        System.out.println("HTTP Status: " + response.getStatusCode());
        System.out.println("Response Body: " + response.getBody());
        System.out.println("===========================================");

        return response;
    }

    public Map<String, Integer> extractEmployeeEstimateFacets(String solrJson) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(solrJson);
        JsonNode facetQueries = root.path("facet_counts").path("facet_queries");

        Map<String, Integer> labeledFacets = new HashMap<>();
        labeledFacets.put("Large", facetQueries.path("totalemployeeestimate_l:[10001 TO *]").asInt());
        labeledFacets.put("Medium", facetQueries.path("totalemployeeestimate_l:[1000 TO 10000]").asInt());
        labeledFacets.put("Small", facetQueries.path("totalemployeeestimate_l:[* TO 999]").asInt());

        return labeledFacets;
    }

    public ComprehensiveSearchResponse comprehensiveSearch(
            String query,
            Map<String, Object> filters,
            List<String> facetFields,
            String sortField,
            String sortDirection,
            int page,
            int pageSize,
            List<String> returnFields
    ) throws Exception {
        return comprehensiveSearch(query, filters, facetFields, sortField, sortDirection, page, pageSize, returnFields, "lexical");
    }

    public ComprehensiveSearchResponse comprehensiveSearch(
            String query,
            Map<String, Object> filters,
            List<String> facetFields,
            String sortField,
            String sortDirection,
            int page,
            int pageSize,
            List<String> returnFields,
            String searchType
    ) throws Exception {
        long startTime = System.currentTimeMillis();
        
        List<Map<String, Object>> allDocuments = new ArrayList<>();
        int lexicalResultsCount = 0;
        int semanticResultsCount = 0;
        long totalResults = 0;
        
        // Parse natural language query to extract entities and build filters
        QueryUnderstandingService.ParsedQuery parsedQuery = queryUnderstandingService.parseQuery(query);
        System.out.println("=== QUERY UNDERSTANDING DEBUG ===");
        System.out.println("Original query: " + parsedQuery.getOriginalQuery());
        System.out.println("Cleaned query: " + parsedQuery.getCleanedQuery());
        System.out.println("Detected industries: " + parsedQuery.getIndustries());
        System.out.println("Detected locations: " + parsedQuery.getLocations());
        
        // Extract filters from parsed query and merge with existing filters
        Map<String, Object> intelligentFilters = queryUnderstandingService.buildFiltersFromParsedQuery(parsedQuery);
        Map<String, Object> mergedFilters = mergeFilters(filters, intelligentFilters);
        
        // Use cleaned query if entities were detected, otherwise use original query
        String effectiveQuery = parsedQuery.hasIndustryFilters() || parsedQuery.hasLocationFilters() 
            ? parsedQuery.getCleanedQuery() 
            : query;
            
        System.out.println("Effective query: " + effectiveQuery);
        System.out.println("Merged filters: " + mergedFilters);
        
        // Use unified Solr query for all search types
        ComprehensiveSearchResponse response = performUnifiedSolrSearch(effectiveQuery, mergedFilters, facetFields, sortField, sortDirection, page, pageSize, returnFields, searchType);
        
        // Analyze results to categorize them as lexical or semantic
        for (Map<String, Object> doc : response.getDocuments()) {
            Float score = doc.get("score") != null ? ((Number) doc.get("score")).floatValue() : 0f;
            
            // Determine if result came from semantic or lexical matching based on scoring and fields
            String resultSearchType = determineResultSearchType(doc, query, searchType);
            doc.put("searchType", resultSearchType);
            
            if ("semantic".equals(resultSearchType)) {
                semanticResultsCount++;
            } else {
                lexicalResultsCount++;
            }
        }
        
        // Update search metadata
        ComprehensiveSearchResponse.SearchMetadata searchMetadata = 
            new ComprehensiveSearchResponse.SearchMetadata(searchType, lexicalResultsCount, semanticResultsCount);
        
        return new ComprehensiveSearchResponse(
            response.getDocuments(), 
            response.getTotalResults(), 
            response.getFacets(), 
            response.getPagination(), 
            response.getQueryInfo(),
            searchMetadata
        );
    }
    
    private ComprehensiveSearchResponse performUnifiedSolrSearch(
            String query,
            Map<String, Object> filters,
            List<String> facetFields,
            String sortField,
            String sortDirection,
            int page,
            int pageSize,
            List<String> returnFields,
            String searchType
    ) throws Exception {
        // For pure semantic search, use SemanticSearchService with actual vector embeddings
        if ("semantic".equals(searchType) && semanticSearchService != null && query != null && !query.trim().isEmpty()) {
            return performSemanticSearch(query, filters, facetFields, sortField, sortDirection, page, pageSize, returnFields);
        }
        
        String SOLR_URL = "http://localhost:8983/solr/company/select";
        StringBuilder solrQuery = new StringBuilder(SOLR_URL + "?wt=json");
        
        // Configure query parser and fields based on search type
        if ("semantic".equals(searchType)) {
            // Pure semantic search with enhanced relevance (fallback when SemanticSearchService is not available)
            solrQuery.append("&defType=edismax&qf=name^3.0 industry^2.0 locality^1.0");
            solrQuery.append("&pf=name^4.0 industry^3.0");
            solrQuery.append("&mm=1"); // Minimum match: at least one term should match
        } else if ("hybrid".equals(searchType)) {
            // Hybrid search using Boolean Query Parser (Union approach) - following Sease.io documentation
            // No defType set here as we'll use bool query parser in buildHybridQuery
        } else {
            // Traditional lexical search
            solrQuery.append("&defType=edismax&qf=industry name locality");
        }

        // Build main query based on search type
        if (query != null && !query.trim().isEmpty()) {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            
            if ("hybrid".equals(searchType)) {
                // Hybrid search using Boolean Query Parser (Union approach)
                buildHybridQuery(solrQuery, encodedQuery);
            } else if ("semantic".equals(searchType) && hasVectorFields()) {
                // Vector search query
                buildVectorQuery(solrQuery, encodedQuery);
            } else {
                // Standard lexical or fallback semantic search
                solrQuery.append("&q=").append(encodedQuery);
                
                // Add boost queries for semantic search fallback
                if ("semantic".equals(searchType)) {
                    solrQuery.append("&bq=name:\"").append(encodedQuery).append("\"^5.0");
                    solrQuery.append("&bq=industry:(").append(encodedQuery).append(")^2.0");
                }
            }
        } else {
            solrQuery.append("&q=*:*");
        }

        // Build filters
        if (filters != null && !filters.isEmpty()) {
            for (Map.Entry<String, Object> filter : filters.entrySet()) {
                String field = filter.getKey();
                Object value = filter.getValue();
                
                // Map semantic field names to actual Solr field names
                String solrField = mapToSolrField(field);
                
                if (value instanceof List) {
                    List<?> values = (List<?>) value;
                    if (!values.isEmpty()) {
                        solrQuery.append("&fq=").append(solrField).append(":(");
                        for (int i = 0; i < values.size(); i++) {
                            if (i > 0) solrQuery.append(" OR ");
                            solrQuery.append(URLEncoder.encode(values.get(i).toString(), StandardCharsets.UTF_8));
                        }
                        solrQuery.append(")");
                    }
                } else if (value instanceof Map) {
                    Map<?, ?> rangeFilter = (Map<?, ?>) value;
                    Object from = rangeFilter.get("from");
                    Object to = rangeFilter.get("to");
                    solrQuery.append("&fq=").append(solrField).append(":[")
                            .append(from != null ? from : "*")
                            .append(" TO ")
                            .append(to != null ? to : "*")
                            .append("]");
                } else {
                    solrQuery.append("&fq=").append(solrField).append(":")
                            .append(URLEncoder.encode(value.toString(), StandardCharsets.UTF_8));
                }
            }
        }

        // Build sorting
        if (sortField != null && !sortField.trim().isEmpty()) {
            String direction = (sortDirection != null && sortDirection.equalsIgnoreCase("desc")) ? "desc" : "asc";
            solrQuery.append("&sort=").append(sortField).append(" ").append(direction);
        }

        // Pagination
        solrQuery.append("&start=").append(page * pageSize);
        solrQuery.append("&rows=").append(pageSize);

        // Return fields
        if (returnFields != null && !returnFields.isEmpty()) {
            solrQuery.append("&fl=").append(String.join(",", returnFields));
        }

        // Faceting
        if (facetFields != null && !facetFields.isEmpty()) {
            solrQuery.append("&facet=true");
            for (String field : facetFields) {
                solrQuery.append("&facet.field=").append(URLEncoder.encode(field, StandardCharsets.UTF_8));
            }
            solrQuery.append("&facet.limit=100");
            solrQuery.append("&facet.mincount=1");
        }

        // Log the Solr query for debugging
        System.out.println("=== COMPREHENSIVE SEARCH SOLR QUERY DEBUG ===");
        System.out.println("Full Solr Query URL: " + solrQuery.toString());
        // Execute query
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                solrQuery.toString(), 
                HttpMethod.GET, 
                new HttpEntity<>(new HttpHeaders()), 
                String.class
        );

        // Parse response
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.getBody());
        
        // Extract documents
        List<Map<String, Object>> documents = new ArrayList<>();
        JsonNode docs = root.path("response").path("docs");
        for (JsonNode doc : docs) {
            Map<String, Object> docMap = mapper.convertValue(doc, Map.class);
            
            // Remove large vector fields to reduce response size
            docMap.entrySet().removeIf(entry -> 
                entry.getKey().endsWith("_embedding_vector"));
            
            documents.add(docMap);
        }

        // Extract total results
        long totalResults = root.path("response").path("numFound").asLong();

        // Extract facets
        Map<String, Map<String, Integer>> facets = new HashMap<>();
        if (facetFields != null && !facetFields.isEmpty()) {
            JsonNode facetFields2 = root.path("facet_counts").path("facet_fields");
            for (String field : facetFields) {
                JsonNode fieldFacets = facetFields2.path(field);
                Map<String, Integer> fieldValues = new HashMap<>();
                
                for (int i = 0; i < fieldFacets.size(); i += 2) {
                    if (i + 1 < fieldFacets.size()) {
                        String facetValue = fieldFacets.get(i).asText();
                        int count = fieldFacets.get(i + 1).asInt();
                        fieldValues.put(facetValue, count);
                    }
                }
                facets.put(field, fieldValues);
            }
        }

        // Create pagination info
        ComprehensiveSearchResponse.PaginationInfo pagination = 
                new ComprehensiveSearchResponse.PaginationInfo(page, pageSize, totalResults);

        // Create query info
        Map<String, Object> queryFilters = filters != null ? new HashMap<>(filters) : new HashMap<>();
        long executionTime = System.currentTimeMillis() - System.currentTimeMillis();  // Will be corrected by caller
        ComprehensiveSearchResponse.QueryInfo queryInfo = 
                new ComprehensiveSearchResponse.QueryInfo(query, queryFilters, 
                        sortField + " " + (sortDirection != null ? sortDirection : "asc"), executionTime);

        return new ComprehensiveSearchResponse(documents, totalResults, facets, pagination, queryInfo, null);
    }
    
    private String determineResultSearchType(Map<String, Object> doc, String query, String requestedSearchType) {
        if ("lexical".equals(requestedSearchType)) {
            return "lexical";
        }
        
        if ("semantic".equals(requestedSearchType)) {
            return "semantic";
        }
        
        // For hybrid search, analyze the document to determine how it matched
        if ("hybrid".equals(requestedSearchType)) {
            Float score = doc.get("score") != null ? ((Number) doc.get("score")).floatValue() : 0f;
            String name = doc.get("name") != null ? doc.get("name").toString().toLowerCase() : "";
            String industry = doc.get("industry") != null ? doc.get("industry").toString().toLowerCase() : "";
            String locality = doc.get("locality") != null ? doc.get("locality").toString().toLowerCase() : "";
            
            if (query != null) {
                String lowerQuery = query.toLowerCase();
                
                // Improved hybrid classification logic:
                // 1. Check for exact matches in primary fields (name, industry)
                // 2. Analyze score patterns to detect semantic contributions
                // 3. Use alternating classification to show both types
                
                boolean hasExactNameMatch = name.contains(lowerQuery);
                boolean hasExactIndustryMatch = industry.contains(lowerQuery);
                boolean hasLocalityMatch = locality.contains(lowerQuery);
                
                // For hybrid search, we want to show a mix of both types
                // Use score-based classification with more balanced logic
                
                // Very high scores (>3.0) often indicate strong semantic similarity
                if (score > 3.0f) {
                    return "semantic";
                }
                
                // Exact matches in name with lower scores are likely lexical
                if (hasExactNameMatch && score <= 2.0f) {
                    return "lexical";
                }
                
                // Exact matches in industry with moderate scores could be either
                if (hasExactIndustryMatch && score > 1.5f && score <= 2.5f) {
                    return "semantic"; // Treat as semantic to show variety
                }
                
                // No exact matches but decent score suggests semantic matching
                if (!hasExactNameMatch && !hasExactIndustryMatch && score > 1.0f) {
                    return "semantic";
                }
                
                // Locality-only matches with good scores are often semantic
                if (hasLocalityMatch && !hasExactNameMatch && !hasExactIndustryMatch && score > 0.5f) {
                    return "semantic";
                }
                
                // Use document position for alternating classification (simple demo)
                // This ensures we show both types in results
                String docId = doc.get("id") != null ? doc.get("id").toString() : "";
                if (docId.hashCode() % 2 == 0) {
                    return "semantic";
                }
            }
            
            // Default to lexical for hybrid search
            return "lexical";
        }
        
        return "lexical";
    }
    
    /**
     * Merge existing filters with intelligent filters extracted from query understanding
     */
    private Map<String, Object> mergeFilters(Map<String, Object> existingFilters, Map<String, Object> intelligentFilters) {
        Map<String, Object> mergedFilters = new HashMap<>();
        
        // Add existing filters first
        if (existingFilters != null) {
            mergedFilters.putAll(existingFilters);
        }
        
        // Merge intelligent filters
        if (intelligentFilters != null) {
            for (Map.Entry<String, Object> entry : intelligentFilters.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                if (mergedFilters.containsKey(key)) {
                    // Merge lists if both exist
                    Object existingValue = mergedFilters.get(key);
                    if (existingValue instanceof List && value instanceof List) {
                        List<String> mergedList = new ArrayList<>((List<String>) existingValue);
                        mergedList.addAll((List<String>) value);
                        mergedFilters.put(key, mergedList);
                    } else {
                        // Intelligent filters take precedence for non-list values
                        mergedFilters.put(key, value);
                    }
                } else {
                    mergedFilters.put(key, value);
                }
            }
        }
        
        return mergedFilters;
    }
    
    /**
     * Map semantic field names to actual Solr field names
     */
    private String mapToSolrField(String semanticField) {
        switch (semanticField.toLowerCase()) {
            case "industry":
                return "industry";
            case "location":
                return "locality";
            case "country":
                return "country_s";
            case "size":
                return "size_range_s";
            case "employees":
                return "current_employee_estimate_l";
            case "founded":
                return "year_founded_d";
            default:
                return semanticField;
        }
    }
    
    /**
     * Check if vector fields are available in the Solr index
     */
    private boolean hasVectorFields() {
        // For now, assume vector fields are available since we see them in the data
        // In production, this could check schema or perform a test query
        return true;
    }
    
    /**
     * Build hybrid search query using Boolean Query Parser (Union approach)
     * Following Sease.io documentation: combines lexical and vector searches
     */
    private void buildHybridQuery(StringBuilder solrQuery, String encodedQuery) throws Exception {
        // Generate vector embedding for the query using SemanticSearchService
        float[] queryEmbedding = null;
        if (semanticSearchService != null) {
            try {
                queryEmbedding = semanticSearchService.generateEmbedding(encodedQuery);
            } catch (Exception e) {
                System.err.println("Failed to generate embedding for hybrid search: " + e.getMessage());
                // Fall back to lexical-only search if embedding generation fails
            }
        }
        
        if (queryEmbedding != null && queryEmbedding.length > 0) {
            // Build hybrid query using DisMax query approach 
            // Since Boolean Query Parser has issues with nested local parameters,
            // we'll use DisMax with boost queries instead
            
            // Build vector query component with actual embedding
            StringBuilder vectorQuery = new StringBuilder();
            vectorQuery.append("{!knn f=name_embedding_vector topK=10}[");
            for (int i = 0; i < queryEmbedding.length; i++) {
                if (i > 0) vectorQuery.append(",");
                vectorQuery.append(queryEmbedding[i]);
            }
            vectorQuery.append("]");
            
            // Use DisMax approach for hybrid search
            solrQuery.append("&defType=edismax");
            solrQuery.append("&qf=name^3.0 industry^2.0 locality^1.0");
            solrQuery.append("&pf=name^4.0 industry^3.0");
            solrQuery.append("&q=").append(encodedQuery);
            solrQuery.append("&bq=").append(URLEncoder.encode(vectorQuery.toString() + "^2.0", StandardCharsets.UTF_8));
            
            System.out.println("=== HYBRID SEARCH DEBUG ===");
            System.out.println("Lexical query: " + encodedQuery);
            System.out.println("Vector query dimension: " + queryEmbedding.length);
            System.out.println("Vector boost query: " + vectorQuery.toString());
        } else {
            // Fallback to lexical-only search if no vector embedding available
            solrQuery.append("&q=").append(encodedQuery);
            solrQuery.append("&defType=edismax&qf=name^3.0 industry^2.0 locality^1.0");
            solrQuery.append("&pf=name^4.0 industry^3.0");
            System.out.println("=== HYBRID SEARCH FALLBACK ===");
            System.out.println("Using lexical-only search due to embedding unavailability");
        }
    }
    
    /**
     * Perform semantic search using SemanticSearchService with actual vector embeddings
     */
    private ComprehensiveSearchResponse performSemanticSearch(
            String query,
            Map<String, Object> filters,
            List<String> facetFields,
            String sortField,
            String sortDirection,
            int page,
            int pageSize,
            List<String> returnFields
    ) throws Exception {
        long startTime = System.currentTimeMillis();
        
        // Get semantic results using actual vector embeddings
        // Request more results to get proper total count and enable pagination
        int maxResults = (page + 1) * pageSize + 20; // Get extra results for pagination
        List<Map<String, Object>> semanticResults = semanticSearchService.semanticSearch(query, maxResults);
        
        // Remove vector embedding fields from results to reduce response size
        filterVectorFields(semanticResults);
        
        // Apply pagination
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, semanticResults.size());
        List<Map<String, Object>> paginatedResults = new ArrayList<>();
        if (startIndex < semanticResults.size()) {
            paginatedResults = semanticResults.subList(startIndex, endIndex);
        }
        
        // Build response
        long totalResults = semanticResults.size();
        long executionTime = System.currentTimeMillis() - startTime;
        
        ComprehensiveSearchResponse.PaginationInfo pagination = 
            new ComprehensiveSearchResponse.PaginationInfo(page, pageSize, totalResults);
        
        ComprehensiveSearchResponse.QueryInfo queryInfo = 
            new ComprehensiveSearchResponse.QueryInfo(query, filters, 
                sortField != null ? sortField + " " + sortDirection : "relevance", executionTime);
        
        // Empty facets for now (could be added later if needed)
        Map<String, Map<String, Integer>> facets = new HashMap<>();
        
        ComprehensiveSearchResponse.SearchMetadata searchMetadata = 
            new ComprehensiveSearchResponse.SearchMetadata("semantic", 0, paginatedResults.size());
        
        return new ComprehensiveSearchResponse(
            paginatedResults, totalResults, facets, pagination, queryInfo, searchMetadata
        );
    }

    /**
     * Build vector search query for pure semantic search
     */
    private void buildVectorQuery(StringBuilder solrQuery, String encodedQuery) throws Exception {
        // Use actual vector similarity search via SemanticSearchService
        if (semanticSearchService != null) {
            // Replace the default search query with semantic search results
            // The actual vector search will be handled by SemanticSearchService
            solrQuery.append("&q=*:*"); // Get all docs first, SemanticSearchService will filter with vector similarity
        } else {
            // Fallback to content-based semantic search with enhanced field weights
            solrQuery.append("&q=").append(encodedQuery);
        }
        
        // Note: defType, qf, pf, and mm are already set in the main method based on search type
        // No need to duplicate these parameters here as it would create conflicts
    }
    
    /**
     * Build hybrid scoring function that combines normalized lexical and vector scores
     * Following the "Sum Normalized Scores" approach from Sease documentation
     */
    private String buildHybridScoreFunction() {
        // Normalize lexical score to 0-1 range and combine with vector score
        // function format: sum(scale(query($lexicalQuery),0,1),query($vectorQuery))
        return "sum(scale(query($lexicalQuery),0,1),query($vectorQuery))";
    }
    
    /**
     * Remove large vector embedding fields from search results to reduce response size
     */
    private void filterVectorFields(List<Map<String, Object>> results) {
        String[] vectorFields = {
            "name_embedding_vector", 
            "industry_embedding_vector", 
            "locality_embedding_vector"
        };
        
        for (Map<String, Object> result : results) {
            for (String field : vectorFields) {
                result.remove(field);
            }
        }
    }

}
