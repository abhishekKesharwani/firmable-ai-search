package com.firmable.aisearch.controller;

import com.firmable.aisearch.service.AutosuggestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/autosuggest")
@CrossOrigin(origins = "*")
public class AutosuggestController {

    @Autowired
    private AutosuggestService autosuggestService;

    @GetMapping
    public ResponseEntity<AutosuggestService.AutosuggestResponse> getAutosuggestions(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            AutosuggestService.AutosuggestResponse response = autosuggestService.getAutosuggestions(query, limit);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error in autosuggest endpoint: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new AutosuggestService.AutosuggestResponse());
        }
    }

    @GetMapping("/companies")
    public ResponseEntity<AutosuggestService.AutosuggestResponse> getCompanySuggestions(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            AutosuggestService.AutosuggestResponse response = autosuggestService.getAutosuggestions(query, limit);
            AutosuggestService.AutosuggestResponse companiesOnly = new AutosuggestService.AutosuggestResponse();
            companiesOnly.setCompanyNames(response.getCompanyNames());
            companiesOnly.setAllSuggestions(response.getCompanyNames());
            return ResponseEntity.ok(companiesOnly);
        } catch (Exception e) {
            System.err.println("Error in company suggestions endpoint: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new AutosuggestService.AutosuggestResponse());
        }
    }

    @GetMapping("/industries")
    public ResponseEntity<AutosuggestService.AutosuggestResponse> getIndustrySuggestions(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            AutosuggestService.AutosuggestResponse response = autosuggestService.getAutosuggestions(query, limit);
            AutosuggestService.AutosuggestResponse industriesOnly = new AutosuggestService.AutosuggestResponse();
            industriesOnly.setIndustries(response.getIndustries());
            industriesOnly.setAllSuggestions(response.getIndustries());
            return ResponseEntity.ok(industriesOnly);
        } catch (Exception e) {
            System.err.println("Error in industry suggestions endpoint: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new AutosuggestService.AutosuggestResponse());
        }
    }

    @GetMapping("/locations")
    public ResponseEntity<AutosuggestService.AutosuggestResponse> getLocationSuggestions(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            AutosuggestService.AutosuggestResponse response = autosuggestService.getAutosuggestions(query, limit);
            AutosuggestService.AutosuggestResponse locationsOnly = new AutosuggestService.AutosuggestResponse();
            locationsOnly.setLocations(response.getLocations());
            locationsOnly.setAllSuggestions(response.getLocations());
            return ResponseEntity.ok(locationsOnly);
        } catch (Exception e) {
            System.err.println("Error in location suggestions endpoint: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new AutosuggestService.AutosuggestResponse());
        }
    }

    @GetMapping("/templates")
    public ResponseEntity<AutosuggestService.AutosuggestResponse> getQueryTemplateSuggestions(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            AutosuggestService.AutosuggestResponse response = autosuggestService.getAutosuggestions(query, limit);
            AutosuggestService.AutosuggestResponse templatesOnly = new AutosuggestService.AutosuggestResponse();
            templatesOnly.setQueryTemplates(response.getQueryTemplates());
            templatesOnly.setAllSuggestions(response.getQueryTemplates());
            return ResponseEntity.ok(templatesOnly);
        } catch (Exception e) {
            System.err.println("Error in query template suggestions endpoint: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new AutosuggestService.AutosuggestResponse());
        }
    }
}