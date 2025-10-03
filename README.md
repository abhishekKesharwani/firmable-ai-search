# Firmable AI Search

**Firmable AI Search** is an intelligent company search platform built with Spring Boot that provides advanced search capabilities, intelligent query understanding, and comprehensive autosuggest functionality. The platform combines lexical and semantic search technologies to deliver highly relevant company discovery results.

## 🚀 Features

### **🔍 Advanced Search Engine**
- **Hybrid Search**: Combines lexical (keyword-based) and semantic (AI-powered) search for optimal results
- **Comprehensive Filtering**: Filter by industry, company size, location, founding year, and custom tags
- **Intelligent Query Understanding**: Natural language processing to interpret user intent
- **Multiple Search Types**: Support for lexical, semantic, and hybrid search modes
- **Faceted Search**: Dynamic faceting for refined result exploration

### **💡 Intelligent Autosuggest**
- **Company Name Suggestions**: Real-time company name autocompletion with fuzzy matching
- **Industry Suggestions**: Context-aware industry recommendations
- **Location Suggestions**: Geographic location autosuggestions
- **Query Templates**: Intelligent query pattern suggestions based on user input
- **Multi-type Suggestions**: Unified response with categorized suggestion types

### **🧠 Query Understanding**
- **Natural Language Processing**: Interprets user queries to extract entities and intent
- **Entity Recognition**: Automatically identifies companies, industries, and locations in queries
- **Query Enhancement**: Enriches search queries with additional context and filters
- **Pattern Matching**: Advanced regex-based pattern recognition for structured data

### **⚡ High Performance**
- **Apache Solr Integration**: Leverages Solr's powerful search and indexing capabilities
- **Caching Layer**: Optimized response times with intelligent caching
- **Async Processing**: Non-blocking operations for improved scalability
- **RESTful APIs**: Clean, well-documented REST endpoints

## 🏗️ Architecture

### **Technology Stack**
- **Backend**: Spring Boot 3.2.0 (Java 17)
- **Search Engine**: Apache Solr
- **Build Tool**: Maven
- **API Documentation**: RESTful endpoints with JSON responses

### **Package Structure**
```
com.firmable.aisearch/
├── FirmableAiSearchApplication.java    # Main Spring Boot application
├── controller/                         # REST API controllers
│   ├── SearchController.java          # Search endpoints
│   └── AutosuggestController.java     # Autosuggest endpoints
├── service/                           # Business logic services
│   ├── SearchService.java            # Core search functionality
│   ├── AutosuggestService.java       # Autosuggest logic
│   └── QueryUnderstandingService.java # NLP and query processing
└── model/                            # Data models and DTOs
    ├── SearchResponse.java           # Search result models
    ├── SearchResult.java            # Individual result model
    └── ComprehensiveSearchResponse.java # Enhanced search response
```

## 🚦 Getting Started

### **Prerequisites**
- Java 17 or higher
- Maven 3.6+
- Apache Solr 8.x+ (running on localhost:8983)

### **Installation**

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd firmable-ai-search
   ```

2. **Build the project**
   ```bash
   mvn clean compile
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Verify installation**
   ```bash
   curl http://localhost:8080/api/autosuggest?query=tech&limit=5
   ```

### **Configuration**

The application connects to Solr at `http://localhost:8983/solr/company` by default. Update the configuration in:
- `src/main/java/com/firmable/aisearch/service/AutosuggestService.java`
- `src/main/java/com/firmable/aisearch/service/SearchService.java`

## 📚 API Documentation

### **🔍 Search Endpoints**

#### **GET /api/search**
Basic search with filtering capabilities.

**Parameters:**
- `query` (optional): Search query string
- `industry` (optional): Industry filter (array)
- `size` (optional): Company size filter (array)
- `country` (optional): Country filter
- `city` (optional): City filter
- `foundingYearFrom` (optional): Minimum founding year
- `foundingYearTo` (optional): Maximum founding year
- `tags` (optional): Tag filters (array)
- `sort` (optional): Sort field (default: "relevance")
- `page` (optional): Page number (default: 0)
- `sizePerPage` (optional): Results per page (default: 10)
- `facetFields` (optional): Fields to facet on (array)
- `facetLimit` (optional): Facet result limit (default: 10)

**Example:**
```bash
curl "http://localhost:8080/api/search?query=technology&industry=software&sizePerPage=5"
```

#### **GET /api/search/comprehensive**
Advanced search with enhanced query understanding.

**Parameters:**
- `query` (optional): Natural language search query
- `filters` (optional): JSON-encoded filter object
- `facetFields` (optional): Fields for faceting (array)
- `sortField` (optional): Field to sort by
- `sortDirection` (optional): "asc" or "desc" (default: "asc")
- `page` (optional): Page number (default: 0)
- `pageSize` (optional): Results per page (default: 10)
- `returnFields` (optional): Specific fields to return (array)
- `searchType` (optional): "lexical", "semantic", or "hybrid" (default: "lexical")

**Example:**
```bash
curl "http://localhost:8080/api/search/comprehensive?query=fintech startups in california&searchType=hybrid&pageSize=3"
```

#### **POST /api/search/comprehensive**
Advanced search with complex filter objects.

**Request Body:**
```json
{
  "query": "artificial intelligence companies",
  "filters": {
    "industry": ["technology", "ai"],
    "foundingYear": {"from": 2010, "to": 2023}
  },
  "facetFields": ["industry", "locality"],
  "sortField": "founding_year",
  "sortDirection": "desc",
  "pageSize": 10,
  "searchType": "hybrid"
}
```

### **💡 Autosuggest Endpoints**

#### **GET /api/autosuggest**
Main autosuggest endpoint returning all suggestion types.

**Parameters:**
- `query` (required): Partial query string
- `limit` (optional): Maximum suggestions (default: 10)

**Response:**
```json
{
  "allSuggestions": [
    {
      "text": "technology companies",
      "type": "template",
      "score": 0.8,
      "description": "Find all technology companies"
    }
  ],
  "companyNames": [...],
  "industries": [...],
  "locations": [...],
  "queryTemplates": [...]
}
```

#### **GET /api/autosuggest/companies**
Company name suggestions only.

#### **GET /api/autosuggest/industries**
Industry suggestions only.

#### **GET /api/autosuggest/locations**
Location suggestions only.

#### **GET /api/autosuggest/templates**
Query template suggestions only.

**Example:**
```bash
curl "http://localhost:8080/api/autosuggest?query=tech&limit=5"
```

## 🔧 Configuration

### **Solr Configuration**

Ensure your Solr instance has the following configuration:

1. **Core Name**: `company`
2. **Required Fields**:
   - `name` (company name)
   - `name_s` (string version for exact matching)
   - `industry` (company industry)
   - `locality` (company location)
   - `year_founded_d` (founding year as date)

3. **Suggester Configuration** (optional but recommended):
   ```xml
   <searchComponent name="suggest" class="solr.SuggestComponent">
     <lst name="suggester">
       <str name="name">companySuggester</str>
       <str name="lookupImpl">AnalyzingInfixLookupFactory</str>
       <str name="dictionaryImpl">DocumentDictionaryFactory</str>
       <str name="field">name</str>
       <str name="weightField">popularity</str>
       <str name="suggestAnalyzerFieldType">text_general</str>
     </lst>
   </searchComponent>
   ```

### **Application Properties**

Key configuration options:

```properties
# Server configuration
server.port=8080

# Solr configuration
solr.url=http://localhost:8983/solr/company

# Logging configuration
logging.level.com.firmable.aisearch=DEBUG
```

## 🧪 Testing

### **Run Tests**
```bash
mvn test
```

### **API Testing Examples**

**Test Autosuggest:**
```bash
# Basic autosuggest
curl "http://localhost:8080/api/autosuggest?query=micro"

# Company suggestions
curl "http://localhost:8080/api/autosuggest/companies?query=app"

# Query templates
curl "http://localhost:8080/api/autosuggest/templates?query=tech"
```

**Test Search:**
```bash
# Basic search
curl "http://localhost:8080/api/search?query=software"

# Comprehensive search
curl "http://localhost:8080/api/search/comprehensive?query=ai companies&searchType=hybrid"

# Search with filters
curl "http://localhost:8080/api/search/comprehensive?query=startups&filters={\"foundingYear\":{\"from\":2020}}"
```

## 🔍 Features Deep Dive

### **Query Understanding Engine**

The QueryUnderstandingService provides sophisticated natural language processing:

- **Industry Detection**: Recognizes industry terms and maps them to standardized categories
- **Location Extraction**: Identifies geographic references in queries
- **Company Recognition**: Detects company names and aliases
- **Intent Classification**: Determines search intent (company lookup, industry exploration, etc.)

### **Autosuggest Intelligence**

The autosuggest system provides context-aware suggestions:

- **Prefix Matching**: Fast prefix-based company name matching
- **Fuzzy Matching**: Handles typos and partial matches
- **Popularity Scoring**: Suggestions ranked by relevance and popularity
- **Multi-modal Suggestions**: Combines different suggestion types for comprehensive results

### **Hybrid Search**

The search engine combines multiple approaches:

- **Lexical Search**: Traditional keyword matching with TF-IDF scoring
- **Semantic Search**: AI-powered understanding of query meaning and context
- **Faceted Navigation**: Dynamic filtering based on search results
- **Result Ranking**: Machine learning-enhanced relevance scoring

## 🚀 Deployment

### **Production Deployment**

1. **Build for production**
   ```bash
   mvn clean package -Pprod
   ```

2. **Deploy JAR**
   ```bash
   java -jar target/firmable-ai-search-0.0.1-SNAPSHOT.jar
   ```

3. **Environment Variables**
   ```bash
   export SOLR_URL=http://your-solr-server:8983/solr/company
   export SERVER_PORT=8080
   ```

### **Docker Deployment**

Create a `Dockerfile`:
```dockerfile
FROM openjdk:17-jre-slim
COPY target/firmable-ai-search-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

For support and questions:
- Open an issue on GitHub
- Check the [API documentation](#-api-documentation)
- Review the [troubleshooting guide](#troubleshooting)

## 📈 Roadmap

- [ ] Machine learning-based ranking improvements
- [ ] Advanced NLP entity recognition
- [ ] Real-time search analytics
- [ ] Multi-language support
- [ ] GraphQL API endpoints
- [ ] Advanced caching strategies

---

**Firmable AI Search** - Powering intelligent company discovery through advanced search technology.