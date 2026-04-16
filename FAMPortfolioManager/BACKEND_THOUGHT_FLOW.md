# FAM Portfolio Manager — Backend Thought Flow

## 1) Backend purpose

The backend is a Spring Boot REST API that lets clients:
- Create/manage portfolios
- Create/manage assets inside portfolios
- Fetch live market prices from Alpha Vantage
- Return enriched asset metrics (current price, market value, P/L)

The main entry point is `PortfolioApplication` in [src/main/java/com/training/FAMPortfolioManager/PortfolioApplication.java](src/main/java/com/training/FAMPortfolioManager/PortfolioApplication.java).

---

## 2) Layered architecture (how responsibilities are split)

### Controller layer (HTTP)
- Receives HTTP requests
- Validates request payloads (`@Valid`)
- Delegates to services
- Returns status codes and JSON

Files:
- [src/main/java/com/training/FAMPortfolioManager/controller/PortfolioController.java](src/main/java/com/training/FAMPortfolioManager/controller/PortfolioController.java)
- [src/main/java/com/training/FAMPortfolioManager/controller/AssetController.java](src/main/java/com/training/FAMPortfolioManager/controller/AssetController.java)

### Service layer (business logic)
- Enforces workflow rules
- Coordinates repository + external API calls
- Maps entities to response DTOs

Files:
- [src/main/java/com/training/FAMPortfolioManager/service/PortfolioService.java](src/main/java/com/training/FAMPortfolioManager/service/PortfolioService.java)
- [src/main/java/com/training/FAMPortfolioManager/service/AssetService.java](src/main/java/com/training/FAMPortfolioManager/service/AssetService.java)
- [src/main/java/com/training/FAMPortfolioManager/service/PriceService.java](src/main/java/com/training/FAMPortfolioManager/service/PriceService.java)

### Repository layer (data access)
- JPA repositories provide CRUD and query methods
- Spring generates runtime implementations automatically

Files:
- [src/main/java/com/training/FAMPortfolioManager/repository/PortfolioRepository.java](src/main/java/com/training/FAMPortfolioManager/repository/PortfolioRepository.java)
- [src/main/java/com/training/FAMPortfolioManager/repository/AssetRepository.java](src/main/java/com/training/FAMPortfolioManager/repository/AssetRepository.java)

### Domain and transport models
- JPA entities model database tables
- DTOs model API input/output contracts

Entities:
- [src/main/java/com/training/FAMPortfolioManager/model/Portfolio.java](src/main/java/com/training/FAMPortfolioManager/model/Portfolio.java)
- [src/main/java/com/training/FAMPortfolioManager/model/Asset.java](src/main/java/com/training/FAMPortfolioManager/model/Asset.java)

DTOs:
- [src/main/java/com/training/FAMPortfolioManager/dto/PortfolioRequestDTO.java](src/main/java/com/training/FAMPortfolioManager/dto/PortfolioRequestDTO.java)
- [src/main/java/com/training/FAMPortfolioManager/dto/PortfolioResponseDto.java](src/main/java/com/training/FAMPortfolioManager/dto/PortfolioResponseDto.java)
- [src/main/java/com/training/FAMPortfolioManager/dto/AssetRequestDto.java](src/main/java/com/training/FAMPortfolioManager/dto/AssetRequestDto.java)
- [src/main/java/com/training/FAMPortfolioManager/dto/AssetResponseDto.java](src/main/java/com/training/FAMPortfolioManager/dto/AssetResponseDto.java)

### Cross-cutting config
- CORS + `RestTemplate` bean: [src/main/java/com/training/FAMPortfolioManager/config/WebConfig.java](src/main/java/com/training/FAMPortfolioManager/config/WebConfig.java)
- Caffeine cache manager: [src/main/java/com/training/FAMPortfolioManager/config/CacheConfig.java](src/main/java/com/training/FAMPortfolioManager/config/CacheConfig.java)
- Exception mapping: [src/main/java/com/training/FAMPortfolioManager/exception/GlobalExceptionHandler.java](src/main/java/com/training/FAMPortfolioManager/exception/GlobalExceptionHandler.java)

---

## 3) Data model thought flow

### `Portfolio`
- Table: `portfolios`
- Core fields: id, name, description, created date
- One-to-many relation with `Asset`

### `Asset`
- Table: `assets`
- Core fields: ticker, company name, quantity, purchase price/date
- Many-to-one relation with `Portfolio` via `portfolio_id`

Relationship behavior:
- A portfolio can own many assets
- An asset must belong to one portfolio

---

## 4) Request-to-response flow (end-to-end)

## A) Create Portfolio
1. Client calls `POST /api/portfolios`.
2. `PortfolioController.createPortfolio()` receives `PortfolioRequestDTO`.
3. `PortfolioService.addPortfolio()` builds entity + sets created timestamp.
4. `PortfolioRepository.save()` persists row.
5. Service maps entity to `PortfolioResponseDto`.
6. Controller returns `201 Created`.

## B) Add Asset to Portfolio
1. Client calls `POST /api/assets` with asset payload + `portfolioId`.
2. `AssetController.createAsset()` validates request.
3. `AssetService.addAsset()` first verifies parent portfolio exists using `PortfolioRepository.findById()`.
4. Builds and saves `Asset` via `AssetRepository.save()`.
5. Service maps to response using `mapToResponse()`.
6. During mapping, `PriceService.getCurrentPrice()` is called to enrich with live price metrics.
7. Controller returns `201 Created` enriched payload.

## C) Get Assets by Portfolio
1. Client calls `GET /api/assets?portfolioId={id}`.
2. `AssetService.getAssetsByPortfolio()` queries `AssetRepository.findByPortfolioId()`.
3. Each asset is mapped to `AssetResponseDto` with computed:
   - `marketValue = currentPrice * quantity`
   - `profitLoss = (currentPrice - purchasePrice) * quantity`
   - `profitLossPercent = profitLoss / (purchasePrice * quantity) * 100`

---

## 5) External market price flow

`PriceService.getCurrentPrice(symbol)`:
1. Validates API key is configured.
2. Builds Alpha Vantage URL using configured base URL + query params.
3. Calls API via `RestTemplate`.
4. Handles common failure shapes (`null`, rate-limit note, missing quote, missing price).
5. Parses and returns numeric price.
6. Result is cached via `@Cacheable("stockPrices")` (configured in cache config).

Config keys are in [src/main/resources/application.properties](src/main/resources/application.properties).

---

## 6) Error handling flow

`GlobalExceptionHandler` centralizes exceptions:
- `AssetNotFoundException` -> HTTP 404 JSON body
- Generic `Exception` -> HTTP 500 JSON body

This keeps controller code clean while standardizing error payloads.

---

## 7) Why this backend works well for a frontend phase

- Stable REST endpoints for portfolios and assets
- DTO boundaries isolate frontend contract from DB schema internals
- Service layer already computes frontend-ready financial metrics
- CORS is enabled for local UI clients
- Live pricing is abstracted behind `PriceService` and cache-backed

---

## 8) Practical notes before frontend integration

1. **Secrets/config**: move DB credentials and API key out of source config into env vars.
2. **DDL mode**: `create-drop` is good for development, not production.
3. **Exception consistency**: services currently throw generic `RuntimeException` for missing portfolio/asset in several paths; consider dedicated exceptions for consistent 404s.
4. **Field naming consistency**: ensure entity/DTO id field naming is consistent (`id` vs `Id`) to avoid serialization confusion.

---

## 9) One-line mental model

**Controller receives JSON -> Service enforces business flow -> Repository persists/fetches entities -> Service enriches data (prices + calculations) -> Controller returns frontend-ready JSON.**
