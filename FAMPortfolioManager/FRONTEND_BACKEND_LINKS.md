# Frontend ↔ Backend Link Map

## Base behavior
- Frontend is served by Spring Boot static resources (`src/main/resources/static`), so API calls use same-origin paths (`/api/...`).
- Shared API helpers are in [src/main/resources/static/app.js](src/main/resources/static/app.js):
  - `apiGet(path)`
  - `apiPost(path, body)`
  - `apiPut(path, body)`
  - `apiDelete(path)`
- API responses are normalized in `app.js` so frontend pages can use consistent fields:
  - `id`
  - `purchaseDate` / `datePurchased`
  - `currentPrice`, `marketValue`, `profitLoss`

## Feature wiring

### Dashboard (`index.html` + `app.js`)
- Load portfolios: `GET /api/portfolios`
- Load holdings per portfolio: `GET /api/assets?portfolioId={id}`
- Uses backend-calculated fields for charts/KPIs with fallback data if backend unavailable.

### Holdings page (`holdings.html` + `holdings.js`)
- Portfolio list/cards:
  - `GET /api/portfolios`
- Holdings table:
  - `GET /api/assets?portfolioId={id}`
- Add holding:
  - `POST /api/assets`
  - Payload linked to backend DTO (`AssetRequestDto`):
    - `ticker`
    - `companyName`
    - `quantity`
    - `purchasePrice`
    - `purchaseDate` (ISO datetime)
    - `portfolioId`
- Update holding:
  - `PUT /api/assets/{assetId}`
  - Same DTO-aligned payload including required `portfolioId`
- Remove holding:
  - `DELETE /api/assets/{assetId}`
- Add portfolio:
  - `POST /api/portfolios` with `{ name, description }`
- Update portfolio:
  - `PUT /api/portfolios/{id}` with `{ name, description }`
- Remove portfolio:
  - `DELETE /api/portfolios/{id}`

### Performance page (`performance.html` + `performance.js`)
- Load portfolios: `GET /api/portfolios`
- Load assets for selected portfolio: `GET /api/assets?portfolioId={id}`
- Uses live backend asset metrics (`currentPrice`, `marketValue`, `profitLoss`) to drive KPI cards and chart input.

## Backend code used by these features
- Controllers:
  - [src/main/java/com/training/FAMPortfolioManager/controller/PortfolioController.java](src/main/java/com/training/FAMPortfolioManager/controller/PortfolioController.java)
  - [src/main/java/com/training/FAMPortfolioManager/controller/AssetController.java](src/main/java/com/training/FAMPortfolioManager/controller/AssetController.java)
- Services:
  - [src/main/java/com/training/FAMPortfolioManager/service/PortfolioService.java](src/main/java/com/training/FAMPortfolioManager/service/PortfolioService.java)
  - [src/main/java/com/training/FAMPortfolioManager/service/AssetService.java](src/main/java/com/training/FAMPortfolioManager/service/AssetService.java)
  - [src/main/java/com/training/FAMPortfolioManager/service/PriceService.java](src/main/java/com/training/FAMPortfolioManager/service/PriceService.java)
- DTO contracts:
  - [src/main/java/com/training/FAMPortfolioManager/dto/AssetRequestDto.java](src/main/java/com/training/FAMPortfolioManager/dto/AssetRequestDto.java)
  - [src/main/java/com/training/FAMPortfolioManager/dto/AssetResponseDto.java](src/main/java/com/training/FAMPortfolioManager/dto/AssetResponseDto.java)
  - [src/main/java/com/training/FAMPortfolioManager/dto/PortfolioRequestDTO.java](src/main/java/com/training/FAMPortfolioManager/dto/PortfolioRequestDTO.java)
  - [src/main/java/com/training/FAMPortfolioManager/dto/PortfolioResponseDto.java](src/main/java/com/training/FAMPortfolioManager/dto/PortfolioResponseDto.java)
