# Portfolio Manager Test Reference

This document lists every **active** test in the project, what each test verifies, and where to find it.

## Quick summary

- Total active test classes: **6**
- Total active test methods: **16**
- Test style: JUnit 5 + Mockito unit/controller tests

---

## 1) Application smoke test

### Class
- [FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/PortfolioApplicationTests.java](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/PortfolioApplicationTests.java)

### Tests
1. `smokeTest()` — [reference](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/PortfolioApplicationTests.java#L10)
   - Verifies the test framework is wired and can execute a trivial assertion.
   - Purpose: basic "test runtime is alive" sanity check.

---

## 2) Asset service tests

### Class
- [FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/service/AssetServiceTest.java](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/service/AssetServiceTest.java)

### Tests
1. `addAsset_createsAssetAndReturnsMappedResponse()` — [reference](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/service/AssetServiceTest.java#L50)
   - Mocks portfolio lookup, price lookup, and repository save.
   - Verifies `addAsset()` returns mapped DTO fields (`id`, `ticker`) and calculated values (`currentPrice`, `marketValue`, `profitLoss`).

2. `updateAsset_whenPortfolioChanged_movesAssetToNewPortfolio()` — [reference](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/service/AssetServiceTest.java#L72)
   - Starts with an existing asset in one portfolio.
   - Verifies `updateAsset()` updates core fields and moves the asset to a different portfolio when `portfolioId` changes.

3. `getAssetById_whenMissing_throws()` — [reference](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/service/AssetServiceTest.java#L99)
   - Mocks `findById()` as empty.
   - Verifies service throws `RuntimeException` containing "Asset not found".

4. `deleteAsset_whenFound_deletes()` — [reference](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/service/AssetServiceTest.java#L106)
   - Mocks existing asset retrieval.
   - Verifies repository `delete()` is called with the found entity.

---

## 3) Portfolio service tests

### Class
- [FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/service/PortfolioServiceTest.java](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/service/PortfolioServiceTest.java)

### Tests
1. `addPortfolio_savesAndMapsResponse()` — [reference](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/service/PortfolioServiceTest.java#L44)
   - Mocks save behavior and generated `id`.
   - Verifies `addPortfolio()` maps request → entity → response DTO correctly.

2. `updatePortfolio_updatesExisting()` — [reference](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/service/PortfolioServiceTest.java#L61)
   - Mocks existing portfolio fetch + save.
   - Verifies `updatePortfolio()` mutates name/description and returns updated DTO.

3. `deletePortfolio_whenExists_deletes()` — [reference](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/service/PortfolioServiceTest.java#L77)
   - Mocks existing portfolio fetch.
   - Verifies repository `delete()` is called.

4. `getOverallPerformance_groupsUniqueTickersAndBuildsSeries()` — [reference](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/service/PortfolioServiceTest.java#L88)
   - Uses assets across months with mixed-case ticker values.
   - Verifies `getOverallPerformance()`:
     - builds expected monthly point count from start month to current month,
     - produces positive aggregated value,
     - normalizes/group tickers so monthly-series hydration runs once per unique ticker (`AAPL`, `MSFT`).

---

## 4) Asset controller tests

### Class
- [FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/controller/AssetControllerTest.java](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/controller/AssetControllerTest.java)

### Tests
1. `createAsset_returnsCreatedStatus()` — [reference](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/controller/AssetControllerTest.java#L36)
   - Mocks service response for create.
   - Verifies controller returns HTTP `201 CREATED` and body contains created asset id.

2. `getAssetPerformance_returnsSeries()` — [reference](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/controller/AssetControllerTest.java#L50)
   - Mocks one performance datapoint.
   - Verifies controller returns HTTP `200 OK` and expected list size.

3. `deleteAsset_returnsNoContent()` — [reference](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/controller/AssetControllerTest.java#L60)
   - Calls delete endpoint.
   - Verifies service `deleteAsset()` invocation and HTTP `204 NO_CONTENT`.

---

## 5) Portfolio controller tests

### Class
- [FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/controller/PortfolioControllerTest.java](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/controller/PortfolioControllerTest.java)

### Tests
1. `getAllPortfolios_returnsOk()` — [reference](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/controller/PortfolioControllerTest.java#L35)
   - Mocks non-empty portfolio DTO list.
   - Verifies controller returns HTTP `200 OK` and expected list size.

2. `getOverallPerformance_returnsData()` — [reference](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/controller/PortfolioControllerTest.java#L45)
   - Mocks one overall performance datapoint.
   - Verifies controller returns HTTP `200 OK` and expected list size.

3. `deletePortfolio_returnsNoContent()` — [reference](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/controller/PortfolioControllerTest.java#L55)
   - Calls delete endpoint.
   - Verifies service `deletePortfolio()` invocation and HTTP `204 NO_CONTENT`.

---

## 6) Global exception handler tests

### Class
- [FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/exception/GlobalExceptionHandlerTest.java](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/exception/GlobalExceptionHandlerTest.java)

### Tests
1. `handleGlobalException_returns500Payload()` — [reference](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/exception/GlobalExceptionHandlerTest.java#L28)
   - Passes a generic exception with message `boom`.
   - Verifies handler returns HTTP `500 INTERNAL_SERVER_ERROR` and payload fields (`message`, `details`, `status`).

### Note on inactive test
- There is a commented-out (inactive) test:
  - `handleAssetNotFound_returns404Payload()` — [reference](FAMPortfolioManager/src/test/java/com/training/FAMPortfolioManager/exception/GlobalExceptionHandlerTest.java#L16)
  - It does **not** run because it is commented.

---

## Latest run status

- Last Maven test run exited successfully (`EXIT:0`) in your terminal context.
