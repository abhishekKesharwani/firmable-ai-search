package com.firmable.aisearch.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class QueryUnderstandingService {
    
    private static final Map<String, List<String>> INDUSTRY_KEYWORDS = new HashMap<>();
    private static final Map<String, List<String>> LOCATION_KEYWORDS = new HashMap<>();
    
    static {
        INDUSTRY_KEYWORDS.put("technology", Arrays.asList("tech", "technology", "software", "it", "computer", "digital", "internet", "web", "app", "mobile", "ai", "artificial intelligence", "machine learning", "data", "cloud", "saas", "platform", "development", "programming", "coding", "engineering"));
        INDUSTRY_KEYWORDS.put("finance", Arrays.asList("finance", "financial", "bank", "banking", "investment", "insurance", "fintech", "trading", "hedge fund", "private equity", "venture capital", "asset management"));
        INDUSTRY_KEYWORDS.put("healthcare", Arrays.asList("healthcare", "health", "medical", "pharma", "pharmaceutical", "biotech", "biotechnology", "hospital", "clinic", "doctor", "medicine", "therapy", "treatment"));
        INDUSTRY_KEYWORDS.put("retail", Arrays.asList("retail", "ecommerce", "e-commerce", "shopping", "store", "marketplace", "fashion", "clothing", "apparel", "consumer goods"));
        INDUSTRY_KEYWORDS.put("manufacturing", Arrays.asList("manufacturing", "factory", "production", "industrial", "automotive", "aerospace", "chemical", "materials", "machinery", "equipment"));
        INDUSTRY_KEYWORDS.put("education", Arrays.asList("education", "educational", "school", "university", "college", "learning", "training", "academic", "research", "edtech"));
        INDUSTRY_KEYWORDS.put("energy", Arrays.asList("energy", "oil", "gas", "renewable", "solar", "wind", "nuclear", "power", "electricity", "utilities", "green energy"));
        INDUSTRY_KEYWORDS.put("real estate", Arrays.asList("real estate", "property", "housing", "construction", "building", "architecture", "development", "commercial real estate"));
        INDUSTRY_KEYWORDS.put("media", Arrays.asList("media", "entertainment", "publishing", "news", "television", "tv", "radio", "film", "movie", "music", "gaming", "advertising", "marketing"));
        INDUSTRY_KEYWORDS.put("telecommunications", Arrays.asList("telecommunications", "telecom", "communications", "wireless", "mobile", "broadband", "internet", "networking", "connectivity"));
        
        LOCATION_KEYWORDS.put("california", Arrays.asList("california", "ca", "san francisco", "los angeles", "silicon valley", "bay area", "san diego", "sacramento", "oakland", "san jose"));
        LOCATION_KEYWORDS.put("new york", Arrays.asList("new york", "ny", "nyc", "manhattan", "brooklyn", "queens", "bronx", "albany", "buffalo", "rochester"));
        LOCATION_KEYWORDS.put("texas", Arrays.asList("texas", "tx", "houston", "dallas", "austin", "san antonio", "fort worth", "el paso", "arlington", "corpus christi"));
        LOCATION_KEYWORDS.put("florida", Arrays.asList("florida", "fl", "miami", "tampa", "orlando", "jacksonville", "tallahassee", "fort lauderdale", "west palm beach"));
        LOCATION_KEYWORDS.put("washington", Arrays.asList("washington", "wa", "seattle", "spokane", "tacoma", "vancouver", "bellevue", "everett", "kent", "renton"));
        LOCATION_KEYWORDS.put("illinois", Arrays.asList("illinois", "il", "chicago", "aurora", "rockford", "joliet", "naperville", "springfield", "peoria", "elgin"));
        LOCATION_KEYWORDS.put("pennsylvania", Arrays.asList("pennsylvania", "pa", "philadelphia", "pittsburgh", "allentown", "erie", "reading", "scranton", "bethlehem"));
        LOCATION_KEYWORDS.put("ohio", Arrays.asList("ohio", "oh", "columbus", "cleveland", "cincinnati", "toledo", "akron", "dayton", "parma", "canton"));
        LOCATION_KEYWORDS.put("georgia", Arrays.asList("georgia", "ga", "atlanta", "augusta", "columbus", "savannah", "athens", "sandy springs", "roswell"));
        LOCATION_KEYWORDS.put("north carolina", Arrays.asList("north carolina", "nc", "charlotte", "raleigh", "greensboro", "durham", "winston-salem", "fayetteville", "cary"));
        LOCATION_KEYWORDS.put("michigan", Arrays.asList("michigan", "mi", "detroit", "grand rapids", "warren", "sterling heights", "lansing", "ann arbor", "flint"));
        LOCATION_KEYWORDS.put("massachusetts", Arrays.asList("massachusetts", "ma", "boston", "worcester", "springfield", "lowell", "cambridge", "new bedford", "brockton"));
        LOCATION_KEYWORDS.put("virginia", Arrays.asList("virginia", "va", "virginia beach", "norfolk", "chesapeake", "richmond", "newport news", "alexandria", "hampton"));
        LOCATION_KEYWORDS.put("maryland", Arrays.asList("maryland", "md", "baltimore", "frederick", "rockville", "gaithersburg", "bowie", "hagerstown", "annapolis"));
        LOCATION_KEYWORDS.put("colorado", Arrays.asList("colorado", "co", "denver", "colorado springs", "aurora", "fort collins", "lakewood", "thornton", "arvada"));
        LOCATION_KEYWORDS.put("united states", Arrays.asList("usa", "us", "united states", "america", "american"));
        LOCATION_KEYWORDS.put("canada", Arrays.asList("canada", "canadian", "toronto", "vancouver", "montreal", "calgary", "ottawa", "edmonton", "mississauga"));
        LOCATION_KEYWORDS.put("united kingdom", Arrays.asList("uk", "united kingdom", "britain", "england", "london", "manchester", "birmingham", "leeds", "glasgow", "scotland"));
        LOCATION_KEYWORDS.put("germany", Arrays.asList("germany", "german", "berlin", "munich", "hamburg", "cologne", "frankfurt", "stuttgart", "dusseldorf"));
        LOCATION_KEYWORDS.put("france", Arrays.asList("france", "french", "paris", "marseille", "lyon", "toulouse", "nice", "nantes", "strasbourg"));
        LOCATION_KEYWORDS.put("india", Arrays.asList("india", "indian", "mumbai", "delhi", "bangalore", "hyderabad", "chennai", "kolkata", "pune", "ahmedabad"));
        LOCATION_KEYWORDS.put("china", Arrays.asList("china", "chinese", "beijing", "shanghai", "guangzhou", "shenzhen", "wuhan", "chengdu", "dongguan"));
        LOCATION_KEYWORDS.put("japan", Arrays.asList("japan", "japanese", "tokyo", "osaka", "yokohama", "nagoya", "sapporo", "kobe", "kyoto", "fukuoka"));
    }
    
    public ParsedQuery parseQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ParsedQuery();
        }
        
        String normalizedQuery = query.toLowerCase().trim();
        ParsedQuery parsed = new ParsedQuery();
        parsed.setOriginalQuery(query);
        
        List<String> detectedIndustries = extractIndustries(normalizedQuery);
        List<String> detectedLocations = extractLocations(normalizedQuery);
        String cleanedQuery = cleanQuery(normalizedQuery, detectedIndustries, detectedLocations);
        
        parsed.setIndustries(detectedIndustries);
        parsed.setLocations(detectedLocations);
        parsed.setCleanedQuery(cleanedQuery);
        
        return parsed;
    }
    
    private List<String> extractIndustries(String query) {
        List<String> detected = new ArrayList<>();
        
        for (Map.Entry<String, List<String>> entry : INDUSTRY_KEYWORDS.entrySet()) {
            String industry = entry.getKey();
            List<String> keywords = entry.getValue();
            
            for (String keyword : keywords) {
                if (containsKeyword(query, keyword)) {
                    detected.add(industry);
                    break;
                }
            }
        }
        
        return detected;
    }
    
    private List<String> extractLocations(String query) {
        List<String> detected = new ArrayList<>();
        
        for (Map.Entry<String, List<String>> entry : LOCATION_KEYWORDS.entrySet()) {
            String location = entry.getKey();
            List<String> keywords = entry.getValue();
            
            for (String keyword : keywords) {
                if (containsKeyword(query, keyword)) {
                    detected.add(location);
                    break;
                }
            }
        }
        
        return detected;
    }
    
    private boolean containsKeyword(String query, String keyword) {
        String pattern = "\\b" + Pattern.quote(keyword.toLowerCase()) + "\\b";
        return Pattern.compile(pattern).matcher(query).find();
    }
    
    private String cleanQuery(String query, List<String> industries, List<String> locations) {
        String cleaned = query;
        
        Set<String> wordsToRemove = new HashSet<>();
        
        for (String industry : industries) {
            List<String> keywords = INDUSTRY_KEYWORDS.get(industry);
            if (keywords != null) {
                wordsToRemove.addAll(keywords);
            }
        }
        
        for (String location : locations) {
            List<String> keywords = LOCATION_KEYWORDS.get(location);
            if (keywords != null) {
                wordsToRemove.addAll(keywords);
            }
        }
        
        wordsToRemove.addAll(Arrays.asList("companies", "company", "businesses", "business", "firms", "firm", "organizations", "organization", "in", "at", "from", "near", "around"));
        
        for (String word : wordsToRemove) {
            String pattern = "\\b" + Pattern.quote(word.toLowerCase()) + "\\b";
            cleaned = cleaned.replaceAll(pattern, " ");
        }
        
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        if (cleaned.isEmpty() && !industries.isEmpty()) {
            cleaned = industries.get(0);
        }
        
        return cleaned;
    }
    
    public Map<String, Object> buildFiltersFromParsedQuery(ParsedQuery parsedQuery) {
        Map<String, Object> filters = new HashMap<>();
        
        if (parsedQuery.hasIndustryFilters()) {
            List<String> industryFilters = new ArrayList<>();
            for (String industry : parsedQuery.getIndustries()) {
                industryFilters.addAll(mapIndustryToSolrTerms(industry));
            }
            if (!industryFilters.isEmpty()) {
                filters.put("industry", industryFilters);
            }
        }
        
        if (parsedQuery.hasLocationFilters()) {
            List<String> locationFilters = new ArrayList<>();
            for (String location : parsedQuery.getLocations()) {
                locationFilters.addAll(mapLocationToSolrTerms(location));
            }
            if (!locationFilters.isEmpty()) {
                filters.put("location", locationFilters);
            }
        }
        
        return filters;
    }
    
    private List<String> mapIndustryToSolrTerms(String industry) {
        List<String> solrTerms = new ArrayList<>();
        
        switch (industry.toLowerCase()) {
            case "technology":
                solrTerms.addAll(Arrays.asList("Software Development", "Information Technology", "Computer Software", 
                    "Internet", "Telecommunications", "E-commerce", "SaaS", "Cloud Computing", "Artificial Intelligence",
                    "Machine Learning", "Data Analytics", "Cybersecurity", "Mobile Applications", "Web Development",
                    "Software", "Technology", "IT", "Tech", "Computer", "Digital", "Platform"));
                break;
            case "finance":
                solrTerms.addAll(Arrays.asList("Financial Services", "Banking", "Investment Banking", "Insurance",
                    "Asset Management", "Private Equity", "Venture Capital", "Hedge Fund", "Financial Technology",
                    "Fintech", "Credit", "Lending", "Trading", "Securities", "Finance", "Financial"));
                break;
            case "healthcare":
                solrTerms.addAll(Arrays.asList("Healthcare", "Medical", "Pharmaceutical", "Biotechnology", "Hospital",
                    "Clinical", "Medical Device", "Health Technology", "Telemedicine", "Digital Health", "Life Sciences",
                    "Biotech", "Pharma", "Medical Services", "Health", "Medicine"));
                break;
            case "retail":
                solrTerms.addAll(Arrays.asList("Retail", "E-commerce", "Consumer Goods", "Fashion", "Apparel",
                    "Food & Beverage", "Grocery", "Shopping", "Marketplace", "Consumer Products", "Clothing"));
                break;
            case "manufacturing":
                solrTerms.addAll(Arrays.asList("Manufacturing", "Industrial", "Automotive", "Aerospace", "Chemical",
                    "Materials", "Construction", "Heavy Industry", "Production", "Factory", "Engineering"));
                break;
            case "education":
                solrTerms.addAll(Arrays.asList("Education", "Educational Technology", "E-learning", "Training",
                    "Academic", "University", "School", "Learning", "Research", "EdTech"));
                break;
            case "energy":
                solrTerms.addAll(Arrays.asList("Energy", "Oil & Gas", "Renewable Energy", "Solar", "Wind",
                    "Nuclear", "Electric Utilities", "Power Generation", "Clean Energy", "Green Technology"));
                break;
            case "real estate":
                solrTerms.addAll(Arrays.asList("Real Estate", "Property", "Construction", "Architecture",
                    "Property Management", "Commercial Real Estate", "Real Estate Development"));
                break;
            case "media":
                solrTerms.addAll(Arrays.asList("Media", "Entertainment", "Publishing", "Broadcasting", "Film",
                    "Television", "Music", "Gaming", "Advertising", "Marketing", "Digital Media"));
                break;
            case "telecommunications":
                solrTerms.addAll(Arrays.asList("Telecommunications", "Wireless", "Mobile", "Internet Service Provider",
                    "Broadband", "Network", "Communications", "Telecom"));
                break;
            default:
                solrTerms.add(industry);
                break;
        }
        
        return solrTerms;
    }
    
    private List<String> mapLocationToSolrTerms(String location) {
        List<String> solrTerms = new ArrayList<>();
        
        switch (location.toLowerCase()) {
            case "california":
                solrTerms.addAll(Arrays.asList("California", "CA", "San Francisco", "Los Angeles", "San Diego",
                    "Sacramento", "Oakland", "San Jose", "Silicon Valley", "Bay Area"));
                break;
            case "new york":
                solrTerms.addAll(Arrays.asList("New York", "NY", "NYC", "Manhattan", "Brooklyn", "Queens",
                    "Bronx", "Albany", "Buffalo", "Rochester"));
                break;
            case "texas":
                solrTerms.addAll(Arrays.asList("Texas", "TX", "Houston", "Dallas", "Austin", "San Antonio",
                    "Fort Worth", "El Paso", "Arlington", "Corpus Christi"));
                break;
            case "florida":
                solrTerms.addAll(Arrays.asList("Florida", "FL", "Miami", "Tampa", "Orlando", "Jacksonville",
                    "Tallahassee", "Fort Lauderdale", "West Palm Beach"));
                break;
            case "washington":
                solrTerms.addAll(Arrays.asList("Washington", "WA", "Seattle", "Spokane", "Tacoma", "Vancouver",
                    "Bellevue", "Everett", "Kent", "Renton"));
                break;
            case "illinois":
                solrTerms.addAll(Arrays.asList("Illinois", "IL", "Chicago", "Aurora", "Rockford", "Joliet",
                    "Naperville", "Springfield", "Peoria", "Elgin"));
                break;
            case "united states":
                solrTerms.addAll(Arrays.asList("United States", "USA", "US", "America"));
                break;
            case "canada":
                solrTerms.addAll(Arrays.asList("Canada", "Toronto", "Vancouver", "Montreal", "Calgary",
                    "Ottawa", "Edmonton", "Mississauga"));
                break;
            case "united kingdom":
                solrTerms.addAll(Arrays.asList("United Kingdom", "UK", "Britain", "England", "London",
                    "Manchester", "Birmingham", "Leeds", "Glasgow", "Scotland"));
                break;
            case "germany":
                solrTerms.addAll(Arrays.asList("Germany", "Berlin", "Munich", "Hamburg", "Cologne",
                    "Frankfurt", "Stuttgart", "Dusseldorf"));
                break;
            case "france":
                solrTerms.addAll(Arrays.asList("France", "Paris", "Marseille", "Lyon", "Toulouse",
                    "Nice", "Nantes", "Strasbourg"));
                break;
            case "india":
                solrTerms.addAll(Arrays.asList("India", "Mumbai", "Delhi", "Bangalore", "Hyderabad",
                    "Chennai", "Kolkata", "Pune", "Ahmedabad"));
                break;
            case "china":
                solrTerms.addAll(Arrays.asList("China", "Beijing", "Shanghai", "Guangzhou", "Shenzhen",
                    "Wuhan", "Chengdu", "Dongguan"));
                break;
            case "japan":
                solrTerms.addAll(Arrays.asList("Japan", "Tokyo", "Osaka", "Yokohama", "Nagoya",
                    "Sapporo", "Kobe", "Kyoto", "Fukuoka"));
                break;
            default:
                solrTerms.add(location);
                break;
        }
        
        return solrTerms;
    }
    
    public static class ParsedQuery {
        private String originalQuery;
        private String cleanedQuery;
        private List<String> industries = new ArrayList<>();
        private List<String> locations = new ArrayList<>();
        
        public String getOriginalQuery() { return originalQuery; }
        public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }
        
        public String getCleanedQuery() { return cleanedQuery; }
        public void setCleanedQuery(String cleanedQuery) { this.cleanedQuery = cleanedQuery; }
        
        public List<String> getIndustries() { return industries; }
        public void setIndustries(List<String> industries) { this.industries = industries; }
        
        public List<String> getLocations() { return locations; }
        public void setLocations(List<String> locations) { this.locations = locations; }
        
        public boolean hasIndustryFilters() { return !industries.isEmpty(); }
        public boolean hasLocationFilters() { return !locations.isEmpty(); }
        
        @Override
        public String toString() {
            return "ParsedQuery{" +
                    "originalQuery='" + originalQuery + '\'' +
                    ", cleanedQuery='" + cleanedQuery + '\'' +
                    ", industries=" + industries +
                    ", locations=" + locations +
                    '}';
        }
    }
}